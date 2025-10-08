package com.ai.middle.platform.service.processing.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.FileDetailAttrUtils;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.common.enums.ProcessingStatus;
import com.ai.middle.platform.entity.graph.DocumentNode;
import com.ai.middle.platform.entity.graph.EntityNode;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.ai.middle.platform.entity.po.KbDocument;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.repository.mapper.KbDocumentMapper;
import com.ai.middle.platform.repository.neo4j.DocumentNodeRepository;
import com.ai.middle.platform.repository.neo4j.EntityNodeRepository;
import com.ai.middle.platform.service.PromptService;
import com.ai.middle.platform.service.processing.ChatExecutor;
import com.ai.middle.platform.service.processing.KnowledgeGraphProcessor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphProcessorImpl implements KnowledgeGraphProcessor {

    private static final String BUSINESS_KG = "kg";
    private static final int KG_PAGES_PER_REQUEST = 3;

    private final FileDetailMapper fileDetailMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final DocumentNodeRepository documentNodeRepository;
    private final EntityNodeRepository entityNodeRepository;
    private final Neo4jClient neo4jClient;
    private final PromptService promptService;
    private final ChatExecutor chatExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public void generate(String fileId, String content) {
        log.info("开始生成知识图谱: fileId={}", fileId);
        FileDetail fileDetail = loadFile(fileId);
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());

        List<KbDocument> documentPages = loadCompletedPages(fileId);
        boolean hasPageContent = !documentPages.isEmpty();
        if (!hasPageContent && !StringUtils.hasText(content)) {
            log.warn("知识图谱生成内容为空，跳过: fileId={}", fileId);
            return;
        }

        List<ChunkExtractionResult> chunkResults = new ArrayList<>();

        if (hasPageContent) {
            int totalPages = documentPages.size();
            List<List<KbDocument>> pageChunks = partitionPages(documentPages, KG_PAGES_PER_REQUEST);
            int totalChunks = pageChunks.size();
            for (int index = 0; index < totalChunks; index++) {
                List<KbDocument> chunk = pageChunks.get(index);
                String chunkContent = buildChunkContent(chunk);
                if (!StringUtils.hasText(chunkContent)) {
                    log.debug("跳过空白页面块: fileId={} chunkIndex={}", fileId, index + 1);
                    continue;
                }
                String prompt = buildKnowledgeGraphPrompt(fileDetail, attributes, index + 1, totalChunks, totalPages, chunkContent, chunk);
                String response = chatExecutor.execute(BUSINESS_KG, (chatModel, runtimeConfig) -> chatModel.generate(prompt));
                KnowledgeGraphData chunkData = parseKnowledgeGraph(response, fileId);
                if (chunkData.entities().isEmpty() && chunkData.relationships().isEmpty()) {
                    log.debug("知识图谱块提取为空: fileId={} chunkIndex={}", fileId, index + 1);
                    continue;
                }
                Set<Integer> pageNumbers = chunk.stream().map(KbDocument::getPageIndex).collect(Collectors.toCollection(LinkedHashSet::new));
                chunkResults.add(new ChunkExtractionResult(pageNumbers, chunkData));
            }
        } else {
            String normalizedContent = StringUtils.trimWhitespace(content);
            if (!StringUtils.hasText(normalizedContent)) {
                log.warn("知识图谱生成内容为空，跳过: fileId={}", fileId);
                return;
            }
            String prompt = buildKnowledgeGraphPrompt(fileDetail, attributes, 1, 1, 1, normalizedContent, Collections.emptyList());
            String response = chatExecutor.execute(BUSINESS_KG, (chatModel, runtimeConfig) -> chatModel.generate(prompt));
            KnowledgeGraphData chunkData = parseKnowledgeGraph(response, fileId);
            if (!chunkData.entities().isEmpty() || !chunkData.relationships().isEmpty()) {
                chunkResults.add(new ChunkExtractionResult(Collections.emptySet(), chunkData));
            }
        }

        KnowledgeGraphData mergedGraph = mergeChunkResults(chunkResults);
        persistKnowledgeGraph(fileDetail, mergedGraph);
    }

    private FileDetail loadFile(String fileId) {
        FileDetail fileDetail = fileDetailMapper.selectById(fileId);
        if (fileDetail == null) {
            throw new BusinessException("文件不存在: " + fileId);
        }
        return fileDetail;
    }

    private List<KbDocument> loadCompletedPages(String fileId) {
        return kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId)
                .eq(KbDocument::getOcrStatus, ProcessingStatus.COMPLETED.getCode())
                .orderByAsc(KbDocument::getPageIndex));
    }

    private List<List<KbDocument>> partitionPages(List<KbDocument> pages, int pageSize) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }
        List<List<KbDocument>> chunks = new ArrayList<>();
        for (int index = 0; index < pages.size(); index += Math.max(pageSize, 1)) {
            int end = Math.min(index + Math.max(pageSize, 1), pages.size());
            chunks.add(pages.subList(index, end));
        }
        return chunks;
    }

    private String buildChunkContent(List<KbDocument> chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (KbDocument page : chunk) {
            builder.append("【第").append(page.getPageIndex()).append("页】").append('\n');
            String pageContent = StringUtils.hasText(page.getContent()) ? page.getContent().trim() : "(空白页，无有效文本)";
            builder.append(pageContent).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String buildKnowledgeGraphPrompt(FileDetail fileDetail,
                                             FileDetailAttributes attributes,
                                             int chunkIndex,
                                             int totalChunks,
                                             int totalPages,
                                             String chunkContent,
                                             List<KbDocument> chunkPages) {
        String documentName = StringUtils.hasText(fileDetail.getOriginalFilename()) ? fileDetail.getOriginalFilename() : fileDetail.getFilename();
        String fileType = Optional.ofNullable(attributes.getFileType()).orElse("unknown");
        String pageSummary = (chunkPages == null || chunkPages.isEmpty()) ? "全文"
                : (chunkPages.size() == 1 ? "第" + chunkPages.get(0).getPageIndex() + "页"
                : ("第" + chunkPages.get(0).getPageIndex() + "-" + chunkPages.get(chunkPages.size() - 1).getPageIndex() + "页"));

        String promptTemplate = promptService.getActivePromptContent(BUSINESS_KG);
        Map<String, Object> variables = new HashMap<>();
        variables.put("documentName", documentName);
        variables.put("fileType", fileType);
        variables.put("totalPages", Math.max(totalPages, 1));
        variables.put("chunkIndex", Math.max(chunkIndex, 1));
        variables.put("totalChunks", Math.max(totalChunks, 1));
        variables.put("pageSummary", pageSummary);
        variables.put("content", chunkContent);
        return promptService.formatPrompt(promptTemplate, variables);
    }

    private KnowledgeGraphData mergeChunkResults(List<ChunkExtractionResult> chunkResults) {
        if (chunkResults == null || chunkResults.isEmpty()) {
            return KnowledgeGraphData.empty();
        }
        Map<String, AggregatedEntity> entityMap = new LinkedHashMap<>();
        Map<String, AggregatedEntity> lookupIndex = new HashMap<>();

        for (ChunkExtractionResult chunkResult : chunkResults) {
            Set<Integer> pages = chunkResult.pageNumbers();
            for (GraphEntity entity : chunkResult.data().entities()) {
                if (!StringUtils.hasText(entity.name())) {
                    continue;
                }
                AggregatedEntity aggregated = resolveAggregatedEntity(entityMap, lookupIndex, entity);
                aggregated.merge(entity, pages);
                registerAliasMappings(lookupIndex, aggregated);
            }
        }

        Map<String, AggregatedRelationship> relationshipMap = new LinkedHashMap<>();
        for (ChunkExtractionResult chunkResult : chunkResults) {
            Set<Integer> pages = chunkResult.pageNumbers();
            for (GraphRelationship relationship : chunkResult.data().relationships()) {
                AggregatedEntity fromEntity = findEntityByReference(relationship.fromId(), relationship.fromName(), entityMap, lookupIndex);
                AggregatedEntity toEntity = findEntityByReference(relationship.toId(), relationship.toName(), entityMap, lookupIndex);
                if (fromEntity == null || toEntity == null) {
                    continue;
                }
                String relationshipType = sanitizeRelationshipType(relationship.type());
                String key = fromEntity.getPreferredId() + "->" + toEntity.getPreferredId() + "::" + relationshipType;
                AggregatedRelationship aggregatedRelationship = relationshipMap.computeIfAbsent(key,
                        k -> new AggregatedRelationship(
                                fromEntity.getPreferredId(),
                                toEntity.getPreferredId(),
                                fromEntity.getDisplayName(),
                                toEntity.getDisplayName(),
                                relationshipType));
                aggregatedRelationship.merge(relationship, pages);
            }
        }

        List<GraphEntity> entities = entityMap.values().stream().map(AggregatedEntity::toGraphEntity).toList();
        List<GraphRelationship> relationships = relationshipMap.values().stream().map(AggregatedRelationship::toGraphRelationship).toList();
        return new KnowledgeGraphData(entities, relationships);
    }

    private AggregatedEntity resolveAggregatedEntity(Map<String, AggregatedEntity> entityMap,
                                                     Map<String, AggregatedEntity> lookupIndex,
                                                     GraphEntity entity) {
        String normalizedId = normalizeEntityIdentifier(entity.id());
        if (StringUtils.hasText(normalizedId)) {
            AggregatedEntity existing = lookupIndex.get(normalizedId);
            if (existing != null) {
                return existing;
            }
        }
        String normalizedName = normalizeEntityName(entity.name());
        if (StringUtils.hasText(normalizedName)) {
            AggregatedEntity existing = lookupIndex.get(normalizedName);
            if (existing != null) {
                return existing;
            }
        }
        if (entity.aliases() != null) {
            for (String alias : entity.aliases()) {
                String aliasNormalized = normalizeEntityName(alias);
                if (StringUtils.hasText(aliasNormalized)) {
                    AggregatedEntity existing = lookupIndex.get(aliasNormalized);
                    if (existing != null) {
                        return existing;
                    }
                }
            }
        }
        String canonicalKey;
        if (StringUtils.hasText(normalizedId)) {
            canonicalKey = normalizedId;
        } else if (StringUtils.hasText(normalizedName)) {
            canonicalKey = normalizedName;
        } else {
            canonicalKey = IdGenerator.simpleUUID();
        }
        return entityMap.computeIfAbsent(canonicalKey, key -> new AggregatedEntity(key, entity));
    }

    private void registerAliasMappings(Map<String, AggregatedEntity> lookupIndex, AggregatedEntity aggregated) {
        for (String alias : aggregated.getAllNames()) {
            String normalized = normalizeEntityName(alias);
            if (StringUtils.hasText(normalized)) {
                lookupIndex.put(normalized, aggregated);
            }
        }
        for (String identifier : aggregated.getAllIdentifiers()) {
            String normalized = normalizeEntityIdentifier(identifier);
            if (StringUtils.hasText(normalized)) {
                lookupIndex.put(normalized, aggregated);
            }
        }
    }

    private AggregatedEntity findEntityByReference(String id,
                                                   String name,
                                                   Map<String, AggregatedEntity> entityMap,
                                                   Map<String, AggregatedEntity> lookupIndex) {
        if (StringUtils.hasText(id)) {
            String normalizedId = normalizeEntityIdentifier(id);
            if (StringUtils.hasText(normalizedId)) {
                AggregatedEntity existing = lookupIndex.get(normalizedId);
                if (existing != null) {
                    return existing;
                }
            }
        }
        if (StringUtils.hasText(name)) {
            String normalizedName = normalizeEntityName(name);
            if (StringUtils.hasText(normalizedName)) {
                AggregatedEntity existing = lookupIndex.get(normalizedName);
                if (existing != null) {
                    return existing;
                }
            }
        }
        if (StringUtils.hasText(name)) {
            for (AggregatedEntity candidate : entityMap.values()) {
                if (candidate.matches(name)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String normalizeEntityName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String trimmed = Normalizer.normalize(name.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    private String ensureEntityIdentifier(String candidateId, String fallbackName) {
        String sanitized = normalizeEntityIdentifier(candidateId);
        if (StringUtils.hasText(sanitized)) {
            return sanitized;
        }
        String derived = normalizeEntityIdentifier(fallbackName);
        if (StringUtils.hasText(derived)) {
            return derived;
        }
        return "entity-" + IdGenerator.simpleUUID();
    }

    private String normalizeEntityIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > 80) {
            normalized = normalized.substring(0, 80);
        }
        return normalized;
    }

    private void persistKnowledgeGraph(FileDetail fileDetail, KnowledgeGraphData graphData) {
        log.info("开始持久化知识图谱: fileId={}, entities={}, relationships={}",
                fileDetail.getId(), graphData.entities().size(), graphData.relationships().size());

        try {
            documentNodeRepository.deleteDocumentWithRelations(fileDetail.getId());
            log.info("删除旧知识图谱数据成功: fileId={}", fileDetail.getId());
        } catch (Exception e) {
            log.error("删除旧知识图谱数据失败: fileId={}", fileDetail.getId(), e);
            throw new BusinessException("删除旧知识图谱数据失败: " + e.getMessage());
        }

        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
        String documentName = StringUtils.hasText(fileDetail.getOriginalFilename()) ? fileDetail.getOriginalFilename() : fileDetail.getFilename();

        DocumentNode documentNode = DocumentNode.builder()
                .id(fileDetail.getId())
                .name(documentName)
                .type(Optional.ofNullable(attributes.getFileType()).orElse("document"))
                .createdAt(LocalDateTime.now())
                .build();
        documentNodeRepository.save(documentNode);
        log.info("保存文档节点成功: id={}, name={}", documentNode.getId(), documentNode.getName());

        if (graphData.entities().isEmpty()) {
            log.warn("知识图谱没有实体数据: fileId={}", fileDetail.getId());
            return;
        }

        Map<String, String> entityNodeLookup = new HashMap<>();
        int savedEntityCount = 0;
        for (GraphEntity entity : graphData.entities()) {
            String externalId = StringUtils.hasText(entity.id()) ? entity.id() : "entity-" + IdGenerator.simpleUUID();
            String normalizedExternalId = normalizeEntityIdentifier(externalId);
            if (!StringUtils.hasText(normalizedExternalId)) {
                normalizedExternalId = "entity-" + IdGenerator.simpleUUID();
            }
            String nodeId = fileDetail.getId() + "::" + normalizedExternalId;

            String[] aliasArray = null;
            if (entity.aliases() != null && !entity.aliases().isEmpty()) {
                aliasArray = new LinkedHashSet<>(entity.aliases())
                        .stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .toArray(String[]::new);
                if (aliasArray.length == 0) {
                    aliasArray = null;
                }
            }

            int[] pageArray = null;
            if (entity.sourcePages() != null && !entity.sourcePages().isEmpty()) {
                pageArray = entity.sourcePages().stream()
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .distinct()
                        .sorted()
                        .toArray();
                if (pageArray.length == 0) {
                    pageArray = null;
                }
            }

            EntityNode entityNode = EntityNode.builder()
                    .id(nodeId)
                    .name(entity.name())
                    .type(StringUtils.hasText(entity.type()) ? entity.type() : "concept")
                    .externalId(externalId)
                    .description(entity.description())
                    .aliases(aliasArray)
                    .sourcePages(pageArray)
                    .documentId(fileDetail.getId())
                    .build();

            try {
                entityNodeRepository.save(entityNode);
                savedEntityCount++;
                log.debug("保存实体节点成功: id={}, name={}, type={}", nodeId, entity.name(), entity.type());
            } catch (Exception e) {
                log.error("保存实体节点失败: id={}, name={}", nodeId, entity.name(), e);
                throw new BusinessException("保存实体节点失败: " + e.getMessage());
            }

            try {
                neo4jClient.query("""
                        MATCH (d:Document {id: $documentId})
                        MATCH (e:Entity {id: $entityId})
                        MERGE (e)-[:BELONGS_TO]->(d)
                        """)
                        .bind(fileDetail.getId()).to("documentId")
                        .bind(nodeId).to("entityId")
                        .run();
                log.debug("创建BELONGS_TO关系成功: entity={} -> document={}", nodeId, fileDetail.getId());
            } catch (Exception e) {
                log.error("创建BELONGS_TO关系失败: entity={} -> document={}", nodeId, fileDetail.getId(), e);
                throw new BusinessException("创建BELONGS_TO关系失败: " + e.getMessage());
            }

            registerEntityLookup(entityNodeLookup, externalId, nodeId, true);
            registerEntityLookup(entityNodeLookup, normalizedExternalId, nodeId, true);
            registerEntityLookup(entityNodeLookup, entity.name(), nodeId, false);
            if (entity.aliases() != null) {
                for (String alias : entity.aliases()) {
                    registerEntityLookup(entityNodeLookup, alias, nodeId, false);
                }
            }
        }

        log.info("保存实体节点完成: fileId={}, 成功数={}/{}",
                fileDetail.getId(), savedEntityCount, graphData.entities().size());

        int savedRelationshipCount = 0;
        for (GraphRelationship relationship : graphData.relationships()) {
            String sourceId = resolveEntityNodeId(entityNodeLookup, relationship.fromId(), relationship.fromName());
            String targetId = resolveEntityNodeId(entityNodeLookup, relationship.toId(), relationship.toName());
            if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)) {
                log.debug("跳过关系（实体未找到）: from={}/{} -> to={}/{}",
                        relationship.fromId(), relationship.fromName(), relationship.toId(), relationship.toName());
                continue;
            }

            String relationshipType = sanitizeRelationshipType(relationship.type());
            List<Integer> sourcePages = relationship.sourcePages() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(relationship.sourcePages()));

            try {
                neo4jClient.query(String.format("""
                    MATCH (source:Entity {id: $sourceId})
                    MATCH (target:Entity {id: $targetId})
                    MERGE (source)-[r:%s]->(target)
                    SET r.description = $description,
                        r.sourcePages = $sourcePages
                    """, relationshipType))
                        .bind(sourceId).to("sourceId")
                        .bind(targetId).to("targetId")
                        .bind(Optional.ofNullable(relationship.description()).orElse(""))
                        .to("description")
                        .bind(sourcePages)
                        .to("sourcePages")
                        .run();
                savedRelationshipCount++;
                log.debug("保存关系成功: {} -[{}]-> {}", sourceId, relationshipType, targetId);
            } catch (Exception e) {
                log.error("保存关系失败: {} -[{}]-> {}", sourceId, relationshipType, targetId, e);
                throw new BusinessException("保存关系失败: " + e.getMessage());
            }
        }

        log.info("知识图谱持久化完成: fileId={}, 实体={}/{}, 关系={}/{}",
                fileDetail.getId(), savedEntityCount, graphData.entities().size(), savedRelationshipCount, graphData.relationships().size());
    }

    private String sanitizeRelationshipType(String type) {
        if (!StringUtils.hasText(type)) {
            return "关联";
        }
        String sanitized = type.trim().replaceAll("[\\s\\p{Punct}]+", "_");
        return StringUtils.hasText(sanitized) ? sanitized : "关联";
    }

    private void registerEntityLookup(Map<String, String> lookup, String value, String nodeId, boolean identifier) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(nodeId)) {
            return;
        }
        String normalized = identifier ? normalizeEntityIdentifier(value) : normalizeEntityName(value);
        if (StringUtils.hasText(normalized)) {
            lookup.put(normalized, nodeId);
        }
    }

    private String resolveEntityNodeId(Map<String, String> lookup, String id, String name) {
        if (lookup.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(id)) {
            String normalizedId = normalizeEntityIdentifier(id);
            if (StringUtils.hasText(normalizedId)) {
                String nodeId = lookup.get(normalizedId);
                if (StringUtils.hasText(nodeId)) {
                    return nodeId;
                }
            }
        }
        if (StringUtils.hasText(name)) {
            String normalizedName = normalizeEntityName(name);
            if (StringUtils.hasText(normalizedName)) {
                String nodeId = lookup.get(normalizedName);
                if (StringUtils.hasText(nodeId)) {
                    return nodeId;
                }
            }
        }
        return null;
    }

    private KnowledgeGraphData parseKnowledgeGraph(String response, String fileId) {
        String json = extractJson(response);
        if (!StringUtils.hasText(json)) {
            log.warn("知识图谱响应为空或格式不正确: fileId={}", fileId);
            return KnowledgeGraphData.empty();
        }
        log.info("知识图谱模型输出: fileId={} json={}", fileId, json);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<GraphEntity> entities = new ArrayList<>();
            if (root.has("entities") && root.get("entities").isArray()) {
                for (JsonNode entityNode : root.get("entities")) {
                    String name = toString(entityNode.get("name"));
                    String id = ensureEntityIdentifier(toString(entityNode.get("id")), name);
                    if (!StringUtils.hasText(id)) {
                        continue;
                    }
                    if (!StringUtils.hasText(name)) {
                        name = id;
                    }
                    String type = toString(entityNode.get("type"));
                    String description = toString(entityNode.get("description"));
                    List<String> aliases = readStringArray(entityNode, "aliases");
                    List<Integer> sourcePages = readIntegerArray(entityNode, "sourcePages");
                    entities.add(new GraphEntity(id, name, type, description, aliases, sourcePages));
                }
            }
            List<GraphRelationship> relationships = new ArrayList<>();
            if (root.has("relationships") && root.get("relationships").isArray()) {
                for (JsonNode relNode : root.get("relationships")) {
                    String fromName = toString(relNode.get("from"));
                    String toName = toString(relNode.get("to"));
                    String fromId = ensureEntityIdentifier(toString(relNode.get("fromId")), fromName);
                    String toId = ensureEntityIdentifier(toString(relNode.get("toId")), toName);
                    if (!StringUtils.hasText(fromId) || !StringUtils.hasText(toId)) {
                        continue;
                    }
                    String type = toString(relNode.get("type"));
                    String description = toString(relNode.get("description"));
                    List<Integer> sourcePages = readIntegerArray(relNode, "sourcePages");
                    relationships.add(new GraphRelationship(fromId, toId, type, description, sourcePages, fromName, toName));
                }
            }
            return new KnowledgeGraphData(entities, relationships);
        } catch (Exception e) {
            log.error("解析知识图谱结果失败: fileId={} response={}", fileId, response, e);
            return KnowledgeGraphData.empty();
        }
    }

    private List<String> readStringArray(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node.get(fieldName)) {
            if (item != null && item.isTextual()) {
                String value = item.asText();
                if (StringUtils.hasText(value)) {
                    values.add(value.trim());
                }
            }
        }
        return values;
    }

    private List<Integer> readIntegerArray(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isArray()) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        for (JsonNode item : node.get(fieldName)) {
            if (item == null) {
                continue;
            }
            if (item.isInt() || item.isLong()) {
                values.add(item.asInt());
            } else if (item.isTextual()) {
                String raw = item.asText();
                if (StringUtils.hasText(raw)) {
                    try {
                        values.add(Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return values;
    }

    private String extractJson(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence > -1) {
                trimmed = trimmed.substring(0, closingFence);
            }
        }
        return trimmed.trim();
    }

    private String toString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode jsonNode) {
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
            return jsonNode.toString();
        }
        return String.valueOf(value);
    }

    private static final class AggregatedEntity {
        private final String canonicalKey;
        private String displayName;
        private final LinkedHashSet<String> names = new LinkedHashSet<>();
        private final LinkedHashSet<String> aliases = new LinkedHashSet<>();
        private final LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        private String preferredId;
        private final LinkedHashMap<String, Integer> typeCounts = new LinkedHashMap<>();
        private final LinkedHashSet<String> descriptions = new LinkedHashSet<>();
        private final TreeSet<Integer> sourcePages = new TreeSet<>();

        AggregatedEntity(String canonicalKey, GraphEntity seed) {
            this.canonicalKey = canonicalKey;
            if (StringUtils.hasText(seed.name())) {
                this.displayName = seed.name().trim();
                names.add(this.displayName);
            } else {
                this.displayName = canonicalKey;
            }
            registerIdentifier(seed.id());
            if (seed.aliases() != null) {
                for (String alias : seed.aliases()) {
                    if (StringUtils.hasText(alias)) {
                        aliases.add(alias.trim());
                    }
                }
            }
        }

        void merge(GraphEntity entity, Set<Integer> chunkPages) {
            registerIdentifier(entity.id());
            if (StringUtils.hasText(entity.name())) {
                String trimmed = entity.name().trim();
                names.add(trimmed);
                if (!StringUtils.hasText(displayName) || displayName.equalsIgnoreCase(canonicalKey)) {
                    displayName = trimmed;
                }
            }
            if (StringUtils.hasText(entity.type())) {
                String type = entity.type().trim();
                typeCounts.merge(type, 1, Integer::sum);
            }
            if (StringUtils.hasText(entity.description())) {
                descriptions.add(entity.description().trim());
            }
            if (entity.aliases() != null) {
                for (String alias : entity.aliases()) {
                    if (StringUtils.hasText(alias)) {
                        aliases.add(alias.trim());
                    }
                }
            }
            if (entity.sourcePages() != null) {
                for (Integer page : entity.sourcePages()) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
            if (chunkPages != null) {
                for (Integer page : chunkPages) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
        }

        private void registerIdentifier(String id) {
            if (!StringUtils.hasText(id)) {
                return;
            }
            String trimmed = id.trim();
            identifiers.add(trimmed);
            if (!StringUtils.hasText(preferredId)) {
                preferredId = trimmed;
            }
        }

        String getPreferredId() {
            return StringUtils.hasText(preferredId) ? preferredId : canonicalKey;
        }

        String getDisplayName() {
            if (StringUtils.hasText(displayName)) {
                return displayName;
            }
            return names.stream().filter(StringUtils::hasText).findFirst().orElse(canonicalKey);
        }

        Set<String> getAllNames() { return names; }
        Set<String> getAllIdentifiers() { return identifiers; }

        boolean matches(String candidateName) {
            String normalizedCandidate = normalizeStatic(candidateName);
            if (!StringUtils.hasText(normalizedCandidate)) {
                return false;
            }
            for (String n : names) {
                if (areSimilarStatic(normalizeStatic(n), normalizedCandidate)) {
                    return true;
                }
            }
            for (String a : aliases) {
                if (areSimilarStatic(normalizeStatic(a), normalizedCandidate)) {
                    return true;
                }
            }
            return false;
        }

        GraphEntity toGraphEntity() {
            String displayName = getDisplayName();
            String type = typeCounts.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey).findFirst().orElse("concept");
            String description = descriptions.stream().filter(StringUtils::hasText).limit(3).collect(Collectors.joining("\n"));
            List<String> aliasList = aliases.stream().filter(StringUtils::hasText).limit(10).collect(Collectors.toList());
            List<Integer> pages = new ArrayList<>(sourcePages);
            return new GraphEntity(getPreferredId(), displayName, type, description, aliasList, pages);
        }

        private String normalizeStatic(String value) {
            return value == null ? null : Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
        }

        private boolean areSimilarStatic(String left, String right) {
            if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
                return false;
            }
            if (left.equals(right)) {
                return true;
            }
            String compactLeft = left.replaceAll("\\s+", "");
            String compactRight = right.replaceAll("\\s+", "");
            if (compactLeft.equals(compactRight)) {
                return true;
            }
            if (compactLeft.contains(compactRight) || compactRight.contains(compactLeft)) {
                return true;
            }
            int distance = levenshteinStatic(compactLeft, compactRight);
            return distance >= 0 && distance <= 2;
        }

        private int levenshteinStatic(String left, String right) {
            if (left == null || right == null) {
                return -1;
            }
            int[][] dp = new int[left.length() + 1][right.length() + 1];
            for (int i = 0; i <= left.length(); i++) {
                dp[i][0] = i;
            }
            for (int j = 0; j <= right.length(); j++) {
                dp[0][j] = j;
            }
            for (int i = 1; i <= left.length(); i++) {
                for (int j = 1; j <= right.length(); j++) {
                    int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
            return dp[left.length()][right.length()];
        }
    }

    private static final class AggregatedRelationship {
        private final String fromId;
        private final String toId;
        private final String type;
        private final String fromName;
        private final String toName;
        private final LinkedHashSet<String> descriptions = new LinkedHashSet<>();
        private final TreeSet<Integer> sourcePages = new TreeSet<>();

        AggregatedRelationship(String fromId, String toId, String type, String fromName, String toName) {
            this.fromId = fromId;
            this.toId = toId;
            this.type = type;
            this.fromName = fromName;
            this.toName = toName;
        }

        void merge(GraphRelationship relationship, Set<Integer> chunkPages) {
            if (StringUtils.hasText(relationship.description())) {
                descriptions.add(relationship.description().trim());
            }
            if (relationship.sourcePages() != null) {
                for (Integer page : relationship.sourcePages()) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
            if (chunkPages != null) {
                for (Integer page : chunkPages) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
        }

        GraphRelationship toGraphRelationship() {
            String description = descriptions.stream().filter(StringUtils::hasText).limit(3).collect(Collectors.joining("\n"));
            return new GraphRelationship(fromId, toId, type, description, new ArrayList<>(sourcePages), fromName, toName);
        }
    }

    private record ChunkExtractionResult(Set<Integer> pageNumbers, KnowledgeGraphData data) {}

    private record GraphEntity(String id, String name, String type, String description, List<String> aliases, List<Integer> sourcePages) {}

    private record GraphRelationship(String fromId, String toId, String type, String description, List<Integer> sourcePages, String fromName, String toName) {}

    private record KnowledgeGraphData(List<GraphEntity> entities, List<GraphRelationship> relationships) {
        static KnowledgeGraphData empty() {
            return new KnowledgeGraphData(List.of(), List.of());
        }
    }
}


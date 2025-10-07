package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphDTO {

    /**
     * 图谱节点列表
     */
    private List<GraphNodeDTO> nodes;

    /**
     * 图谱边列表
     */
    private List<GraphEdgeDTO> edges;

    /**
     * 图谱节点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphNodeDTO {

        /**
         * 节点ID
         */
        private String id;

        /**
         * 节点名称
         */
        private String label;

        /**
         * 节点类型
         */
        private String type;
    }

    /**
     * 图谱边信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdgeDTO {

        /**
         * 起始节点ID
         */
        private String source;

        /**
         * 目标节点ID
         */
        private String target;

        /**
         * 边的标签或关系类型
         */
        private String label;
    }
}

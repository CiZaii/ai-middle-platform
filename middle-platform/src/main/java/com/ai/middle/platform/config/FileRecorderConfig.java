package com.ai.middle.platform.config;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FilePartDetail;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.repository.mapper.FilePartDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自定义实现 X-File-Storage 的 {@link FileRecorder}，
 * 将文件元数据持久化到本地数据库，便于统一管理、下载和删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecorderConfig implements FileRecorder {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {};

    private final FileDetailMapper fileDetailMapper;
    private final FilePartDetailMapper filePartDetailMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(FileInfo fileInfo) {
        if (fileInfo == null) {
            return false;
        }
        ensureFileId(fileInfo);
        FileDetail existing = fileDetailMapper.selectById(fileInfo.getId());
        FileDetail entity = toEntity(fileInfo, existing);
        if (existing == null) {
            int rows = fileDetailMapper.insert(entity);
            fileInfo.setId(entity.getId());
            log.debug("Persisted file record: id={}, url={}", entity.getId(), entity.getUrl());
            return rows > 0;
        }
        int rows = fileDetailMapper.updateById(entity);
        log.debug("Updated existing file record: id={}, url={}", entity.getId(), entity.getUrl());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FileInfo fileInfo) {
        if (fileInfo == null) {
            return;
        }
        FileDetail existing = null;
        if (StrUtil.isNotBlank(fileInfo.getId())) {
            existing = fileDetailMapper.selectById(fileInfo.getId());
        }
        if (existing == null && StrUtil.isNotBlank(fileInfo.getUrl())) {
            existing = fileDetailMapper.selectOne(new LambdaQueryWrapper<FileDetail>()
                    .eq(FileDetail::getUrl, fileInfo.getUrl()));
        }
        if (existing == null) {
            save(fileInfo);
            return;
        }
        fileInfo.setId(existing.getId());
        FileDetail entity = toEntity(fileInfo, existing);
        fileDetailMapper.updateById(entity);
        log.debug("Updated file info by url/id: id={}, url={}", entity.getId(), entity.getUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public FileInfo getByUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        FileDetail record = fileDetailMapper.selectOne(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getUrl, url));
        if (record == null) {
            return null;
        }
        return toFileInfo(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        FileDetail record = fileDetailMapper.selectOne(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getUrl, url));
        if (record == null) {
            return true;
        }
        fileDetailMapper.deleteById(record.getId());
        if (StrUtil.isNotBlank(record.getUploadId())) {
            filePartDetailMapper.delete(new LambdaQueryWrapper<FilePartDetail>()
                    .eq(FilePartDetail::getUploadId, record.getUploadId()));
        }
        log.debug("Deleted file record and related parts: id={}, url={}", record.getId(), record.getUrl());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFilePart(FilePartInfo filePartInfo) {
        if (filePartInfo == null) {
            return;
        }
        String partId = StrUtil.blankToDefault(filePartInfo.getId(), generatePartId(filePartInfo));
        filePartInfo.setId(partId);
        FilePartDetail existing = filePartDetailMapper.selectById(partId);
        FilePartDetail entity = toPartEntity(filePartInfo, existing);
        if (existing == null) {
            filePartDetailMapper.insert(entity);
        } else {
            filePartDetailMapper.updateById(entity);
        }
        log.debug("Persisted file part: uploadId={}, partNumber={}", filePartInfo.getUploadId(), filePartInfo.getPartNumber());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFilePartByUploadId(String uploadId) {
        if (StrUtil.isBlank(uploadId)) {
            return;
        }
        filePartDetailMapper.delete(new LambdaQueryWrapper<FilePartDetail>()
                .eq(FilePartDetail::getUploadId, uploadId));
        log.debug("Deleted file parts by uploadId={}", uploadId);
    }

    private void ensureFileId(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getId())) {
            fileInfo.setId(IdUtil.fastSimpleUUID());
        }
    }

    private FileDetail toEntity(FileInfo info, FileDetail existing) {
        LocalDateTime createTime = Optional.ofNullable(toLocalDateTime(info.getCreateTime()))
                .orElseGet(() -> existing != null ? existing.getCreateTime() : LocalDateTime.now());
        return FileDetail.builder()
                .id(info.getId())
                .url(info.getUrl())
                .size(info.getSize())
                .filename(info.getFilename())
                .originalFilename(info.getOriginalFilename())
                .basePath(info.getBasePath())
                .path(info.getPath())
                .ext(info.getExt())
                .contentType(info.getContentType())
                .platform(info.getPlatform())
                .thUrl(info.getThUrl())
                .thFilename(info.getThFilename())
                .thSize(info.getThSize())
                .thContentType(info.getThContentType())
                .objectId(info.getObjectId())
                .objectType(info.getObjectType())
                .metadata(toJson(info.getMetadata()))
                .userMetadata(toJson(info.getUserMetadata()))
                .thMetadata(toJson(info.getThMetadata()))
                .thUserMetadata(toJson(info.getThUserMetadata()))
                .attr(toJson(info.getAttr()))
                .fileAcl(toJson(info.getFileAcl()))
                .thFileAcl(toJson(info.getThFileAcl()))
                .hashInfo(toJson(info.getHashInfo()))
                .uploadId(info.getUploadId())
                .uploadStatus(info.getUploadStatus())
                .createTime(createTime)
                .build();
    }

    private FilePartDetail toPartEntity(FilePartInfo partInfo, FilePartDetail existing) {
        LocalDateTime createTime = Optional.ofNullable(toLocalDateTime(partInfo.getCreateTime()))
                .orElseGet(() -> existing != null ? existing.getCreateTime() : LocalDateTime.now());
        return FilePartDetail.builder()
                .id(partInfo.getId())
                .platform(partInfo.getPlatform())
                .uploadId(partInfo.getUploadId())
                .eTag(partInfo.getETag())
                .partNumber(partInfo.getPartNumber())
                .partSize(partInfo.getPartSize())
                .hashInfo(toJson(partInfo.getHashInfo()))
                .createTime(createTime)
                .build();
    }

    private FileInfo toFileInfo(FileDetail record) {
        FileInfo info = new FileInfo();
        info.setId(record.getId());
        info.setUrl(record.getUrl());
        info.setSize(record.getSize());
        info.setFilename(record.getFilename());
        info.setOriginalFilename(record.getOriginalFilename());
        info.setBasePath(record.getBasePath());
        info.setPath(record.getPath());
        info.setExt(record.getExt());
        info.setContentType(record.getContentType());
        info.setPlatform(record.getPlatform());
        info.setThUrl(record.getThUrl());
        info.setThFilename(record.getThFilename());
        info.setThSize(record.getThSize());
        info.setThContentType(record.getThContentType());
        info.setObjectId(record.getObjectId());
        info.setObjectType(record.getObjectType());
        info.setMetadata(fromJson(record.getMetadata(), STRING_MAP_TYPE));
        info.setUserMetadata(fromJson(record.getUserMetadata(), STRING_MAP_TYPE));
        info.setThMetadata(fromJson(record.getThMetadata(), STRING_MAP_TYPE));
        info.setThUserMetadata(fromJson(record.getThUserMetadata(), STRING_MAP_TYPE));
        info.setAttr(toDict(record.getAttr()));
        info.setFileAcl(fromJson(record.getFileAcl(), Object.class));
        info.setThFileAcl(fromJson(record.getThFileAcl(), Object.class));
        info.setHashInfo(fromJson(record.getHashInfo(), HashInfo.class));
        info.setUploadId(record.getUploadId());
        info.setUploadStatus(record.getUploadStatus());
        info.setCreateTime(toDate(record.getCreateTime()));
        return info;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize value to JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }

    private Dict toDict(String json) {
        Map<String, Object> data = fromJson(json, OBJECT_MAP_TYPE);
        if (data == null) {
            return null;
        }
        Dict dict = Dict.create();
        dict.putAll(data);
        return dict;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
    }

    private Date toDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return Date.from(time.atZone(DEFAULT_ZONE).toInstant());
    }

    private String generatePartId(FilePartInfo info) {
        String uploadId = StrUtil.blankToDefault(info.getUploadId(), IdUtil.fastSimpleUUID());
        Integer partNumber = Optional.ofNullable(info.getPartNumber()).orElse(0);
        return uploadId + ":" + partNumber;
    }
}

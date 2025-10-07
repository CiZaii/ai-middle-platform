package com.ai.middle.platform.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MinioFileStorage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageInitializer implements ApplicationRunner {

    private final FileStorageService fileStorageService;

    @Override
    public void run(ApplicationArguments args) {
        CopyOnWriteArrayList<FileStorage> storages = fileStorageService.getFileStorageList();
        for (FileStorage storage : storages) {
            if (storage instanceof MinioFileStorage minioStorage) {
                ensureBucketExists(minioStorage);
            }
        }
    }

    private void ensureBucketExists(MinioFileStorage minioStorage) {
        String bucketName = minioStorage.getBucketName();
        MinioClient client = minioStorage.getClient();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created missing MinIO bucket: {}", bucketName);
            }
        } catch (Exception ex) {
            log.error("Failed to ensure MinIO bucket {}", bucketName, ex);
            throw new IllegalStateException("Unable to access MinIO bucket: " + bucketName, ex);
        }
    }
}

package com.nexusos.api.files.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioService(
            @Value("${nexusos.minio.url:http://localhost:9000}") String url,
            @Value("${nexusos.minio.access-key:nexus_minio}") String accessKey,
            @Value("${nexusos.minio.secret-key:SuperStrongPass123!}") String secretKey,
            @Value("${nexusos.minio.bucket:nexusos-files}") String bucketName) {
        
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
        this.bucketName = bucketName;
        
        try {
            boolean found = minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket check/creation failed during startup. File operations may fail until MinIO is available. Error: {}", e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, UUID workspaceId) throws Exception {
        String objectKey = workspaceId.toString() + "/" + UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        return objectKey;
    }

    public String getPresignedDownloadUrl(String objectKey) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectKey)
                        .expiry(1, TimeUnit.HOURS)
                        .build());
    }

    public byte[] downloadFile(String objectKey) throws Exception {
        try (InputStream stream = minioClient.getObject(io.minio.GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build())) {
            return stream.readAllBytes();
        }
    }

    public void deleteFile(String objectKey) throws Exception {
        minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build());
    }
}

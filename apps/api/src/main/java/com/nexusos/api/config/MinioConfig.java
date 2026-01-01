package com.nexusos.api.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${nexusos.minio.url:http://localhost:9000}") String url,
            @Value("${nexusos.minio.access-key:nexus_minio}") String accessKey,
            @Value("${nexusos.minio.secret-key:SuperStrongPass123!}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}

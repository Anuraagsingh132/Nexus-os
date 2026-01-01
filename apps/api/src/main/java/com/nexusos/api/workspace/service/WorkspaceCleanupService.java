package com.nexusos.api.workspace.service;

import com.nexusos.api.files.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

@Service
public class WorkspaceCleanupService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceCleanupService.class);

    private final MinioService minioService;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.vectorstore.qdrant.host:qdrant}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private String qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:nexusos-embeddings}")
    private String collectionName;

    public WorkspaceCleanupService(MinioService minioService) {
        this.minioService = minioService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    public void cleanupWorkspaceResources(UUID workspaceId, List<String> fileObjectKeys) {
        log.info("Starting async cleanup for workspace {}", workspaceId);
        
        // 1. Delete MinIO objects
        for (String objectKey : fileObjectKeys) {
            try {
                minioService.deleteFile(objectKey);
                log.debug("Deleted MinIO object: {}", objectKey);
            } catch (Exception e) {
                log.error("Failed to delete MinIO object: {}", objectKey, e);
            }
        }

        // 2. Delete Qdrant vectors
        try {
            // Qdrant REST API runs on port 6333 usually, even if gRPC is 6334. 
            // We should use 6333 for REST. Let's try 6333 since qdrant defaults to 6333 for REST.
            String qdrantRestUrl = "http://" + qdrantHost + ":6333/collections/" + collectionName + "/points/delete";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create payload: 
            // { "filter": { "must": [ { "key": "workspaceId", "match": { "value": "uuid" } } ] } }
            Map<String, Object> match = new HashMap<>();
            match.put("value", workspaceId.toString());

            Map<String, Object> condition = new HashMap<>();
            condition.put("key", "workspaceId");
            condition.put("match", match);

            Map<String, Object> must = new HashMap<>();
            must.put("must", Arrays.asList(condition));

            Map<String, Object> payload = new HashMap<>();
            payload.put("filter", must);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.exchange(qdrantRestUrl, HttpMethod.POST, request, String.class);
            log.info("Deleted Qdrant vectors for workspace {}", workspaceId);
        } catch (Exception e) {
            log.error("Failed to delete Qdrant vectors for workspace {}", workspaceId, e);
        }
        
        log.info("Completed async cleanup for workspace {}", workspaceId);
    }
}

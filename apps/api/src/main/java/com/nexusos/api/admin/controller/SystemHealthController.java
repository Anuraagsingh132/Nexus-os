package com.nexusos.api.admin.controller;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/health")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final MinioClient minioClient;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.vectorstore.qdrant.host:qdrant}")
    private String qdrantHost;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    public SystemHealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate, MinioClient minioClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.minioClient = minioClient;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> healthStatus = new HashMap<>();

        // Postgres
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            healthStatus.put("database", "UP");
        } catch (Exception e) {
            healthStatus.put("database", "DOWN");
        }

        // Redis
        try {
            String pingResponse = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pingResponse)) {
                healthStatus.put("redis", "UP");
            } else {
                healthStatus.put("redis", "DOWN");
            }
        } catch (Exception e) {
            healthStatus.put("redis", "DOWN");
        }

        // MinIO
        try {
            minioClient.listBuckets();
            healthStatus.put("minio", "UP");
        } catch (Exception e) {
            healthStatus.put("minio", "DOWN");
        }

        // Qdrant
        try {
            // Assume default REST port for Qdrant is 6333
            String qdrantUrl = "http://" + qdrantHost + ":6333/healthz";
            ResponseEntity<String> response = restTemplate.getForEntity(qdrantUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                healthStatus.put("qdrant", "UP");
            } else {
                healthStatus.put("qdrant", "DOWN");
            }
        } catch (Exception e) {
            healthStatus.put("qdrant", "DOWN");
        }

        // Ollama
        try {
            String ollamaUrl = ollamaBaseUrl + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(ollamaUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                healthStatus.put("ollama", "UP");
            } else {
                healthStatus.put("ollama", "DOWN");
            }
        } catch (Exception e) {
            healthStatus.put("ollama", "DOWN");
        }

        return ResponseEntity.ok(healthStatus);
    }
}

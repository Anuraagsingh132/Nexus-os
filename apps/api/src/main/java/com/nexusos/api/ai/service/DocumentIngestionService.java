package com.nexusos.api.ai.service;

import com.nexusos.api.files.repository.FileMetadataRepository;
import com.nexusos.api.files.domain.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final IngestionFailureRepository ingestionFailureRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.vectorstore.qdrant.host:qdrant}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:nexusos-embeddings}")
    private String collectionName;

    public DocumentIngestionService(@org.springframework.context.annotation.Lazy VectorStore vectorStore, IngestionFailureRepository ingestionFailureRepository, FileMetadataRepository fileMetadataRepository) {
        this.vectorStore = vectorStore;
        this.ingestionFailureRepository = ingestionFailureRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    private void updateFileStatus(UUID documentId, String status, String error) {
        fileMetadataRepository.findById(documentId).ifPresent(file -> {
            file.setIngestionStatus(status);
            file.setIngestionError(error);
            fileMetadataRepository.save(file);
        });
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000)
    public void failStaleIngestions() {
        java.time.Instant tenMinutesAgo = java.time.Instant.now().minusSeconds(600);
        List<FileMetadata> staleFiles = fileMetadataRepository.findByIngestionStatusAndUpdatedAtBefore("PROCESSING", tenMinutesAgo);
        if (!staleFiles.isEmpty()) {
            log.info("Found {} stale document ingestions, marking as FAILED", staleFiles.size());
            staleFiles.forEach(file -> {
                file.setIngestionStatus("FAILED");
                file.setIngestionError("Ingestion timed out — possibly interrupted by a service restart. Please re-upload.");
            });
            fileMetadataRepository.saveAll(staleFiles);
        }
    }

    @Async("taskExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void ingestPdf(byte[] pdfBytes, UUID workspaceId, UUID documentId, String title) {
        log.info("Ingesting PDF '{}' for workspace {}", title, workspaceId);
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfBottomTextLinesToDelete(0)
                        .withNumberOfTopPagesToSkipBeforeDelete(0)
                        .build())
                .withPagesPerDocument(1)
                .build();

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
        
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(pdfReader.get());
        
        for (Document doc : splitDocuments) {
            doc.getMetadata().put("workspaceId", workspaceId.toString());
            doc.getMetadata().put("documentId", documentId.toString());
            doc.getMetadata().put("title", title);
        }

        try {
            deleteExistingVectors(documentId);
            vectorStore.accept(splitDocuments);
            log.info("Successfully ingested PDF '{}' ({} chunks)", title, splitDocuments.size());
            updateFileStatus(documentId, "SUCCESS", null);
        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.error("Failed to ingest PDF '{}': {}", title, errorMsg, e);
            updateFileStatus(documentId, "FAILED", errorMsg);
            throw e;
        }
    }
    
    @Async("taskExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void ingestText(String text, UUID workspaceId, UUID documentId, String title) {
        log.info("Ingesting text '{}' for workspace {}", title, workspaceId);
        Document document = new Document(text);
        
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(List.of(document));
        
        for (Document doc : splitDocuments) {
            doc.getMetadata().put("workspaceId", workspaceId.toString());
            doc.getMetadata().put("documentId", documentId.toString());
            doc.getMetadata().put("title", title);
        }

        try {
            deleteExistingVectors(documentId);
            vectorStore.accept(splitDocuments);
            log.info("Successfully ingested text '{}' ({} chunks)", title, splitDocuments.size());
            updateFileStatus(documentId, "SUCCESS", null);
        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.error("Failed to ingest text '{}': {}", title, errorMsg, e);
            updateFileStatus(documentId, "FAILED", errorMsg);
            throw e;
        }
    }

    @Recover
    public void recoverPdf(Exception e, byte[] pdfBytes, UUID workspaceId, UUID documentId, String title) {
        String errorMsg = extractErrorMessage(e);
        log.error("All retries exhausted for PDF ingestion '{}': {}", title, errorMsg);
        ingestionFailureRepository.save(new IngestionFailure(workspaceId, documentId, title, "PDF", errorMsg));
    }

    @Recover
    public void recoverText(Exception e, String text, UUID workspaceId, UUID documentId, String title) {
        String errorMsg = extractErrorMessage(e);
        log.error("All retries exhausted for text ingestion '{}': {}", title, errorMsg);
        ingestionFailureRepository.save(new IngestionFailure(workspaceId, documentId, title, "TEXT", errorMsg));
    }

    private String extractErrorMessage(Throwable e) {
        if (e == null) return "Unknown error during ingestion";
        if (e instanceof NullPointerException) {
            return "Embedding engine returned empty vector. Ensure Ollama model 'nomic-embed-text' is pulled ('docker exec -it nexusos-ollama-1 ollama pull nomic-embed-text') or OPENAI_API_KEY is provided.";
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        if (e.getCause() != null && e.getCause() != e) {
            msg += " [Cause: " + extractErrorMessage(e.getCause()) + "]";
        }
        return msg;
    }

    private void deleteExistingVectors(UUID documentId) {
        String qdrantRestUrl = "http://" + qdrantHost + ":6333/collections/" + collectionName + "/points/delete";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> match = new HashMap<>();
        match.put("value", documentId.toString());

        Map<String, Object> condition = new HashMap<>();
        condition.put("key", "documentId");
        condition.put("match", match);

        Map<String, Object> filter = new HashMap<>();
        filter.put("must", Arrays.asList(condition));

        Map<String, Object> payload = new HashMap<>();
        payload.put("filter", filter);

        try {
            restTemplate.exchange(qdrantRestUrl, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            log.debug("Deleted existing Qdrant vectors for document {}", documentId);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.debug("Qdrant collection '{}' does not exist yet (404), skipping pre-ingestion deletion", collectionName);
        } catch (Exception e) {
            log.warn("Unable to delete existing Qdrant vectors for document {} before ingestion: {}", documentId, e.getMessage());
        }
    }
}

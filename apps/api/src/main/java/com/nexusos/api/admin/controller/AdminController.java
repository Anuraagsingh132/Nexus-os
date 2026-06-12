package com.nexusos.api.admin.controller;

import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.files.repository.FileMetadataRepository;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.notifications.repository.NotificationRepository;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.chat.repository.ChannelRepository;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.admin.ActivityDataPointDto;
import com.nexusos.api.workspace.service.WorkspaceCleanupService;
import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.domain.Document;
import com.nexusos.api.files.domain.FileMetadata;
import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.chat.domain.Channel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final NotificationRepository notificationRepository;
    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final WorkspaceCleanupService workspaceCleanupService;

    public AdminController(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            ChannelRepository channelRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            WorkspaceRepository workspaceRepository,
            FileMetadataRepository fileMetadataRepository,
            NotificationRepository notificationRepository,
            DataSource dataSource,
            StringRedisTemplate redisTemplate,
            WorkspaceCleanupService workspaceCleanupService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.notificationRepository = notificationRepository;
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.workspaceCleanupService = workspaceCleanupService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getWorkspaceStats() {
        Long totalBytes = fileMetadataRepository.sumSizeBytes();
        double storageUsedGB = Math.round((totalBytes != null ? totalBytes : 0L) / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeWorkspaces", workspaceRepository.count());
        stats.put("storageUsed", storageUsedGB);
        stats.put("activeProjects", projectRepository.count());
        stats.put("totalTasks", taskRepository.count());
        stats.put("documents", documentRepository.count());
        return stats;
    }

    @GetMapping("/activity")
    public List<ActivityDataPointDto> getActivity() {
        return userRepository.countUsersPerDayNative().stream().map(row -> {
            String label = (String) row[0];
            long value = ((Number) row[1]).longValue();
            return new ActivityDataPointDto(label, value);
        }).collect(Collectors.toList());
    }

    public record UserAdminDto(UUID id, String email, String displayName, String createdAt, String status) {}

    @GetMapping("/users")
    public Page<UserAdminDto> getUsers(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(u -> new UserAdminDto(u.getId(), u.getEmail(), u.getFullName(), u.getCreatedAt().toString(), u.getStatus()));
    }

    public record StatusUpdateRequest(String status) {}

    @PatchMapping("/users/{id}/status")
    public UserAdminDto updateUserStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(request.status());
        userRepository.save(user);
        
        redisTemplate.delete("user:status:" + id);
        
        return new UserAdminDto(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt().toString(), user.getStatus());
    }

    public record WorkspaceAdminDto(UUID id, String name, String slug, String createdAt, String deletedAt) {}

    @GetMapping("/workspaces")
    public Page<WorkspaceAdminDto> getWorkspaces(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return workspaceRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(w -> new WorkspaceAdminDto(w.getId(), w.getName(), w.getSlug(), w.getCreatedAt().toString(), 
                        w.getDeletedAt() != null ? w.getDeletedAt().toString() : null));
    }

    @DeleteMapping("/workspaces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.transaction.annotation.Transactional
    public void deleteWorkspace(@PathVariable UUID id) {
        Workspace w = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        
        Instant now = Instant.now();
        w.setDeletedAt(now);
        workspaceRepository.save(w);
        
        // Bulk delete Projects and Tasks
        taskRepository.deleteByWorkspaceId(id);
        projectRepository.deleteByWorkspaceId(id);
        
        // Hard-delete Channels
        List<Channel> channels = channelRepository.findByWorkspaceId(id);
        channelRepository.deleteAll(channels);
        
        // Soft-delete documents
        List<Document> documents = documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(id);
        documents.forEach(d -> d.setDeletedAt(now));
        documentRepository.saveAll(documents);
        
        // Soft-delete files and get keys for async cleanup
        List<FileMetadata> files = fileMetadataRepository.findByWorkspaceId(id);
        List<String> fileObjectKeys = new ArrayList<>();
        files.forEach(f -> {
            f.setDeletedAt(now);
            fileObjectKeys.add(f.getObjectKey());
        });
        fileMetadataRepository.saveAll(files);
        
        // Async cleanup of MinIO and Qdrant resources
        workspaceCleanupService.cleanupWorkspaceResources(id, fileObjectKeys);
    }

    @GetMapping("/alerts")
    public List<Map<String, String>> getAlerts() {
        List<Map<String, String>> alerts = new ArrayList<>();
        String now = Instant.now().toString();

        // Check Postgres
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2);
            alerts.add(buildAlert("PostgreSQL Operational", "Database connection pool is healthy", now, "text-emerald-500", "bg-emerald-100"));
        } catch (Exception e) {
            log.warn("Postgres health check failed: {}", e.getMessage());
            alerts.add(buildAlert("PostgreSQL Down", "Database connection failed: " + e.getMessage(), now, "text-red-500", "bg-red-100"));
        }

        // Check Redis
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                alerts.add(buildAlert("Redis Operational", "Cache service is responding normally", now, "text-emerald-500", "bg-emerald-100"));
            } else {
                alerts.add(buildAlert("Redis Warning", "Redis responded unexpectedly: " + pong, now, "text-amber-500", "bg-amber-100"));
            }
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            alerts.add(buildAlert("Redis Down", "Cache service unreachable: " + e.getMessage(), now, "text-red-500", "bg-red-100"));
        }

        return alerts;
    }

    private Map<String, String> buildAlert(String title, String desc, String time, String color, String bg) {
        Map<String, String> alert = new LinkedHashMap<>();
        alert.put("title", title);
        alert.put("desc", desc);
        alert.put("time", time);
        alert.put("color", color);
        alert.put("bg", bg);
        return alert;
    }
}


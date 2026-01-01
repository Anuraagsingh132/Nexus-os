package com.nexusos.api.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeletePurgeService {

    private static final Logger log = LoggerFactory.getLogger(SoftDeletePurgeService.class);

    private final JdbcTemplate jdbcTemplate;
    private final int retentionDays;

    public SoftDeletePurgeService(
            JdbcTemplate jdbcTemplate,
            @Value("${nexusos.purge.retention-days}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredSoftDeletedRecords() {
        int files = purgeTable("files");
        int documents = purgeTable("documents");
        int workspaces = purgeTable("workspaces");

        if (files > 0 || documents > 0 || workspaces > 0) {
            log.info("Purged expired soft-deleted records: workspaces={}, documents={}, files={}", workspaces, documents, files);
        }
    }

    private int purgeTable(String tableName) {
        String sql = "DELETE FROM " + tableName + " WHERE deleted_at IS NOT NULL AND deleted_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 day')";
        return jdbcTemplate.update(sql, retentionDays);
    }
}

# Database Schema

Use PostgreSQL, UUID primary keys, `created_at`, `updated_at`, optional `deleted_at`, and optimistic `version` where concurrent edits matter. Tenant tables include `organization_id`; workspace-scoped tables include `workspace_id`. Enforce foreign keys, unique constraints, and useful composite indexes. Flyway owns schema changes.

## Identity and tenancy

`users`, `user_profiles`, `credentials`, `oauth_accounts`, `email_verification_tokens`, `password_reset_tokens`, `sessions`, `refresh_tokens`, `mfa_methods`, `organizations`, `workspaces`, `memberships`, `roles`, `permissions`, `role_permissions`, `invitations`, `user_preferences`.

## Work management

`projects`, `project_members`, `tasks`, `task_dependencies`, `task_assignees`, `subtasks`, `labels`, `task_labels`, `comments`, `task_attachments`, `task_history`, `time_entries`, `recurrence_rules`.

## Collaboration and content

`channels`, `channel_members`, `direct_conversations`, `messages`, `message_reactions`, `message_reads`, `pinned_messages`, `documents`, `document_versions`, `document_comments`, `folders`, `files`, `file_versions`, `file_permissions`, `uploads`.

## Calendar, platform, and intelligence

`meetings`, `meeting_attendees`, `calendar_events`, `reminders`, `notifications`, `notification_preferences`, `integrations`, `webhook_deliveries`, `knowledge_sources`, `ingestion_jobs`, `document_chunks`, `embedding_refs`, `ai_conversations`, `ai_messages`, `ai_citations`, `saved_searches`, `activity_logs`, `audit_logs`, `analytics_events`, `daily_workspace_metrics`, `feature_flags`, `workspace_settings`, `usage_records`, `reports`, `outbox_events`.

## Critical constraints

- Normalized email is globally unique.
- Membership is unique by workspace/user.
- Channel names are unique within a workspace when not archived.
- Task position is indexed by project/status; task keys are unique per project.
- Message sequence supports stable cursor pagination.
- File object keys are server-generated and never accepted raw from clients.
- Chunk/vector metadata always includes organization, workspace, source, version, and ACL projection.
- Audit records are append-only at application permission level.

Indexes must support membership checks, active sessions, project task sorting, message cursors, unread notifications, due dates, audit lookup, outbox polling, and search synchronization. Add row-level-security documentation as defense in depth even if application authorization is primary.


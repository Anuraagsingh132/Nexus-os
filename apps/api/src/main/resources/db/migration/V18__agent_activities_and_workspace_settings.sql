CREATE TABLE agent_activities (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES users(id),
    tool_name VARCHAR(100) NOT NULL,
    arguments_json TEXT,
    status VARCHAR(30) NOT NULL,
    result_summary TEXT,
    error_message TEXT,
    requires_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_by UUID REFERENCES users(id),
    source_channel_id UUID REFERENCES channels(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE workspaces
ADD COLUMN agent_enabled BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN agent_mode VARCHAR(30) NOT NULL DEFAULT 'FULL_AGENT';

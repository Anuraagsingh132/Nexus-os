CREATE TABLE ingestion_failures (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    document_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    retried BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ingestion_failures_workspace ON ingestion_failures(workspace_id);

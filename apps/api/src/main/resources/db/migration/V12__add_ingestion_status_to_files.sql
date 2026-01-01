ALTER TABLE files
ADD COLUMN ingestion_status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN ingestion_error TEXT;

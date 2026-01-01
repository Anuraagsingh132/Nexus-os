# AI and Knowledge System

## Principles

AI is optional, provider-neutral, permission-aware, cited, observable, and resistant to prompt injection. It never silently performs destructive actions. Generated tasks/documents appear as previews requiring confirmation.

## Ingestion

Accept PDF, DOCX, PPTX, XLSX, Markdown/text, supported images with OCR, code, and safe metadata. Upload -> scan -> parse -> normalize -> chunk -> embed -> store -> index. Jobs are resumable and idempotent. Re-index on version or ACL change; delete vectors when sources are deleted.

## Retrieval

Authorize candidate sources before retrieval and again before response. Combine Elasticsearch keyword results and Qdrant vector results, rerank, assemble a bounded context, and return citations containing source ID, title, version, page/section, and permitted deep link. Never put raw secrets or hidden system prompts in model context.

## Capabilities

Workspace Q&A, PDF/document summary, meeting summary, email/document drafting, code explanation/review, sprint plans, chat-to-task extraction, and knowledge search. Each capability has typed input/output schemas, token/cost limits, timeout/cancellation, audit metadata, and deterministic fake-provider tests.

If evidence is missing, say so. Treat retrieved content as untrusted data, not instructions. Redact sensitive logs and defend against cross-workspace retrieval, indirect prompt injection, oversized context, and malicious file content.


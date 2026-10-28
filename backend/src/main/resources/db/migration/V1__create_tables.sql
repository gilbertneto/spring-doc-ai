CREATE TABLE generation_jobs (
    id                      UUID         PRIMARY KEY,
    project_name            VARCHAR(255) NOT NULL,
    source_type             VARCHAR(30)  NOT NULL,
    repository_url          VARCHAR(500),
    status                  VARCHAR(30)  NOT NULL,
    endpoints_count         INTEGER,
    classes_analyzed_count  INTEGER,
    error_message           TEXT,
    created_at              TIMESTAMP    NOT NULL,
    completed_at            TIMESTAMP
);

CREATE TABLE generated_documents (
    id         UUID         PRIMARY KEY,
    job_id     UUID         NOT NULL REFERENCES generation_jobs (id) ON DELETE CASCADE,
    doc_type   VARCHAR(40)  NOT NULL,
    file_name  VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL
);

CREATE INDEX idx_generation_jobs_created_at ON generation_jobs (created_at DESC);
CREATE INDEX idx_generated_documents_job_id ON generated_documents (job_id);

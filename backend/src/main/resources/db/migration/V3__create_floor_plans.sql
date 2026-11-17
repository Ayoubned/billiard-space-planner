CREATE TABLE planner.floor_plans (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    page_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_floor_plans_project
        FOREIGN KEY (project_id)
        REFERENCES planner.projects(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_floor_plans_file_size
        CHECK (file_size_bytes > 0),

    CONSTRAINT chk_floor_plans_page_count
        CHECK (page_count IS NULL OR page_count > 0),

    CONSTRAINT chk_floor_plans_status
        CHECK (status IN (
            'UPLOADED',
            'PROCESSING',
            'READY',
            'FAILED'
        )),

    CONSTRAINT chk_floor_plans_content_type
        CHECK (content_type IN (
            'application/pdf',
            'image/png',
            'image/jpeg'
        ))
);

CREATE INDEX idx_floor_plans_project_created
    ON planner.floor_plans(project_id, created_at DESC);

CREATE INDEX idx_floor_plans_status
    ON planner.floor_plans(status);
CREATE TABLE planner.projects (
    id UUID PRIMARY KEY,

    name VARCHAR(120) NOT NULL,
    client_name VARCHAR(120),
    site_address VARCHAR(250),
    description VARCHAR(2000),

    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_projects_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_projects_status
        CHECK (status IN (
            'DRAFT',
            'FLOOR_PLAN_PENDING',
            'FLOOR_PLAN_PROCESSING',
            'GEOMETRY_VALIDATION',
            'EQUIPMENT_SELECTION',
            'OPTIMIZATION',
            'LAYOUT_REVIEW',
            'QUOTATION',
            'INSTALLATION_PLANNING',
            'COMPLETED',
            'ARCHIVED'
        )),

    CONSTRAINT chk_projects_archive_consistency
        CHECK (
            (status = 'ARCHIVED' AND archived_at IS NOT NULL)
            OR
            (status <> 'ARCHIVED' AND archived_at IS NULL)
        )
);

CREATE INDEX idx_projects_status
    ON planner.projects(status);

CREATE INDEX idx_projects_active_updated_at
    ON planner.projects(updated_at DESC)
    WHERE archived_at IS NULL;

CREATE INDEX idx_projects_name_lower
    ON planner.projects(LOWER(name));

CREATE INDEX idx_projects_client_name_lower
    ON planner.projects(LOWER(client_name))
    WHERE client_name IS NOT NULL;
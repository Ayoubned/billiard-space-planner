CREATE TABLE planner.floor_plan_pages (
    id UUID PRIMARY KEY,
    floor_plan_id UUID NOT NULL,
    page_number INTEGER NOT NULL,
    width_points NUMERIC(10, 3) NOT NULL,
    height_points NUMERIC(10, 3) NOT NULL,
    rotation_degrees INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_floor_plan_pages_floor_plan
        FOREIGN KEY (floor_plan_id)
        REFERENCES planner.floor_plans(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_floor_plan_pages_number
        UNIQUE (floor_plan_id, page_number),

    CONSTRAINT chk_floor_plan_pages_number
        CHECK (page_number > 0),

    CONSTRAINT chk_floor_plan_pages_width
        CHECK (width_points > 0),

    CONSTRAINT chk_floor_plan_pages_height
        CHECK (height_points > 0),

    CONSTRAINT chk_floor_plan_pages_rotation
        CHECK (rotation_degrees IN (0, 90, 180, 270))
);

CREATE INDEX idx_floor_plan_pages_floor_plan
    ON planner.floor_plan_pages(floor_plan_id);


CREATE TABLE planner.floor_plan_image_metadata (
    floor_plan_id UUID PRIMARY KEY,
    format VARCHAR(10) NOT NULL,
    width_pixels INTEGER NOT NULL,
    height_pixels INTEGER NOT NULL,
    color_model VARCHAR(50) NOT NULL,
    dpi_x INTEGER,
    dpi_y INTEGER,
    physical_width_inches NUMERIC(12, 4),
    physical_height_inches NUMERIC(12, 4),

    CONSTRAINT fk_floor_plan_image_metadata_floor_plan
        FOREIGN KEY (floor_plan_id)
        REFERENCES planner.floor_plans(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_floor_plan_image_format
        CHECK (format IN ('PNG', 'JPEG')),

    CONSTRAINT chk_floor_plan_image_width
        CHECK (width_pixels > 0),

    CONSTRAINT chk_floor_plan_image_height
        CHECK (height_pixels > 0),

    CONSTRAINT chk_floor_plan_image_dpi_x
        CHECK (dpi_x IS NULL OR dpi_x > 0),

    CONSTRAINT chk_floor_plan_image_dpi_y
        CHECK (dpi_y IS NULL OR dpi_y > 0),

    CONSTRAINT chk_floor_plan_image_physical_width
        CHECK (physical_width_inches IS NULL OR physical_width_inches > 0),

    CONSTRAINT chk_floor_plan_image_physical_height
        CHECK (physical_height_inches IS NULL OR physical_height_inches > 0)
);
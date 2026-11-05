CREATE TABLE professional_categories (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE professionals (
    id BIGINT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_professionals_category
        FOREIGN KEY (category_id)
        REFERENCES professional_categories (id)
);

CREATE INDEX idx_professionals_category_id
    ON professionals (category_id);

CREATE TABLE weekly_schedules (
    id BIGINT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    day_of_week TEXT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_weekly_schedules_professional
        FOREIGN KEY (professional_id)
        REFERENCES professionals (id),

    CONSTRAINT chk_weekly_schedules_day_of_week
        CHECK (
            day_of_week IN (
                'MONDAY',
                'TUESDAY',
                'WEDNESDAY',
                'THURSDAY',
                'FRIDAY',
                'SATURDAY',
                'SUNDAY'
            )
        ),

    CONSTRAINT chk_weekly_schedules_slot_duration
        CHECK (slot_duration_minutes > 0)
);

CREATE INDEX idx_weekly_schedules_professional_id
    ON weekly_schedules (professional_id);

CREATE TABLE catalog_sync_state (
    state_id SMALLINT PRIMARY KEY,
    applied_version BIGINT NULL,

    CONSTRAINT chk_catalog_sync_state_singleton
        CHECK (state_id = 1)
);

INSERT INTO catalog_sync_state (state_id, applied_version)
VALUES (1, NULL);

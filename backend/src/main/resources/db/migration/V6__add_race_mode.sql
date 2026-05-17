-- Reference migration (Flyway is disabled; Hibernate ddl-auto:update handles schema)
-- Edition: added race_started_at and race_finished_at columns

ALTER TABLE edition ADD COLUMN IF NOT EXISTS race_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE edition ADD COLUMN IF NOT EXISTS race_finished_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE incident (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by_id BIGINT NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE incident_assignee (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incident(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(incident_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_incident_created_by ON incident(created_by_id);
CREATE INDEX IF NOT EXISTS idx_incident_assignee_incident ON incident_assignee(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_assignee_user ON incident_assignee(user_id);

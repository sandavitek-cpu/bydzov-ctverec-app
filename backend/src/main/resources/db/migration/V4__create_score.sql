CREATE TABLE IF NOT EXISTS score (
    id BIGSERIAL PRIMARY KEY,
    racer_registration_id BIGINT NOT NULL REFERENCES racer_registration (id),
    judge_id BIGINT NOT NULL REFERENCES app_user (id),
    run_number INT NOT NULL,
    points INT NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_score_racer ON score (racer_registration_id);
CREATE INDEX idx_score_judge ON score (judge_id);

CREATE TABLE IF NOT EXISTS crew_member (
    id BIGSERIAL PRIMARY KEY,
    racer_registration_id BIGINT NOT NULL REFERENCES racer_registration (id),
    user_id BIGINT REFERENCES app_user (id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_crew_member_registration ON crew_member (racer_registration_id);
CREATE INDEX IF NOT EXISTS idx_crew_member_user ON crew_member (user_id);

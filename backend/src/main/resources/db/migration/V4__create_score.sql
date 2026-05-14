CREATE TABLE IF NOT EXISTS edition (
    id BIGSERIAL PRIMARY KEY,
    edition_year INT NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS racer_registration (
    id BIGSERIAL PRIMARY KEY,
    edition_id BIGINT NOT NULL REFERENCES edition (id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    vehicle_description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_racer_registration_edition ON racer_registration (edition_id);

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS score (
    id BIGSERIAL PRIMARY KEY,
    racer_registration_id BIGINT NOT NULL REFERENCES racer_registration (id),
    judge_id BIGINT NOT NULL REFERENCES app_user (id),
    run_number INT NOT NULL,
    points INT NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_score_racer ON score (racer_registration_id);
CREATE INDEX IF NOT EXISTS idx_score_judge ON score (judge_id);

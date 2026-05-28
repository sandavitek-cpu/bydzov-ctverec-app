CREATE TABLE edition (
    id BIGSERIAL PRIMARY KEY,
    edition_year INT NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL
);

CREATE TABLE racer_registration (
    id BIGSERIAL PRIMARY KEY,
    edition_id BIGINT NOT NULL REFERENCES edition (id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    vehicle_description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_racer_registration_edition ON racer_registration (edition_id);

INSERT INTO edition (edition_year, label)
VALUES (2026, '30. ročník Novobydžovského čtverce');

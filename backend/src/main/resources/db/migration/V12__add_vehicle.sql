CREATE TABLE IF NOT EXISTS vehicle (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    vehicle_make VARCHAR(200),
    vehicle_plate VARCHAR(20),
    vehicle_year INT,
    vehicle_category VARCHAR(50),
    engine_displacement INT,
    power INT,
    max_speed INT,
    vehicle_notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vehicle_user ON vehicle (user_id);

CREATE TABLE IF NOT EXISTS route (
    id BIGSERIAL PRIMARY KEY,
    edition_id BIGINT NOT NULL REFERENCES edition(id),
    variant VARCHAR(30),
    name VARCHAR(200),
    total_distance DOUBLE PRECISION,
    published BOOLEAN,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS route_point (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES route(id),
    sort_order INTEGER,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    distance_from_start DOUBLE PRECISION
);

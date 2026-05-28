CREATE TABLE checkpoint (
    id BIGSERIAL PRIMARY KEY,
    edition_id BIGINT NOT NULL REFERENCES edition(id),
    name VARCHAR(200) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    radius INTEGER NOT NULL,
    sort_order INTEGER,
    task_description VARCHAR(1000),
    max_points INTEGER
);
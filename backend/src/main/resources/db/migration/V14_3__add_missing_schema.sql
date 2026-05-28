CREATE TABLE IF NOT EXISTS app_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_app_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    app_role_id BIGINT NOT NULL REFERENCES app_role(id),
    PRIMARY KEY (user_id, app_role_id)
);

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS username VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS first_name VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_name VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS member_since DATE;

UPDATE app_user SET username = email WHERE username = '';
UPDATE app_user SET first_name = name WHERE first_name = '';

ALTER TABLE app_user ALTER COLUMN name DROP NOT NULL;

ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS team_name VARCHAR(200);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS vehicle_category VARCHAR(50);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS vehicle_plate VARCHAR(20);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS vehicle_year INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS vehicle_make VARCHAR(200);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS crew_count INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS start_number INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS start_fee INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS variant VARCHAR(20);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS first_time BOOLEAN;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS driver_age INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS club VARCHAR(200);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS address VARCHAR(300);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS youngest_age INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS youngest_name VARCHAR(200);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS engine_displacement INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS power INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS max_speed INTEGER;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS vehicle_notes VARCHAR(500);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS notes VARCHAR(500);
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS contacted BOOLEAN;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS properly_registered BOOLEAN;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS arrived BOOLEAN;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS consent BOOLEAN;
ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS approved BOOLEAN;

CREATE TABLE IF NOT EXISTS schedule_item (
    id BIGSERIAL PRIMARY KEY,
    edition_id BIGINT NOT NULL REFERENCES edition(id),
    time VARCHAR(10) NOT NULL,
    label VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    sort_order INTEGER
);

ALTER TABLE checkpoint ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

CREATE TABLE IF NOT EXISTS checkpoint_volunteers (
    checkpoint_id BIGINT NOT NULL REFERENCES checkpoint(id),
    volunteer_name VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS race_category (
    id BIGSERIAL PRIMARY KEY,
    edition_id BIGINT NOT NULL REFERENCES edition(id),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    variant VARCHAR(20),
    determination VARCHAR(30) NOT NULL,
    sort_order INTEGER,
    winner_registration_id BIGINT,
    winner_name VARCHAR(200),
    winner_team VARCHAR(200),
    winner_number INTEGER,
    winner_points INTEGER
);

CREATE TABLE IF NOT EXISTS archive_entry (
    id BIGSERIAL PRIMARY KEY,
    edition_year INTEGER NOT NULL,
    rank INTEGER NOT NULL,
    racer_name VARCHAR(200) NOT NULL,
    vehicle VARCHAR(200),
    points INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS changelog (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS message_log (
    id BIGSERIAL PRIMARY KEY,
    recipient_type VARCHAR(50) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body VARCHAR(5000) NOT NULL,
    recipient_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

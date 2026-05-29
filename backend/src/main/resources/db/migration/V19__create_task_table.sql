CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    recommended_points INTEGER,
    tools TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE checkpoint_task (
    checkpoint_id BIGINT NOT NULL REFERENCES checkpoint(id) ON DELETE CASCADE,
    task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    PRIMARY KEY (checkpoint_id, task_id)
);

CREATE INDEX IF NOT EXISTS idx_checkpoint_task_checkpoint ON checkpoint_task (checkpoint_id);
CREATE INDEX IF NOT EXISTS idx_checkpoint_task_task ON checkpoint_task (task_id);

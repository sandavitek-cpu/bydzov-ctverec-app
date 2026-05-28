ALTER TABLE score ADD COLUMN IF NOT EXISTS checkpoint_id BIGINT;
ALTER TABLE score ADD CONSTRAINT IF NOT EXISTS fk_score_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoint(id);
CREATE INDEX IF NOT EXISTS idx_score_checkpoint ON score (checkpoint_id);

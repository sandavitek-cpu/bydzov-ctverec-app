ALTER TABLE racer_registration ADD COLUMN IF NOT EXISTS payment_reference INT;
CREATE INDEX IF NOT EXISTS idx_racer_registration_edition_payment_ref
    ON racer_registration (edition_id, payment_reference);

ALTER TABLE edition ADD COLUMN cancellation_deadline TIMESTAMP WITH TIME ZONE;
ALTER TABLE racer_registration ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE racer_registration ADD COLUMN refund_amount INTEGER;

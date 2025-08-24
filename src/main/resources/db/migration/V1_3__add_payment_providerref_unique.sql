-- Add unique constraint on payment.provider_ref to enforce providerRef uniqueness
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_payment_provider_ref'
    ) THEN
        ALTER TABLE payment ADD CONSTRAINT uk_payment_provider_ref UNIQUE (provider_ref);
    END IF;
EXCEPTION WHEN duplicate_column THEN
    -- ignore if already present
    NULL;
END$$;

-- Keep the legacy KYC table and rows in place; add the current fields to it.
-- This migration is additive and intentionally never drops or truncates data.
CREATE TABLE IF NOT EXISTS kyc_details (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    pan_masked VARCHAR(255),
    pan_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    aadhaar_reference_id VARCHAR(255),
    aadhaar_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    email_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    face_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    liveness_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    overall_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(1000),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    customer_id BIGINT,
    pan_number VARCHAR(255),
    aadhar_number VARCHAR(255)
);

ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS pan_masked VARCHAR(255);
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS pan_status VARCHAR(40) DEFAULT 'PENDING';
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS aadhaar_reference_id VARCHAR(255);
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS aadhaar_status VARCHAR(40) DEFAULT 'PENDING';
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS email_status VARCHAR(40) DEFAULT 'PENDING';
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS face_status VARCHAR(40) DEFAULT 'PENDING';
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS liveness_status VARCHAR(40) DEFAULT 'PENDING';
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS overall_status VARCHAR(40) DEFAULT 'PENDING';
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000);
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE kyc_details ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Backfill stable auth user IDs from the existing customer relationship when available.
DO $$
BEGIN
    IF to_regclass('public.customers') IS NOT NULL THEN
        EXECUTE 'UPDATE kyc_details k SET user_id = c.user_id FROM customers c WHERE k.customer_id = c.id AND k.user_id IS NULL';
    END IF;
END $$;

-- Preserve the legacy verified state and PAN information in masked form for the new UI.
UPDATE kyc_details
SET pan_masked = LEFT(pan_number, 2)
        || REPEAT('*', GREATEST(LENGTH(pan_number) - 4, 0))
        || RIGHT(pan_number, 2)
WHERE pan_masked IS NULL AND pan_number IS NOT NULL AND LENGTH(pan_number) >= 4;

UPDATE kyc_details
SET pan_status = CASE WHEN UPPER(COALESCE(status, '')) = 'VERIFIED' THEN 'VERIFIED' ELSE COALESCE(NULLIF(pan_status, ''), 'PENDING') END,
    aadhaar_status = COALESCE(NULLIF(aadhaar_status, ''), 'PENDING'),
    email_status = CASE WHEN UPPER(COALESCE(status, '')) = 'VERIFIED' THEN 'VERIFIED' ELSE COALESCE(NULLIF(email_status, ''), 'PENDING') END,
    face_status = COALESCE(NULLIF(face_status, ''), 'PENDING'),
    liveness_status = COALESCE(NULLIF(liveness_status, ''), 'PENDING'),
    overall_status = CASE WHEN UPPER(COALESCE(status, '')) = 'VERIFIED' THEN 'KYC_VERIFIED' ELSE COALESCE(NULLIF(overall_status, ''), 'PENDING') END,
    status = CASE
        WHEN UPPER(COALESCE(status, '')) IN ('VERIFIED', 'KYC_VERIFIED', 'PAN_VERIFIED', 'AADHAAR_VERIFIED', 'FACE_VERIFIED', 'EMAIL_VERIFIED', 'FAILED', 'REJECTED', 'PENDING') THEN UPPER(status)
        ELSE 'PENDING'
    END,
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

ALTER TABLE kyc_details ALTER COLUMN pan_status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN pan_status SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN aadhaar_status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN aadhaar_status SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN email_status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN email_status SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN face_status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN face_status SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN liveness_status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN liveness_status SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN overall_status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN overall_status SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE kyc_details ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE kyc_details ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE kyc_details ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE kyc_details ALTER COLUMN status SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_details_user_id ON kyc_details (user_id) WHERE user_id IS NOT NULL;

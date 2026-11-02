ALTER TABLE transactions ADD COLUMN IF NOT EXISTS sender_account_number VARCHAR(30);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS receiver_account_number VARCHAR(30);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS sender_amount NUMERIC(19, 4);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS sender_currency VARCHAR(3);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS receiver_amount NUMERIC(19, 4);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS receiver_currency VARCHAR(3);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(24, 12);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS exchange_rate_timestamp TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS transfer_fee NUMERIC(19, 4);
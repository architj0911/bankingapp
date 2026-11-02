CREATE SEQUENCE account_seq START 1000000000 INCREMENT BY 1;
CREATE TABLE account_audit(
	id BIGSERIAL PRIMARY KEY,
	account_number VARCHAR(30) NOT NULL,
	user_id BIGINT NOT NULL,
	action VARCHAR(100) NOT NULL,
	ip VARCHAR(100),
	device_id VARCHAR(255),
	created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_account_audit_account_number ON account_audit(account_number);
CREATE INDEX idx_account_audit_user_id ON account_audit(user_id);
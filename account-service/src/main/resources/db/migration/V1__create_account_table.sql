CREATE TABLE accounts (
	id BIGSERIAL PRIMARY KEY,
	account_number VARCHAR(20) UNIQUE NOT NULL,
	customer_id BIGINT NOT NULL,
	account_type VARCHAR(20),
	status VARCHAR(20),
	balance DECIMAL(15,2),
	created_at TIMESTAMP,
	updated_at TIMESTAMP
);
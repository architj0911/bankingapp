CREATE TABLE roles (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(50) UNIQUE NOT NULL
);
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    branch_id VARCHAR(20),
    employee_code VARCHAR(50)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE oauth_clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) UNIQUE NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    scope VARCHAR(255),
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE login_audit (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(100),
    success BOOLEAN,
    ip_address VARCHAR(50),
    reason VARCHAR(255),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(user_name);
CREATE INDEX idx_audit_username ON login_audit(user_name);

ALTER TABLE oauth_clients
ADD CONSTRAINT chk_client_active CHECK (active IN (true, false));
CREATE TABLE transactions ( 
   id BIGSERIAL PRIMARY KEY,    
   txn_id VARCHAR(50) UNIQUE NOT NULL,  
   account_number VARCHAR(30) NOT NULL, 
   type VARCHAR(20), 
   amount DECIMAL(15,2), 
   status VARCHAR(20), 
   reference_id VARCHAR(100),  

   created_at TIMESTAMP, 
   updated_at TIMESTAMP 
); 
 
CREATE TABLE transaction_audit ( 
   id BIGSERIAL PRIMARY KEY, 
   txn_id VARCHAR(50), 
   user_id BIGINT, 

   customer_id BIGINT, 
   action VARCHAR(50), 
   ip VARCHAR(100), 
   device_id VARCHAR(255), 
   created_at TIMESTAMP 
); 
 
CREATE INDEX idx_txn_account ON transactions(account_number); 
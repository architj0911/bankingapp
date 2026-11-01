# ApexBank Digital Banking Demo

A college project for a digital banking portal, built with a React frontend and Java Spring Boot microservices.

## Run locally

1. Copy `.env.example` to `.env` and replace its placeholders with local demo settings.
2. From the repository root, start the backend services:

   ```powershell
   docker compose up --build -d
   ```

3. In another terminal, start the frontend:

   ```powershell
   cd frontend
   npm ci
   npm run dev -- --host 0.0.0.0
   ```

4. Open `http://localhost:5173`.

For the hosting configuration and no-domain demo options, see [HOSTING.md](HOSTING.md). KYC provider setup is documented in [KYC_SETUP.md](KYC_SETUP.md).

## Project modules

- `frontend` — React and Vite user interface
- `api-gateway` — Spring Cloud Gateway entry point
- `auth-service` — registration, login, and verification
- `customer-service` — customer profile and KYC
- `account-service` — bank accounts
- `transaction-service` — transfers and currency conversion
- `config-server` and `eureka-server` — application configuration and service discovery

This is a coursework demo. Use synthetic data only; do not use real financial or identity records.

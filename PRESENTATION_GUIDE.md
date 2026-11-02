# Digital Banking System: Presentation and Viva Guide

This guide is based on the source code in this repository. It describes confirmed behavior and calls out anything that is incomplete, mocked, weakly protected, or not confirmed. The project is a working microservice prototype, not a production-grade banking core.

## 1. Executive Summary

The system is a digital banking application with a React frontend and several Spring Boot backend services:

- `auth-service`: user registration, password verification, OTP, OAuth2/JWT issuance, roles and permissions.
- `customer-service`: customer profile and KYC data.
- `account-service`: bank accounts, account numbers, balances, and account ownership checks.
- `transaction-service`: debit/credit transactions, money transfer, transaction history, reversal, audit records.
- `api-gateway`: one public backend entry point and JWT validation/routing.
- `eureka-server`: service discovery.
- `config-server`: Spring Cloud Config server backed by a Git repository.
- React/Vite frontend in `frontend/`.

The architecture is microservices-based because each main business capability has its own Spring Boot application, database, port, build, and deployment image. It is not a monolith. The services use separate PostgreSQL databases and communicate synchronously through OpenFeign/HTTP. Kafka is configured and used for events, but no consumers were found.

## 2. Actual Architecture

```text
                                   +----------------------+
                                   |  Config Server :8888 |
                                   |  Git-backed config   |
                                   +----------+-----------+
                                              |
+-------------+       HTTP/JWT       +--------v---------+
| React/Vite  | --------------------> | API Gateway :8080|
| frontend    |                       | routes + JWT     |
| :5173       |                       +---+----+----+----+
+-------------+                           |    |    |
                          routes by path  |    |    |
                 +------------------------+    |    +---------------------+
                 |                             |                          |
        +--------v---------+          +--------v---------+       +--------v---------+
        | Auth :9000       |          | Customer :8082   |       | Account :8083    |
        | users, OTP, JWT  |          | profile, KYC     |       | accounts,balance |
        +--------+---------+          +--------+---------+       +--------+---------+
                 |                             |                          |
        +--------v---------+          +--------v---------+       +--------v---------+
        | PostgreSQL auth  |          | PostgreSQL       |       | PostgreSQL       |
        | authdb           |          | customerdb      |       | accountdb       |
        +------------------+          +------------------+       +------------------+
                                                                            ^
                                                                            |
                                                        OpenFeign/HTTP      |
                                                        +-------------------+
                                                        |
        +-------------------------+                     |
        | Transaction :8084       | --------------------+
        | transfer, history       |     AccountClient + CustomerClient
        +------------+------------+
                     |
        +------------v------------+       +------------------------------+
        | PostgreSQL transaction |       | Redis, Kafka, Zookeeper       |
        | transactiondb         |       | OTP/locks/events              |
        +-------------------------+       +------------------------------+

                         Eureka Server :8761 discovers/registers services
```

Important qualification: the frontend normally calls the gateway. Service-to-service clients have URL defaults such as `http://localhost:8083`; Docker configuration supplies service names/URLs where configured. The exact runtime URL should be checked in each service `application.yml`.

### Technologies confirmed

- Java 21 and Maven wrapper.
- Spring Boot `3.4.10` and Spring Cloud `2024.0.0` from `banking-system-parent/pom.xml`.
- Spring Cloud Gateway.
- Spring Cloud Netflix Eureka Server/Client.
- Spring Cloud Config Server.
- Spring Security OAuth2 Authorization Server and Resource Server.
- JWT signed with an RSA key pair.
- Spring Data JPA/Hibernate and PostgreSQL.
- Flyway migrations in auth, account, and transaction services.
- Redis for OTP state, metadata, caching, and distributed locking support.
- Apache Kafka with Zookeeper for event publication.
- OpenFeign for synchronous service calls.
- Resilience4j protection around one account lookup path.
- React 19, Vite, Axios, React Router, and lucide-react.
- Docker Compose for infrastructure and service containers.

## 3. Startup and Deployment

### Docker startup

The authoritative container definition is `docker-compose.yml`. It defines:

- `zookeeper:2181`
- `kafka:9092`
- `redis:6379`
- `config-server:8888`
- `eureka-server:8761`
- `auth-service:9000`
- `customer-service:8082`
- `account-service:8083`
- `transaction-service:8084`
- `api-gateway:8080`
- four PostgreSQL containers on host ports `5432` through `5435`.

The service Dockerfiles copy already-built JARs from each `target/` directory. `docker compose up -d --no-build` starts the existing local images. A full `--build` can upload large `target/` contexts because service `.dockerignore` coverage is incomplete; this is an operational issue, not a business-flow feature.

### Script startup

`scratch/start_services.ps1` starts:

1. Eureka.
2. Waits 10 seconds.
3. Auth, customer, account, and transaction services.
4. Waits 12 seconds.
5. API gateway.

It does not start Config Server, PostgreSQL, Redis, Kafka, or Zookeeper. Its fixed sleeps are not readiness checks. For a presentation, Docker Compose is the more complete startup path.

### Practical startup sequence

1. Start Docker Desktop.
2. From the repository root run `docker compose up -d --no-build` when existing images are available.
3. Confirm `docker compose ps` shows all services `Up`.
4. Start the frontend from `frontend/` with `npm run dev -- --host 0.0.0.0`.
5. Open `http://localhost:5173/`.
6. Use the gateway URL for API calls: `http://localhost:8080`.
7. Open Eureka at `http://localhost:8761/` to show registered services.

`depends_on` starts containers in order but does not guarantee that Spring applications are ready. Wait for logs such as `Started ...Application` and Eureka registrations.

## 4. Service-by-Service Explanation

### 4.1 API Gateway

Location: `api-gateway/src/main/java/com/bank/gateway/`

Important files:

- `ApiGatewayApplication`: Spring Boot entry point.
- `config/SecurityConfig`: resource-server JWT validation, authority conversion, public/secured behavior.
- `config/GatewayConfig` or the gateway `application.yml`: route definitions.
- `filter/BankingRequestLoggingFilter`: correlation/request logging.
- gateway CORS/filter classes where present.

Responsibility:

- Receives frontend HTTP requests at port `8080`.
- Validates JWTs before protected traffic is routed.
- Applies route predicates and path rewriting.
- Adds centralized logging/correlation behavior.

Configured route families from `api-gateway/src/main/resources/application.yml`:

- `/api/auth/register` is rewritten to `/user/register` and sent to auth-service.
- `/api/auth/**` is rewritten to `/auth/**` and sent to auth-service.
- `/auth/**`, `/oauth2/**`, `/user/**`, `/admin/**` go to auth-service.
- `/api/customers/**` and `/api/kyc/**` go to customer-service after removing `/api`.
- `/api/accounts/**` goes to account-service after removing `/api`.
- `/api/transactions/**` goes to transaction-service after removing `/api`.

The UI therefore calls `/api/...`, while backend controllers use `/customers`, `/accounts`, and `/transactions`.

### 4.2 Config Server

Location: `config-server/`

Responsibility:

- Runs Spring Cloud Config Server on `8888`.
- Reads configuration from its configured Git-backed repository.
- Exposes configuration to clients that are configured to use it.

Confirmed limitation: most local service behavior is also present in each service's own `src/main/resources/application.yml`. Do not claim that every property is centrally managed unless the relevant client bootstrap/import configuration confirms it.

### 4.3 Eureka Server

Location: `eureka-server/`

Important class:

- `EurekaServerApplication`.

Responsibility:

- Runs the Eureka registry on `8761`.
- Receives service registrations.
- Lets clients discover registered service instances.

The logs confirm services register with Eureka. Docker `depends_on` does not itself perform discovery; the Spring Eureka client configuration does.

### 4.4 Auth Service

Location: `auth-service/src/main/java/com/bank/authservice/`

Main packages:

- `controller`: `UserController`, `LoginController`.
- `service`: `UserService`, `CustomUserDetailService`, `OtpService`, `AuditService`, `SmsService`, `EmailService`.
- `security`: `OtpAuthenticationConverter`, `OtpAuthenticationProvider`, `OtpAuthenticationToken`.
- `config`: `SecurityBeanConfig`, `AuthorizationServerConfig`, `ClientConfig`, `JwkConfig`.
- `domain`: `User`, `Role`, `Permission`, `Client`, `LoginAuditEntity`.
- `repository`: JPA repositories for those entities.
- `event`: OTP/login event handlers and failure listeners.
- `dto`: `RegisterRequest` and related request/response objects.

Responsibility:

- Registers users.
- Stores BCrypt password hashes.
- Assigns roles/permissions.
- Verifies username/password.
- Tracks failed login attempts and locks accounts after repeated failures.
- Generates and verifies OTP values in Redis.
- Issues JWT access tokens through a custom OAuth2 OTP grant.
- Publishes the JWK endpoint used by resource servers.

#### Registration

`POST /user/register` is implemented by `UserController.register(RegisterRequest)`.

`UserService.register(RegisterRequest)` persists a `User`, encodes the password using the configured `PasswordEncoder`, and assigns `ROLE_CUSTOMER`. The source pass found logic that can assign existing permissions to the customer role when that role has no permissions. Registration creates an auth user only; it does not create a customer profile or account.

#### Password login and OTP

`LoginController.login(String userName, String password, HttpServletRequest)`:

1. Loads the user from `UserRepository`.
2. Checks lock state and failed-attempt count.
3. Verifies the submitted password with BCrypt.
4. Resets failures after success.
5. Calls `OtpService.generateOtp(...)`.
6. Sends through `SmsService` and `EmailService`.
7. Returns only a message in a normal configuration, but `app.auth.expose-dev-otp: true` causes the OTP to be returned in the response.

`OtpService` stores the OTP and request metadata in Redis for 30 minutes. It also logs the OTP. `SmsService` and email behavior can fall back to local development logging rather than real delivery.

#### JWT issuance

The frontend then calls `POST /oauth2/token` with the custom grant `grant_type=otp`, username, OTP, and the registered client credentials.

- `OtpAuthenticationConverter.convert(HttpServletRequest)` recognizes `grant_type=otp` and creates an OTP authentication token.
- `OtpAuthenticationProvider.authenticate(Authentication)` verifies the OTP, loads the user, and creates token context.
- `AuthorizationServerConfig` installs the custom converter/provider into the authorization server token endpoint.
- `JwkConfig` creates a 2048-bit RSA key pair in memory.
- `AuthorizationServerConfig.tokenCostomizer()` adds `userId` and `authorities` claims.
- `ClientConfig` registers the in-memory client `api-gateway`, supports OTP, refresh token, and client credentials grants, and sets a three-hour access-token lifetime and seven-day refresh-token lifetime.

A restart regenerates the RSA key because it is not persisted; tokens issued before restart can become invalid.

### 4.5 Customer Service

Location: `customer-service/src/main/java/com/bank/customer/`

Important classes:

- `controller/CustomerController`.
- `controller/KycController`.
- `service/CustomerService`.
- `service/KycService`.
- `service/SetuService`.
- `entity/Customer`.
- `entity/KycDetails`.
- `repository/CustomerRepository`, `KycRepository`.
- `security/SecurityUtil`.
- `config/SetuConfig` and security configuration.

Responsibility:

- Creates and reads a customer profile for the authenticated auth user.
- Stores customer name, email, phone, status, and logical auth `userId`.
- Stores KYC details.
- Contains Setu WebClient integration methods for PAN/DigiLocker flows.

`CustomerService.create(CustomerRequest)` gets the JWT user ID, prevents a duplicate non-deleted profile, builds a customer with `PENDING` status, and saves it. `CustomerService.getProfileByUserId` and `getByUserId` retrieve profiles. Redis-backed `@Cacheable` methods exist for customer lookups.

The customer database uses Hibernate `ddl-auto: update`; no Flyway migration files were found for this service. The `Customer.userId` is a logical cross-database reference to auth `users.id`, not a PostgreSQL foreign key.

#### KYC qualification

`KycController` exposes `/kyc/me`, `/kyc/{customerId}`, `/kyc/verify_pan`, `/kyc/digilocker`, `/kyc/callback`, and `/kyc/status/{id}`.

`SetuService` contains WebClient calls and configuration properties, but `KycService` uses local behavior and external PAN/Aadhaar calls are commented out or not on the main submission path. The source therefore supports describing KYC as partially implemented/integration-ready, not as confirmed production verification. Also, `POST /kyc/{customerId}` accepts a customer ID and the source pass did not find a matching ownership check.

### 4.6 Account Service

Location: `account-service/src/main/java/com/bank/account/`

Important classes:

- `controller/AccountController`.
- `service/AccountService`.
- `service/AccountSequenceService`.
- `service/DistributedLockService`.
- `client/CustomerClient`.
- `entity/Account`.
- `repository/AccountRepository` and `AccountAuditRepository`.
- `dto/CreateAccountRequest`, `AccountResponse`, `AccountDto`, `BalanceUpdateRequest`, `CustomerDto`.
- `validator/AccountValidator`.
- `util/AccountNumberGenerator`.
- `security/SecurityUtil`.
- `config/SecurityConfig` and `FeignSecurityConfig`.

Responsibility:

- Creates accounts for the caller's customer profile.
- Generates account numbers.
- Stores account status and balance.
- Returns the caller's accounts.
- Returns internal account data to transaction-service.
- Contains intended locking/transactional balance-update logic.

#### Account creation

`AccountController.create(...)` requires `CREATE_ACCOUNT`, extracts IP/device headers, and calls `AccountService.accountCreate(...)`.

`AccountService.accountCreate(...)`:

1. Reads `userId` from the JWT using `SecurityUtil.getUserId()`.
2. Calls `CustomerClient.getCustomerByUserId(userId)`.
3. Gets the next value from PostgreSQL sequence `account_seq` through `AccountSequenceService.getNext()`.
4. Builds an account number using `AccountNumberGenerator.generator("4201", "10", seq)`.
5. Validates the initial deposit and customer.
6. Creates an `ACTIVE` `Account` with the initial balance.
7. Saves to the account database.
8. Attempts to publish `account-created-topic` to Kafka.
9. Returns `AccountResponse`.

The account-number generator creates a branch/type/sequence value plus a Luhn-style check digit.

#### Balance and ownership

`AccountService.getMyAccounts()` resolves the caller's customer and queries `AccountRepository.findByCustomerId(...)`.

`AccountService.getAccount(String)` resolves the caller's customer and checks `account.customerId` before returning the account.

There are two balance-update paths:

- Intended service path: `AccountService.updateBalance(BalanceUpdateRequest)` is `@Transactional`, uses `DistributedLockService`, calls `findByAccountNumberForUpdate`, checks blocked status, checks insufficient balance for debit, and applies debit/credit arithmetic.
- Actual transfer path: `AccountController.updateBalance(BalanceUpdateRequest)` directly loads the account, assigns `request.newBalance`, and saves it. `TransactionService` calls this controller through `AccountClient`, so transfer bypasses the intended lock and debit validation.

This distinction is one of the most important viva points.

### 4.7 Transaction Service

Location: `transaction-service/src/main/java/com/bank/transaction/`

Important classes:

- `controller/TransactionController`.
- `service/TransactionService`.
- `client/AccountClient` and `CustomerClient`.
- `resilience/AccountProtectionService`.
- `entity/Transaction` and `TransactionAudit`.
- `repository/TransactionRepository` and `TransactionAuditRepository`.
- `audit/TransactionAuditService`.
- `dto/TransactionRequest`, `TransactionResponse`, `TransferRequest`, `TransferResponse`, `AccountDto`, `CustomerDto`, `BalanceUpdateRequest`.
- `util/SecurityUtil`.
- `config/SecurityConfig` and `FeignSecurityConfig`.
- `enumrator/TransactionType` and `TransactionStatus`.

Responsibility:

- Performs a single-account debit or credit through `process(...)`.
- Performs two-account transfers through `transfer(...)`.
- Stores transaction rows and audit rows.
- Reads transaction history only for accounts belonging to the authenticated user.
- Publishes transaction events to Kafka.
- Uses Resilience4j around the protected account lookup path.

## 5. API Documentation

The gateway normally adds `/api` to the public frontend contract, while the service controllers use the internal path shown in the final column.

| Method | Public gateway endpoint | Service/controller endpoint | Service | Purpose | Authentication |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | `/user/register` | Auth | Register user | Public |
| POST | `/api/auth/login` | `/auth/login` | Auth | Validate password and issue OTP | Public |
| POST | `/oauth2/token` | `/oauth2/token` | Auth | Exchange OTP for JWT | OAuth client + OTP |
| POST | `/auth/login-success` | same | Auth | Login audit success | Auth service endpoint |
| POST | `/auth/login-failure` | same | Auth | Login audit failure | Auth service endpoint |
| POST/DELETE | `/admin/rbac/role/{roleId}/permission/{permissionId}` | same | Auth | Role-permission assignment/removal | `ROLE_ADMIN` |
| POST/DELETE | `/admin/user-role/user/{userId}/role/{roleId}` | same | Auth | User-role assignment/removal | `ROLE_ADMIN` |
| POST | `/api/customers` | `/customers` | Customer | Create customer profile | JWT |
| GET | `/api/customers/me` | `/customers/me` | Customer | Current profile | JWT |
| GET | internal | `/customers/internal?userId=...` | Customer | Lookup profile for services | JWT route; internal intent only |
| GET | `/api/kyc/me` | `/kyc/me` | Customer | Current KYC | JWT |
| POST | `/api/kyc/{customerId}` | `/kyc/{customerId}` | Customer | Submit KYC data | JWT |
| POST | `/api/kyc/verify_pan?pan=...` | `/kyc/verify_pan` | Customer | Setu PAN call | JWT route |
| GET | `/api/kyc/digilocker` | `/kyc/digilocker` | Customer | Start DigiLocker flow | JWT route |
| GET | `/api/kyc/callback` | `/kyc/callback` | Customer | KYC callback | Endpoint exists; behavior must be demonstrated from code |
| GET | `/api/kyc/status/{id}` | `/kyc/status/{id}` | Customer | Setu status | JWT route |
| POST | `/api/accounts` | `/accounts` | Account | Create account | `CREATE_ACCOUNT` |
| GET | `/api/accounts/me` | `/accounts/me` | Account | List caller accounts | JWT |
| GET | `/api/accounts/{accountNumber}` | `/accounts/{accountNumber}` | Account | Get owned account | JWT + ownership check |
| GET | internal | `/accounts/internal?accountNumber=...` | Account | Account lookup for transaction-service | JWT route; no explicit service-only authority |
| POST | internal | `/accounts/internal/update-balance` | Account | Direct balance update | JWT route; no explicit method authority |
| POST | `/api/transactions` | `/transactions` | Transaction | Single debit/credit | `TRANSFER_MONEY` |
| POST | `/api/transactions/transfer` | `/transactions/transfer` | Transaction | Two-account transfer | `TRANSFER_MONEY` |
| GET | `/api/transactions/me` | `/transactions/me` | Transaction | Caller transaction history | JWT |
| GET | `/api/transactions/account/{accountNumber}` | same | Transaction | Owned account history | JWT + ownership check |
| POST | `/api/transactions/reverse/{txnId}` | same | Transaction | Create compensating transaction | Endpoint has no explicit `@PreAuthorize` |

Request/response examples:

```json
POST /api/auth/register
{
  "userName": "alice",
  "password": "secret",
  "email": "alice@example.com",
  "phoneNumber": "+910000000000"
}
```

```json
POST /api/accounts
{
  "accountType": "SAVINGS",
  "initialDeposit": 10000
}
```

```json
POST /api/transactions/transfer
{
  "fromAccountNumber": "sender-account",
  "toAccountNumber": "receiver-account",
  "amount": 500,
  "description": "Test transfer"
}
```

The exact JSON field names should be checked against the DTOs before a live demo. `CreateAccountRequest` includes a typo `intialDeposit` but accepts `initialDeposit` through `@JsonAlias`.

## 6. Actual End-to-End Flows

### 6.1 Registration

```text
React Register page
  -> frontend authApi.js
  -> POST /api/auth/register
  -> gateway rewrite to POST /user/register
  -> UserController.register()
  -> UserService.register()
  -> BCrypt PasswordEncoder
  -> UserRepository.save()
  -> authdb users/user_roles
  -> JSON message + username + id
```

What does not happen: no Customer row and no Account row are created automatically. The demo must create the customer profile and account separately.

### 6.2 Login and JWT

```text
React Login page
  -> authApi.loginUser()
  -> POST /api/auth/login?userName=...&password=...
  -> gateway -> LoginController.login()
  -> UserRepository lookup
  -> BCrypt matches password
  -> OtpService.generateOtp()
  -> Redis OTP key with 30-minute TTL
  -> SMS/email/local log
  -> development response may include otp

Frontend submits OTP
  -> POST /oauth2/token, grant_type=otp
  -> OtpAuthenticationConverter.convert()
  -> OtpAuthenticationProvider.authenticate()
  -> OtpService.verifyOtp()
  -> CustomUserDetailService.loadUserByUsername()
  -> AuthorizationServerConfig token generator
  -> tokenCostomizer adds userId + authorities
  -> signed RSA JWT response
  -> frontend stores bank_access_token in localStorage
```

For subsequent calls:

```text
Axios interceptor adds Authorization: Bearer <JWT>
  -> gateway/resource server validates signature and claims
  -> service SecurityFilterChain validates same JWT
  -> JwtAuthenticationConverter reads authorities claim without adding prefix
  -> SecurityUtil reads userId claim
  -> controller/method authorization runs
```

The logged-in user is identified by the JWT `userId` claim, not by a client-supplied user ID in normal profile/account paths.

### 6.3 Customer creation

```text
POST /api/customers
  -> CustomerController.create()
  -> CustomerService.create()
  -> SecurityUtil.getUserId()
  -> CustomerRepository.findByUserIdAndIsDeletedFalse()
  -> build Customer(userId, name, email, phone, PENDING)
  -> CustomerRepository.save()
  -> CustomerResponse
```

### 6.4 Account creation

```text
POST /api/accounts
  -> gateway -> AccountController.create()
  -> @PreAuthorize("hasAuthority('CREATE_ACCOUNT')")
  -> AccountService.accountCreate()
  -> SecurityUtil.getUserId()
  -> CustomerClient.getCustomerByUserId(userId)
  -> AccountSequenceService.getNext()
  -> SELECT nextval('account_seq')
  -> AccountNumberGenerator.generator()
  -> AccountValidator.validateInitialDeposit()
  -> AccountValidator.validateCustomer()
  -> AccountRepository.save(Account)
  -> Kafka account-created-topic attempt
  -> AccountResponse(accountNumber, type, balance, status)
```

The account is linked to the customer with `customerId`. There is no cross-database foreign key.

### 6.5 Check balance

```text
GET /api/accounts/me
  -> gateway -> AccountController.getMyAccounts()
  -> AccountService.getMyAccounts()
  -> SecurityUtil.getUserId()
  -> CustomerClient.getCustomerByUserId()
  -> AccountRepository.findByCustomerId(customer.id)
  -> map(Account -> AccountResponse)
  -> frontend dashboard/account page
```

For one account:

```text
GET /api/accounts/{accountNumber}
  -> AccountController.getAccountByNumber()
  -> AccountService.getAccount()
  -> current JWT user -> customer-service lookup
  -> AccountRepository.findByAccountNumber()
  -> compare account.customerId with customer.id
  -> return balance
```

The balance comes from the `accounts.balance` column, not from transaction history calculation.

### 6.6 Money transfer: exact implementation

The main endpoint is `POST /transactions/transfer`, implemented by `TransactionController.transfer(...)`, protected by `TRANSFER_MONEY`.

```text
Frontend TransactionsPage
  -> transactionApi.transfer()
  -> POST /api/transactions/transfer
  -> gateway strips /api and routes to transaction-service
  -> TransactionController.transfer()
  -> TransactionService.transfer()
```

Inside `TransactionService.transfer(...)`:

1. `SecurityUtil.getUserId()` reads the authenticated JWT user.
2. Rejects null/non-positive amount.
3. Rejects missing accounts and same sender/receiver.
4. `AccountClient.getAccount(fromAcc)` calls `GET /accounts/internal`.
5. Rejects missing/blocked sender.
6. `CustomerClient.getCustomerByUserId(userId)` loads the caller's customer.
7. Compares sender `customerId` with caller customer ID. This is the main sender ownership check.
8. Checks the sender balance against the requested amount.
9. `AccountClient.getAccount(toAcc)` loads the receiver and rejects missing/blocked receiver.
10. Calculates `newSenderBalance = oldSenderBalance - amount`.
11. Calculates `newReceiverBalance = oldReceiverBalance + amount`.
12. Calls `POST /accounts/internal/update-balance` for sender.
13. Calls the same endpoint for receiver.
14. If receiver credit fails, attempts compensation by assigning the original sender balance back.
15. Saves a successful debit `Transaction` row with `txnId + "-D"`.
16. Saves a successful credit `Transaction` row with `txnId + "-C"`.
17. Writes a `TransactionAudit` record using `TransactionAuditService.log(...)`.
18. Publishes the debit transaction to Kafka topic `transaction-event`; publish failure is logged and does not fail the response.
19. Returns `TransferResponse` with the base transaction ID, accounts, amount, status, description, and timestamp.

#### Is it synchronous or asynchronous?

The balance changes and transaction row saves are synchronous HTTP/JPA operations. Kafka publication is asynchronous/event-oriented. The transfer API waits for the two HTTP calls and database saves before returning.

#### Is it transactional?

`TransactionService.transfer` has `@Transactional`, but that transaction covers the transaction-service database connection. It cannot atomically include the account-service database because the balance writes are separate HTTP requests. Therefore it is not a distributed transaction.

#### Does it prevent insufficient balance?

The transfer method checks the sender's previously read balance before calculating the new balance. However, the actual Feign request reaches `AccountController.updateBalance`, which directly assigns `newBalance`; it does not call `AccountService.updateBalance`, where the lock and service-level debit check exist. So the check exists, but the final write is vulnerable to stale reads and races.

#### Does it prevent duplicate transfers?

The two-account `transfer` method generates random transaction references for debit/credit and has no client idempotency key. Retrying the same request can perform another transfer. The separate single-account `process` method does look up an existing `referenceId`, but that protection does not automatically protect `/transfer`.

#### What if the receiver credit fails?

The code attempts a compensating sender update. If compensation also fails, it logs a critical error and throws. There is no durable compensation queue, reconciliation record, saga state, or distributed transaction. The sender may remain debited.

#### What protects the receiver?

The code checks receiver existence and `BLOCKED` status. It does not check receiver ownership because receiving money does not require the receiver to belong to the caller. The internal endpoint is not explicitly restricted to a service authority in `AccountController`.

### 6.7 Transaction history

```text
GET /api/transactions/me
  -> TransactionController.getMyTransactions()
  -> AccountClient.getMyAccounts()
  -> get caller account numbers
  -> optional accountNumber ownership check
  -> TransactionRepository.findByAccountNumber...OrderByCreateAtDesc()
  -> transaction rows returned
```

`GET /transactions/account/{accountNumber}` repeats the ownership check against the caller's account list before querying one account.

History is stored in the transaction database as rows in `transactions`. It is not reconstructed from current account balance. A transfer creates two rows: one `DEBIT` for the sender and one `CREDIT` for the receiver.

### 6.8 Single-account debit/credit

`POST /transactions` calls `TransactionService.process(...)` and is protected by `TRANSFER_MONEY`.

This path has more transaction-oriented logic than `/transfer`:

- Checks existing `referenceId` for idempotent replay.
- Creates a `PENDING` row.
- Uses `AccountProtectionService.getAccountWithProtection(...)` with retry, bulkhead, circuit breaker, and time limiter annotations.
- Rejects blocked accounts and amounts over `50000`.
- Checks balance for debit.
- Calls `AccountClient.updateBalance(...)` with the computed target balance.
- Marks the transaction successful, writes audit, and publishes Kafka.
- On failure, attempts to mark the row `FAILED` and writes a failure audit.

The DTO lacks strong Bean Validation annotations, so malformed null fields may fail later with runtime exceptions.

### 6.9 Reversal

`POST /transactions/reverse/{txnId}`:

1. Loads the original transaction by `txnId`.
2. Requires original status `SUCCESS`.
3. Builds an opposite `TransactionRequest` for the same account and amount.
4. Calls `TransactionService.process(reverseReq, "System", "reversal")`.

Important weakness: the endpoint has no explicit `@PreAuthorize`, does not visibly check ownership in the controller, does not mark the original row as reversed, and does not prevent repeated reversal. Describe it as a compensating transaction mechanism, not a complete reversal workflow.

## 7. Database and ER Explanation

### Auth database

Flyway files:

- `auth-service/src/main/resources/db/migration/V1__create_user_and_roles.sql`
- `V2__role_permission.sql`
- `V3__add_phone_email.sql`

Important tables/entities:

- `users`: user identity, username, password hash, failed attempts/lock fields, phone/email.
- `roles`: role definitions such as customer/admin.
- `permissions`: authorities such as `CREATE_ACCOUNT`, `VIEW_ACCOUNT`, `TRANSFER_MONEY`, and `APPROVE_TRNASACTION`.
- `user_roles`: many-to-many join between users and roles.
- `role_permissions`: many-to-many join between roles and permissions.
- `clients`: represented by a domain entity, but the OAuth registered client is configured in memory in `ClientConfig`.
- login audit entity/table: successful and failed login records.

### Customer database

Entities/tables generated or updated by Hibernate:

- `Customer`: generated ID, logical `userId`, name, email, phone, status, deletion flag, timestamps, created-by information.
- `KycDetails`: KYC values/status and `customer_id` relationship.

Relationship: one customer can have related KYC detail rows as represented by `KycDetails.customer` many-to-one. The source should be checked for uniqueness constraints before claiming one-to-one.

### Account database

Flyway files:

- `V1__create_account_table.sql`
- `V2__add_version_column.sql`
- `V3__create_account_sequence.sql`

`Account` maps to `accounts`:

- Primary key: generated `id`.
- `accountNumber`: business identifier.
- `customerId`: logical reference to customer-service, no cross-database FK.
- `accountType`: enum stored as string.
- `status`: enum stored as string.
- `balance`: `BigDecimal`.
- `createdAt`, `updatedAt`.
- `version`: JPA optimistic-lock field.

`account_seq` generates the numeric sequence used to build account numbers.

### Transaction database

Flyway files:

- `V1__create_transaction_table.sql`
- `V2__add_reference_id_unique.sql`

`Transaction` maps to `transactions`:

- Primary key: generated `id`.
- `txnId`: application transaction ID.
- `accountNumber`: string, not a database FK.
- `type`: `DEBIT` or `CREDIT`.
- `status`: pending/success/failed enum.
- `amount`.
- unique nullable `reference_id`.
- `created_at`, `updated_at`.

`TransactionAudit` stores transaction ID, user ID, action, IP, device ID, and timestamp. There is no confirmed database FK between audit and transaction entities.

### Verbal ER explanation

"Auth owns users, roles, and permissions. Customer owns the profile and KYC record. Account owns the account and balance, with a logical customer ID pointing across services. Transaction owns the ledger-like debit and credit rows, with an account number stored as a logical reference. Because every service has its own database, PostgreSQL foreign keys cannot enforce relationships across services. The application performs those checks through Feign calls."

## 8. Security: What Is Actually Implemented

### Password storage

`SecurityBeanConfig.passwordEncoder()` returns `BCryptPasswordEncoder`. The password stored by `UserService` is therefore a BCrypt hash, not plaintext, assuming the normal registration path is used.

### Auth-service security

`SecurityBeanConfig`:

- Disables CSRF for API use.
- Permits `/user/register`, `/auth/**`, and `/error`.
- Requires `ROLE_ADMIN` for `/admin/**`.
- Requires authentication elsewhere.
- Configures resource-server JWT handling.
- Reads authorities from the custom `authorities` claim with no extra prefix.

`AuthorizationServerConfig` installs the OAuth2 authorization-server filter chain at order `-1` and the OTP converter/provider at the token endpoint.

### Downstream JWT security

Account, customer, and transaction services have resource-server `SecurityFilterChain` configurations. They disable CSRF, permit OPTIONS and usually actuator paths, require authentication for other routes, and use a `JwtAuthenticationConverter` that reads the custom `authorities` claim without adding `SCOPE_` or another prefix.

`SecurityUtil` implementations read:

- JWT from `SecurityContextHolder`.
- `userId` claim for identity.
- `sub` for username where needed.

### Endpoint authorities

- Account creation: `CREATE_ACCOUNT`.
- Single transaction and transfer: `TRANSFER_MONEY`.
- Admin RBAC endpoints: `ROLE_ADMIN`.
- Reads are generally authenticated and then protected by ownership checks in service/controller code.

### What prevents cross-user account access?

- Account service resolves the caller's user ID from the JWT.
- It calls customer-service to resolve the caller's customer ID.
- `AccountService.getAccount` compares the account's `customerId` to that ID.
- Transaction history obtains the caller's account list and checks requested account numbers against it.
- Transfer checks that the sender account belongs to the authenticated caller's customer.

### Security weaknesses to state honestly

- Development OTP is exposed in the login response and logs.
- OTP delivery can fall back to logs.
- OAuth client secret `secret123` is configured in source/config rather than external secret management.
- RSA signing keys are generated in memory and change on restart.
- Internal account endpoints have no explicit service-only role check visible in `AccountController`.
- KYC submission uses a caller-supplied customer ID without a confirmed ownership check.
- Reversal lacks explicit authority/ownership protections.
- CORS and CSRF settings are development-oriented; production policy is not confirmed.

## 9. Frontend Structure and Known Contract Issues

Location: `frontend/`

Important files:

- `src/App.jsx`: application routing/layout.
- `src/context/AuthContext.jsx`: token storage/decode/current auth state.
- `src/api/axiosClient.js`: Axios base client and JWT/401 interceptor.
- `src/api/authApi.js`: registration/login/OTP calls.
- `src/api/customerApi.js`: customer/KYC calls.
- `src/api/accountApi.js`: account calls.
- `src/api/transactionApi.js`: transfer/history calls.
- `src/pages/LoginPage.jsx`, `RegisterPage.jsx`, `DashboardPage.jsx`, `CustomerPage.jsx`, `AccountsPage.jsx`, `TransactionsPage.jsx`.

The frontend routes include `/login`, `/register`, `/dashboard`, `/customer`, `/accounts`, and `/transactions`.

Source-confirmed mismatches worth testing before a live demo:

- Backend `AccountResponse` exposes `type`; some UI code reads `accountType`.
- Backend KYC uses `aadharNumber`; frontend submits `aadhaarNumber` in at least one flow.
- `getCustomerByUserId()` ignores its argument and calls `/api/customers/me`.
- Transfer result UI checks `transactionId || id`, while backend response uses `txnId`.

These may affect display only or may break specific screens. Demonstrate from the actual UI and use browser/network logs if an examiner asks.

## 10. Incomplete, Mock, Dead, or Risky Functionality

### Partially implemented or mocked

- KYC local verification behavior is effectively mocked or defaults to successful flags in `KycService`; external calls in `SetuService` are not confirmed as the main submission path.
- SMS/email can log OTPs locally instead of sending them.
- Kafka producers exist, but no consumers were found. Events are therefore not confirmed to trigger downstream behavior.
- `AccountAuditService` exists but the source pass did not find account creation/balance code calling it.
- `AuthenticationService.authenticate()` exists but is not the main `/auth/login` path.
- `TransactionService.getTransactionsForAccounts()` appears unused by the controller.

### Transfer risks

- Two account updates are not one distributed transaction.
- The actual internal controller writes a target balance directly and bypasses `AccountService.updateBalance` locking and validation.
- Read-then-write arithmetic can lose concurrent updates.
- Compensation can fail.
- `/transfer` has no idempotency key.
- Kafka publish failure does not fail the API response.

### Other weaknesses

- `@Transactional` in transaction-service cannot roll back account-service writes.
- DTOs have little Bean Validation coverage.
- Error responses are mostly generic runtime exceptions; stable error contracts are not confirmed.
- Reversal can be repeated and does not mark the original as reversed.
- Cross-service relationships are application-level, not database-enforced.
- Config may be duplicated between Config Server and local YAML.
- Only one Java test file was found, in Eureka; broad business-flow test coverage was not found.

## 11. Know These Classes Before Presenting

| Class/file | Purpose | Why it matters in viva |
|---|---|---|
| `docker-compose.yml` | Entire runtime topology | Shows services, ports, databases, Kafka, Redis |
| `frontend/src/App.jsx` | Frontend routes | Shows actual UI modules |
| `frontend/src/api/axiosClient.js` | JWT injection and 401 handling | Explains frontend security |
| `frontend/src/api/authApi.js` | Auth requests | Explains login/register contract |
| `frontend/src/api/transactionApi.js` | Transfer/history requests | Explains user operation calls |
| `auth-service/.../UserController.java` | Registration endpoint | First auth flow class |
| `auth-service/.../UserService.java` | User persistence and roles | Password/role explanation |
| `auth-service/.../LoginController.java` | Password login and OTP | Exact login implementation |
| `auth-service/.../OtpService.java` | Redis OTP generation/verification | OTP mechanics |
| `auth-service/.../OtpAuthenticationConverter.java` | Reads custom OTP grant | OAuth token flow |
| `auth-service/.../OtpAuthenticationProvider.java` | Verifies OTP and creates auth | Token issuance |
| `auth-service/.../AuthorizationServerConfig.java` | OAuth2 filters/custom JWT claims | JWT internals |
| `auth-service/.../JwkConfig.java` | In-memory RSA keys | Token signing and restart weakness |
| `auth-service/.../SecurityBeanConfig.java` | BCrypt and auth rules | Password/security config |
| `customer-service/.../CustomerController.java` | Customer API | Profile flow |
| `customer-service/.../CustomerService.java` | Customer persistence | User/customer relationship |
| `customer-service/.../KycController.java` | KYC endpoints | Partial feature discussion |
| `customer-service/.../KycService.java` | KYC local behavior | Mock/incomplete behavior |
| `customer-service/.../SetuService.java` | External KYC client | External dependency |
| `account-service/.../AccountController.java` | Account API and internal balance path | Critical transfer caveat |
| `account-service/.../AccountService.java` | Account creation/ownership/lock path | Main account logic |
| `account-service/.../Account.java` | Account table mapping | Database explanation |
| `account-service/.../AccountNumberGenerator.java` | Account number/check digit | Account design choice |
| `account-service/.../DistributedLockService.java` | Intended Redis lock | Consistency discussion |
| `transaction-service/.../TransactionController.java` | Transfer/history/reversal API | Entry points |
| `transaction-service/.../TransactionService.java` | Core transaction logic | Most important viva class |
| `transaction-service/.../AccountClient.java` | Feign account calls | Inter-service communication |
| `transaction-service/.../AccountProtectionService.java` | Resilience4j account lookup | Failure handling |
| `transaction-service/.../Transaction.java` | Transaction table mapping | Ledger/history explanation |
| `transaction-service/.../TransactionAuditService.java` | Audit persistence | Audit trail |
| `api-gateway/.../application.yml` | Routing | Frontend-to-service path mapping |
| `api-gateway/.../SecurityConfig.java` | Gateway JWT validation | Edge security |

## 12. 5-10 Minute Presentation Script

### 1. Problem statement

"The project solves a digital banking use case where a user can register, complete a customer profile, create a bank account, view balances, transfer money, and view transaction history. The goal was to separate identity, customer, account, and transaction responsibilities rather than putting all business logic in one application."

### 2. What the system does

"A user registers through the auth service. Login first verifies the password and then generates an OTP. After the OTP exchange, the user receives a JWT. The frontend sends that JWT to the gateway for customer, account, and transaction operations. A registered user creates a profile and then an account. Transfers debit one account, credit another, and create debit and credit history rows."

### 3. Architecture

"The system is a Spring Boot microservice architecture. React is the presentation layer. Spring Cloud Gateway is the public entry point. Eureka handles registration and discovery, and Config Server provides centralized configuration. Auth, customer, account, and transaction services each have their own PostgreSQL database. Redis supports OTP and locking-related functions. Kafka is used for event publication."

### 4. Technologies

"The backend uses Java 21, Spring Boot, Spring Security, OAuth2 Authorization Server and Resource Server, JPA, PostgreSQL, Flyway, OpenFeign, Eureka, Gateway, Redis, Kafka, and Resilience4j. The frontend uses React, Vite, Axios, and React Router. Docker Compose runs the environment."

### 5. Services

"Auth owns users, roles, permissions, password hashes, OTP, and JWTs. Customer owns the profile and KYC. Account owns account numbers and balances. Transaction owns transfers, debit/credit rows, history, and audit records. The gateway routes external requests, while Eureka and Config Server are platform services."

### 6. Authentication and security

"The password is encoded with BCrypt. Successful password validation creates a Redis OTP with a 30-minute TTL. The custom OTP grant is handled by OtpAuthenticationConverter and OtpAuthenticationProvider. AuthorizationServerConfig creates an RSA-signed JWT containing userId and authorities. Downstream resource servers read the authorities claim and SecurityUtil reads the userId claim. Method security protects account creation and money transfer."

"There are development shortcuts: the OTP is exposed and logged, the signing key is generated in memory, and some internal endpoints are not explicitly service-only. I would not describe this configuration as production-ready without those changes."

### 7. Registration and login

"Registration reaches UserController and UserService and creates only an auth user. Login reaches LoginController, verifies BCrypt, creates the OTP through OtpService, and returns the development OTP. The frontend sends that OTP to `/oauth2/token`. The token is stored in local storage and added by the Axios interceptor."

### 8. Account flow

"AccountController requires CREATE_ACCOUNT. AccountService gets the JWT user ID, calls customer-service to find the customer, obtains the next database sequence value, generates an account number with a check digit, validates the deposit, saves the Account entity, and tries to publish an account-created Kafka event."

### 9. Money transfer

"The transfer endpoint is TransactionController `/transactions/transfer`. TransactionService validates amount and account numbers, reads the sender account, confirms the sender belongs to the JWT user's customer, checks balance and blocked status, reads the receiver, calculates both target balances, and calls account-service twice. It then writes one debit and one credit transaction and an audit row."

"The important limitation is that the two account writes are separate HTTP/database operations. The transaction-service `@Transactional` annotation cannot roll back the account database. The code attempts compensation if receiver credit fails, but this is not a distributed transaction. Also, the Feign call reaches AccountController's direct assignment endpoint and bypasses the stronger lock/check path in AccountService."

### 10. Database

"Each bounded capability has its own database. Auth has users, roles, permissions, and join tables. Customer has Customer and KycDetails. Account has accounts and an account sequence. Transaction has transactions and audit rows. Cross-service IDs such as userId, customerId, and accountNumber are logical references, not foreign keys."

### 11. Inter-service communication

"Synchronous calls use OpenFeign. Account calls customer-service to resolve the customer. Transaction calls account-service for account reads and writes and customer-service for ownership. Kafka publication is asynchronous, but no consumers were found, so it currently acts mainly as an event output."

### 12. Important technical decisions

"The project chose separate databases to preserve service ownership, JWT to avoid server-side session state, Redis for short-lived OTP and distributed-lock support, Eureka for discovery, and Kafka for future event-driven integration. Account numbers use a sequence and check digit."

### 13. Challenges

"The hardest design problem is consistency across account-service and transaction-service. The current implementation compensates on failure but does not provide atomic distributed updates or durable recovery. Other challenges are service startup readiness, token key persistence, external KYC integration, and keeping frontend DTO names aligned with backend contracts."

### 14. Future improvements

"I would move balance mutation behind one protected account-service command that performs an atomic database update with optimistic or pessimistic locking. I would add an idempotency key and a durable saga/outbox workflow. I would persist JWT signing keys, move secrets to a secret manager, remove development OTP exposure, enforce service-to-service authorities, secure reversal, add Bean Validation, add readiness probes, and add integration tests for concurrent and failure cases."

### 15. Conclusion

"The project demonstrates a complete microservice banking prototype with real registration, OTP/JWT login, customer onboarding, account creation, balance lookup, transfer, and history paths. Its strongest presentation point is that the code shows both the intended architecture and the practical tradeoffs: the transfer works in the normal case, but it is not a distributed atomic transaction and needs stronger consistency controls for production."

## 13. Viva Questions

For each answer, the file/class is the best place to demonstrate the point.

### Easy

1. **Q: Is this monolithic or microservices?**  
   **Short:** Microservices.  
   **Detailed:** Auth, customer, account, and transaction are separate Spring Boot applications with separate ports/databases and Dockerfiles.  
   **Look at:** `docker-compose.yml`, each service's `*Application.java`.

2. **Q: What is the role of the API Gateway?**  
   **Short:** One public entry point for routing and JWT validation.  
   **Detailed:** It receives `/api/...`, rewrites paths, routes to services, applies gateway security and logging.  
   **Look at:** `api-gateway/src/main/resources/application.yml`, gateway `SecurityConfig`.

3. **Q: Why use Eureka?**  
   **Short:** Service registration and discovery.  
   **Detailed:** Services register with Eureka and clients can resolve logical service names instead of hardcoding every instance.  
   **Look at:** `eureka-server/EurekaServerApplication.java`, service application YAML.

4. **Q: Which databases are used?**  
   **Short:** PostgreSQL, one database per main business service.  
   **Detailed:** Auth, customer, account, and transaction have separate PostgreSQL databases/containers.  
   **Look at:** `docker-compose.yml`, each service `application.yml`.

5. **Q: Where is the password stored?**  
   **Short:** As a BCrypt hash in auth-service.  
   **Detailed:** `SecurityBeanConfig` provides `BCryptPasswordEncoder`; `UserService` encodes before saving.  
   **Look at:** `auth-service/.../SecurityBeanConfig.java`, `UserService.java`.

6. **Q: What does registration create?**  
   **Short:** An auth user only.  
   **Detailed:** It does not automatically create Customer or Account rows.  
   **Look at:** `UserController.java`, `UserService.java`.

7. **Q: What is stored in the JWT?**  
   **Short:** `userId` and `authorities`, plus standard JWT claims.  
   **Detailed:** The customizer reads the database user and adds those custom claims.  
   **Look at:** `AuthorizationServerConfig.java`.

8. **Q: How does the frontend send the token?**  
   **Short:** Axios interceptor adds a Bearer token.  
   **Detailed:** It reads `bank_access_token` and attaches `Authorization`; a 401 redirects to login.  
   **Look at:** `frontend/src/api/axiosClient.js`.

9. **Q: What is Redis used for?**  
   **Short:** OTP state, metadata, caching, and lock support.  
   **Detailed:** OtpService stores OTP/metadata with TTL; services also define caches and account lock support.  
   **Look at:** `OtpService.java`, `DistributedLockService.java`.

10. **Q: What is Kafka used for?**  
    **Short:** Publishing account and transaction events.  
    **Detailed:** Account creation publishes `account-created-topic`; transactions publish `transaction-event`. No consumer was found.  
    **Look at:** `AccountService.java`, `TransactionService.java`.

11. **Q: How is an account number generated?**  
    **Short:** Sequence plus branch/type prefix and check digit.  
    **Detailed:** `AccountSequenceService` calls `nextval('account_seq')`; `AccountNumberGenerator` builds the value and checksum.  
    **Look at:** those two account-service classes.

12. **Q: What is the account creation permission?**  
    **Short:** `CREATE_ACCOUNT`.  
    **Detailed:** AccountController uses `@PreAuthorize("hasAuthority('CREATE_ACCOUNT')")`.  
    **Look at:** `AccountController.java`.

13. **Q: What is the transfer permission?**  
    **Short:** `TRANSFER_MONEY`.  
    **Detailed:** Both transaction POST methods are protected with that authority.  
    **Look at:** `TransactionController.java`.

14. **Q: Where does balance come from?**  
    **Short:** The `accounts.balance` column.  
    **Detailed:** `/accounts/me` queries AccountRepository and maps Account.balance.  
    **Look at:** `Account.java`, `AccountService.java`.

15. **Q: What are debit and credit?**  
    **Short:** Enum values in transaction rows.  
    **Detailed:** A transfer creates a DEBIT row for the sender and CREDIT row for the receiver.  
    **Look at:** `TransactionType.java`, `TransactionService.transfer()`.

### Medium

16. **Q: Explain the login flow.**  
    **Short:** Password, OTP, then OAuth2 OTP grant for JWT.  
    **Detailed:** LoginController verifies BCrypt and generates Redis OTP; converter/provider verify OTP and generate the token.  
    **Look at:** `LoginController`, `OtpService`, `OtpAuthenticationConverter`, `OtpAuthenticationProvider`.

17. **Q: How are JWT authorities read?**  
    **Short:** From the custom `authorities` claim without a prefix.  
    **Detailed:** Each resource-server converter sets the claim name and empty prefix so `hasAuthority` matches values like `CREATE_ACCOUNT`.  
    **Look at:** service `SecurityConfig.java` files.

18. **Q: How does a service identify the user?**  
    **Short:** `SecurityUtil` reads `userId` from the JWT.  
    **Detailed:** It obtains `JwtAuthenticationToken` from SecurityContextHolder and reads the custom claim.  
    **Look at:** auth/customer/account/transaction `SecurityUtil.java`.

19. **Q: How does account ownership work?**  
    **Short:** JWT user -> customer-service customer ID -> account.customerId comparison.  
    **Detailed:** AccountService calls customer-service and rejects accounts with a different customer ID.  
    **Look at:** `AccountService.getAccount()`.

20. **Q: How does transfer validate the sender?**  
    **Short:** It compares sender account customer ID to authenticated customer ID.  
    **Detailed:** TransactionService gets userId from JWT, calls CustomerClient, and throws on mismatch.  
    **Look at:** `TransactionService.transfer()`.

21. **Q: How is insufficient balance checked?**  
    **Short:** Transfer checks sender balance before calculating debit.  
    **Detailed:** It rejects when `fromAccount.balance < amount`; the intended AccountService update path also has a debit check.  
    **Look at:** `TransactionService.transfer()`, `AccountService.updateBalance()`.

22. **Q: Is transfer one database transaction?**  
    **Short:** No.  
    **Detailed:** The transaction-service annotation covers its own database, while account updates happen in another service/database through HTTP.  
    **Look at:** `TransactionService.transfer()`, `AccountClient`.

23. **Q: What happens if receiver credit fails?**  
    **Short:** Best-effort compensation restores sender balance.  
    **Detailed:** It makes another account-service call; if compensation fails, it logs critical failure and throws.  
    **Look at:** the catch blocks in `TransactionService.transfer()`.

24. **Q: Is transfer idempotent?**  
    **Short:** The two-account transfer is not reliably idempotent.  
    **Detailed:** It generates fresh random references for each request; the reference check belongs to the separate `process()` path.  
    **Look at:** `TransactionService.process()` and `transfer()`.

25. **Q: What protection does Resilience4j provide?**  
    **Short:** Retry, bulkhead, circuit breaker, and time limiter around account lookup.  
    **Detailed:** `AccountProtectionService` wraps an asynchronous Feign call and has a fallback.  
    **Look at:** `AccountProtectionService.java`.

26. **Q: How is transaction history secured?**  
    **Short:** It first obtains the caller's account list and checks requested account numbers.  
    **Detailed:** TransactionController compares requested account to account-service results before querying the repository.  
    **Look at:** `TransactionController.getMyTransactions()` and `getTransactionsByAccount()`.

27. **Q: How are users and roles related?**  
    **Short:** Many-to-many through `user_roles`; roles and permissions are many-to-many through `role_permissions`.  
    **Detailed:** These are auth database join tables managed by domain relationships/migrations.  
    **Look at:** auth domain entities and Flyway V1/V2.

28. **Q: Is customerId a foreign key to customer-service?**  
    **Short:** No confirmed database foreign key.  
    **Detailed:** It is a logical cross-service ID resolved through HTTP.  
    **Look at:** `Account.java`, `CustomerClient.java`.

29. **Q: What does `@Version` do on Account?**  
    **Short:** It supports JPA optimistic locking.  
    **Detailed:** Hibernate can detect conflicting updates, but the direct controller update path and stale target-balance design still need careful verification.  
    **Look at:** `Account.java`, V2 migration.

30. **Q: How does reversal work?**  
    **Short:** It creates an opposite single-account transaction.  
    **Detailed:** It loads a successful transaction, flips DEBIT/CREDIT, and calls `process`; it does not fully model reversal state.  
    **Look at:** `TransactionController.reverse()`.

31. **Q: What is the Config Server doing?**  
    **Short:** Serving centralized Git-backed Spring configuration.  
    **Detailed:** It runs separately on 8888, but local YAML still contains important configuration.  
    **Look at:** config-server source and service application YAML.

32. **Q: Why use separate databases?**  
    **Short:** Service ownership and independent persistence boundaries.  
    **Detailed:** It reduces direct coupling but means relationships and consistency must be handled at the service/API level.  
    **Look at:** `docker-compose.yml`, database configs.

33. **Q: What does `@Transactional` guarantee here?**  
    **Short:** Local database rollback for the current service transaction.  
    **Detailed:** It cannot undo a committed HTTP request in another service/database.  
    **Look at:** `TransactionService.java`, `AccountService.java`.

34. **Q: Why can token validation fail after auth restart?**  
    **Short:** The RSA key pair is generated in memory.  
    **Detailed:** JwkConfig creates a new key on startup, so old tokens are signed by an unknown key afterward.  
    **Look at:** `JwkConfig.java`.

35. **Q: Is KYC fully real?**  
    **Short:** No, it is partial/integration-ready.  
    **Detailed:** Setu methods exist, but KycService has local/mock behavior and the external calls are not confirmed on the main submission path.  
    **Look at:** `KycService.java`, `SetuService.java`.

### Hard

36. **Q: Why is the current transfer not strongly consistent?**  
    **Short:** It performs two independent account writes after stale reads.  
    **Detailed:** Sender debit and receiver credit are separate HTTP calls; `@Transactional` is local, direct assignment bypasses the stronger lock method, and compensation is not atomic.  
    **Look at:** `TransactionService.transfer()`, `AccountController.updateBalance()`.

37. **Q: Can two concurrent transfers lose money or overwrite updates?**  
    **Short:** Yes, the current target-balance approach is vulnerable.  
    **Detailed:** Two requests may read the same balance, calculate targets, and save them in an order that overwrites the other result. The intended lock path is not used by the Feign route.  
    **Look at:** `AccountService.updateBalance()`, `AccountController.updateBalance()`.

38. **Q: How would you fix transfer consistency?**  
    **Short:** Centralize atomic account commands and add a durable saga/outbox/idempotency design.  
    **Detailed:** Use account-service conditional updates/locking, an idempotency key, durable transfer state, an outbox, and reconciliation or a workflow coordinator. A distributed XA transaction would be heavier and less desirable for this architecture.  
    **Look at:** transfer/account classes and Kafka configuration.

39. **Q: What happens if account-service is down during transfer?**  
    **Short:** Account lookup protection can fail fast with a fallback, but the transfer writes are not fully recoverable.  
    **Detailed:** Resilience4j protects one account read; Feign update failures throw. If one update already succeeded, compensation may also fail.  
    **Look at:** `AccountProtectionService`, transfer catch blocks.

40. **Q: Why is the single transaction path safer in one respect?**  
    **Short:** It checks an idempotency reference and uses protected account lookup.  
    **Detailed:** `process()` finds an existing reference and returns it, creates a pending row, handles failure status, and uses resilience annotations. This does not automatically make `/transfer` safe.  
    **Look at:** `TransactionService.process()`.

41. **Q: Does Kafka guarantee that a successful transfer event is consumed?**  
    **Short:** No.  
    **Detailed:** The producer call is not part of a confirmed outbox and there are no consumers found. Publish failure is logged while the transfer can still return success.  
    **Look at:** `TransactionService`, Kafka configuration, repository-wide search for `@KafkaListener`.

42. **Q: Why is the internal balance endpoint dangerous?**  
    **Short:** It is callable through a normal authenticated path and directly assigns a supplied balance.  
    **Detailed:** No explicit internal service authority is visible, and it bypasses AccountService lock/status/amount logic.  
    **Look at:** `AccountController.updateBalance()`, `SecurityConfig.java`.

43. **Q: What is the difference between amount and newBalance in the code?**  
    **Short:** The transaction service computes a target balance, while the intended account service method treats its value as an amount.  
    **Detailed:** `AccountService.updateBalance` subtracts/adds `request.newBalance` as an amount; the controller directly assigns it. This semantic mismatch is why the actual route must be traced precisely.  
    **Look at:** both `BalanceUpdateRequest` classes and both update methods.

44. **Q: How could a duplicate transfer happen even though transaction rows have unique references?**  
    **Short:** Each retry generates new random references.  
    **Detailed:** The database uniqueness protects identical reference values, but `/transfer` does not accept/use a client idempotency key, so retries create different rows and repeat balance changes.  
    **Look at:** `TransactionService.transfer()` and migration `V2__add_reference_id_unique.sql`.

45. **Q: Is reversal authorization complete?**  
    **Short:** No confirmed complete protection.  
    **Detailed:** The endpoint has no visible `@PreAuthorize`, does not clearly check account ownership, and does not mark the original as reversed.  
    **Look at:** `TransactionController.reverse()`.

46. **Q: Why are cross-service foreign keys absent?**  
    **Short:** PostgreSQL databases are owned by different services.  
    **Detailed:** A foreign key cannot span independent databases; services use Feign calls and logical IDs, which improves ownership but shifts consistency to application code.  
    **Look at:** account/customer/transaction entities and clients.

47. **Q: Why can a frontend operation fail even when the backend works?**  
    **Short:** Some DTO field names do not match.  
    **Detailed:** Examples include `type` versus `accountType`, `aadharNumber` versus `aadhaarNumber`, and `txnId` versus UI fallback names.  
    **Look at:** frontend API/pages and backend DTOs.

48. **Q: What happens to old tokens after an auth-service restart?**  
    **Short:** They may fail signature verification.  
    **Detailed:** The in-memory RSA JWK changes on restart; a production system must persist/rotate keys and expose a stable JWK set.  
    **Look at:** `JwkConfig.java`.

49. **Q: What would you test for money transfer?**  
    **Short:** Normal, insufficient, blocked, unauthorized, duplicate, concurrent, and partial-failure cases.  
    **Detailed:** Verify both balances, two transaction rows, audit, event behavior, rollback/compensation, and retry semantics.  
    **Look at:** `scratch/test_full_banking_flow.js`, transaction/account services, and add integration tests.

50. **Q: What is the strongest honest summary of this project?**  
    **Short:** A functional microservice banking prototype with a non-atomic transfer design.  
    **Detailed:** The main happy path is implemented and demonstrable, but production banking requirements such as durable idempotency, atomic multi-account consistency, key management, real KYC, service authorization, and broad tests are incomplete.  
    **Look at:** the complete service structure, especially `TransactionService.java`.

## 14. Demonstration Guide

### Step 1: Start infrastructure and services

- **Do:** `docker compose up -d --no-build` from the root when images exist.
- **Show:** `docker compose ps`; all service containers are `Up`.
- **Internally:** PostgreSQL, Redis, Kafka/Zookeeper, Config, Eureka, and Spring services start.
- **Look at:** `docker-compose.yml`.
- **Expected result:** Ports 8080, 8761, 8888 and service ports are published.

### Step 2: Show service discovery

- **Do:** Open `http://localhost:8761/`.
- **Show:** Registered service instances.
- **Internally:** Eureka clients register after Spring startup.
- **Look at:** service logs and `EurekaServerApplication`.
- **Expected result:** Eureka dashboard loads; registrations may take time.

### Step 3: Start and show frontend

- **Do:** From `frontend/`, run `npm run dev -- --host 0.0.0.0`; open `http://localhost:5173/`.
- **Show:** Login/register UI and navigation.
- **Internally:** Vite serves React; Axios base URL targets gateway.
- **Look at:** `frontend/src/App.jsx`, `axiosClient.js`.
- **Expected result:** UI loads.

### Step 4: Register user A and user B

- **Do:** Use Register UI or `POST /api/auth/register` twice.
- **Show:** Registration response with user ID.
- **Internally:** UserController -> UserService -> BCrypt -> users/role tables.
- **Expected result:** Two auth users; no customers/accounts yet.

### Step 5: Login

- **Do:** Submit username/password.
- **Show:** OTP response in this development configuration.
- **Internally:** LoginController -> BCrypt -> OtpService -> Redis; SMS/email may log.
- **Expected result:** OTP is returned because `expose-dev-otp` is enabled.

### Step 6: Obtain JWT

- **Do:** Submit OTP through the frontend flow or call `/oauth2/token` with the custom OTP grant.
- **Show:** Access token and decoded `userId`/`authorities` claims.
- **Internally:** converter -> provider -> OTP verification -> RSA JWT customizer.
- **Expected result:** JWT stored as `bank_access_token`.

### Step 7: Create customer profiles

- **Do:** Use Customer page for each user.
- **Show:** `POST /api/customers` response.
- **Internally:** CustomerController -> CustomerService -> JWT userId -> customerdb.
- **Expected result:** Customer profile with PENDING status.

### Step 8: Create accounts

- **Do:** Use Accounts page with account type and initial deposit for both users.
- **Show:** account number and starting balance.
- **Internally:** AccountController -> AccountService -> CustomerClient -> sequence -> generator -> accountdb.
- **Expected result:** Two account numbers and balances.

### Step 9: Check balances

- **Do:** Open dashboard/accounts.
- **Show:** `GET /api/accounts/me` response.
- **Internally:** JWT -> customer lookup -> AccountRepository by customerId.
- **Expected result:** Current account balance from `accounts.balance`.

### Step 10: Perform a transfer

- **Do:** As user A, transfer a small amount to user B's account.
- **Show:** request, success response, and both account balances.
- **Internally:** TransactionService validates ownership/balance, makes sender and receiver Feign updates, saves debit/credit rows, audit, Kafka attempt.
- **Expected result:** sender decreases, receiver increases, response contains base `txnId`.

### Step 11: Show history

- **Do:** Open Transactions page or call `GET /api/transactions/me`.
- **Show:** debit for sender and credit for receiver.
- **Internally:** transaction-service asks account-service for caller accounts, then queries transactiondb.
- **Expected result:** rows ordered newest first.

### Step 12: Demonstrate a controlled failure

- **Do:** Try an amount greater than sender balance or use a blocked/missing account in a safe test environment.
- **Show:** rejection response.
- **Internally:** validation in TransactionService or account lookup failure.
- **Expected result:** no normal successful transfer; inspect logs for exact failure.

### Step 13: Explain the consistency caveat

- **Do:** Do not simulate destructive partial failure during a live presentation unless the data is disposable.
- **Say:** The implementation attempts compensation if receiver credit fails, but there is no distributed transaction; if compensation fails, the sender can remain debited.
- **Show:** catch blocks in `TransactionService.transfer()` and direct assignment in `AccountController.updateBalance()`.

## 15. One-Page Revision Sheet

### Architecture

React/Vite -> Spring Cloud Gateway -> Auth, Customer, Account, Transaction services. Eureka discovers services. Config Server serves config. Each main service owns a PostgreSQL database. Redis stores OTP/cache/lock data. Kafka receives account/transaction events.

### Services

- Auth: users, BCrypt, OTP, OAuth2/JWT, roles/permissions.
- Customer: profile, KYC, Setu integration skeleton.
- Account: account numbers, balance, account ownership.
- Transaction: debit/credit, transfer, history, audit, reversal.
- Gateway: routes + JWT edge validation.
- Eureka: discovery.
- Config: centralized configuration.

### Core APIs

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /oauth2/token`
- `POST /api/customers`
- `GET /api/customers/me`
- `POST /api/accounts`
- `GET /api/accounts/me`
- `POST /api/transactions/transfer`
- `GET /api/transactions/me`
- `POST /api/transactions/reverse/{txnId}`

### Authentication

Password -> BCrypt check -> Redis OTP -> custom `grant_type=otp` -> RSA JWT -> `userId` and `authorities` claims -> Axios Bearer token -> resource-server validation -> SecurityUtil user identity.

### Transfer

Validate amount/accounts -> load sender -> verify sender customer matches JWT user -> check balance/status -> load receiver -> calculate two target balances -> direct sender update -> direct receiver update -> compensate sender if receiver update fails -> save debit/credit rows -> audit -> Kafka attempt -> response.

### Critical honesty point

`TransactionService.transfer()` is synchronous but not atomically distributed. Its `@Transactional` annotation covers transactiondb only. Feign reaches `AccountController.updateBalance()` and bypasses `AccountService.updateBalance()` locking/validation. There is no reliable idempotency for `/transfer`.

### Important files

`TransactionService.java`, `TransactionController.java`, `AccountController.java`, `AccountService.java`, `Account.java`, `Transaction.java`, `AccountClient.java`, `CustomerClient.java`, `LoginController.java`, `OtpService.java`, `OtpAuthenticationProvider.java`, `AuthorizationServerConfig.java`, `JwkConfig.java`, `SecurityUtil.java`, `docker-compose.yml`, gateway `application.yml`.

### Ten technical points

1. Separate Spring Boot services and databases.
2. Gateway is the public API entry point.
3. Eureka provides registration/discovery.
4. Auth uses BCrypt plus OTP plus JWT.
5. JWT contains custom `userId` and `authorities` claims.
6. Customer/account links are logical cross-service IDs.
7. Account numbers use a sequence and check digit.
8. Transfer creates debit and credit rows.
9. Transfer is not a distributed transaction.
10. KYC, Kafka consumers, key persistence, idempotency, and failure recovery need production hardening.

## 16. Study Plan

### One hour

- Read `TransactionService.transfer`, `TransactionController`, `AccountController.updateBalance`, and `AccountService.updateBalance`.
- Memorize the login sequence: password -> Redis OTP -> `/oauth2/token` -> JWT.
- Memorize the transfer sequence and the non-atomic consistency caveat.
- Run one registration, login, account creation, balance, transfer, and history demo.
- Review the one-page revision sheet and questions 16, 20, 22, 23, 24, 36, 37, and 42.

### Three hours

- Read the full auth configuration and decode one JWT.
- Read all four entity classes and Flyway migrations.
- Trace every Feign client and gateway rewrite.
- Verify frontend request/response field names in browser network logs.
- Practice explaining the transfer failure cases and proposed fixes.
- Rehearse the 5-10 minute script twice without reading it.

### One day

- Run the entire Docker/frontend demo from a clean start.
- Read every controller and service in auth, customer, account, and transaction modules.
- Inspect all application YAML files and explain every external dependency.
- Run normal, insufficient-balance, unauthorized-account, duplicate-retry, and service-failure tests in disposable data.
- Prepare a diagram of the four databases and logical IDs.
- Be ready to distinguish confirmed behavior from intended-but-unused code.
- Practice all 50 viva questions, with special focus on distributed consistency and security weaknesses.

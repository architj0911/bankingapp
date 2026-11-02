# Digital Banking System Presentation Slides

## Slide 1: Title

**Digital Banking System**

- Microservices-based banking prototype
- React frontend and Spring Boot backend
- JWT/OTP authentication
- Account management, transfers, and transaction history

Speaker note: "This project demonstrates a digital banking workflow separated into independent services for authentication, customers, accounts, and transactions."

## Slide 2: Problem Statement

- Users need a secure digital banking workflow.
- The system should support registration and login.
- Users should create accounts and view balances.
- Users should transfer money and view transaction history.
- Banking responsibilities should be separated into maintainable services.

Speaker note: "The main challenge is not only implementing the screens, but coordinating identity, customer ownership, balances, and transaction records across services."

## Slide 3: Architecture

```text
React/Vite :5173
       |
       v
API Gateway :8080
       |
       +--> Auth Service :9000 ------ authdb
       +--> Customer Service :8082 -- customerdb
       +--> Account Service :8083 ---- accountdb
       +--> Transaction Service :8084 - transactiondb

Eureka :8761 | Config Server :8888
Redis: OTP/cache/locks | Kafka/Zookeeper: events
```

Speaker note: "This is a microservices architecture, not a monolith. Each business service has its own application and database."

## Slide 4: Technologies

- Java 21
- Spring Boot 3.4.10
- Spring Cloud Gateway
- Eureka service discovery
- Spring Cloud Config
- Spring Security OAuth2/JWT
- PostgreSQL and Flyway
- Redis
- Kafka and Zookeeper
- OpenFeign
- Resilience4j
- React 19, Vite, Axios
- Docker Compose

## Slide 5: Services

| Service | Responsibility |
|---|---|
| Auth | Users, BCrypt passwords, OTP, JWT, roles |
| Customer | Profile and KYC |
| Account | Account numbers, balances, ownership |
| Transaction | Transfer, debit/credit, history, audit |
| Gateway | Public entry point, routes, JWT validation |
| Eureka | Service registration/discovery |
| Config | Centralized configuration |

## Slide 6: Registration Flow

```text
Register page
 -> authApi.js
 -> POST /api/auth/register
 -> gateway rewrite: /user/register
 -> UserController.register()
 -> UserService.register()
 -> BCrypt encoding
 -> UserRepository.save()
 -> authdb
```

Important point: registration creates an auth user only. It does not automatically create a customer or bank account.

Files:

- `auth-service/.../UserController.java`
- `auth-service/.../UserService.java`
- `frontend/src/api/authApi.js`

## Slide 7: Login and OTP Flow

```text
POST /api/auth/login
 -> LoginController.login()
 -> UserRepository lookup
 -> BCrypt password check
 -> OtpService.generateOtp()
 -> Redis, 30-minute TTL
 -> SMS/email/local development log
```

Development behavior:

- `app.auth.expose-dev-otp: true`
- OTP is returned in the response and logged.

Files:

- `LoginController.java`
- `OtpService.java`
- `SmsService.java`
- `EmailService.java`

## Slide 8: JWT Flow

```text
POST /oauth2/token with grant_type=otp
 -> OtpAuthenticationConverter
 -> OtpAuthenticationProvider
 -> OtpService.verifyOtp()
 -> AuthorizationServerConfig
 -> RSA-signed JWT
 -> frontend stores bank_access_token
 -> Axios sends Bearer token
```

JWT custom claims:

- `userId`
- `authorities`

Downstream services use `SecurityUtil` to identify the logged-in user.

## Slide 9: Customer and Account Creation

```text
POST /api/customers
 -> CustomerController
 -> CustomerService
 -> JWT userId
 -> customerdb

POST /api/accounts
 -> AccountController
 -> CREATE_ACCOUNT authority
 -> AccountService.accountCreate()
 -> CustomerClient
 -> account_seq
 -> AccountNumberGenerator
 -> AccountRepository.save()
 -> accountdb
```

Account numbers use a branch/type prefix, database sequence, and check digit.

## Slide 10: Balance Lookup

```text
GET /api/accounts/me
 -> AccountController.getMyAccounts()
 -> AccountService.getMyAccounts()
 -> JWT userId
 -> CustomerClient
 -> AccountRepository.findByCustomerId()
 -> accounts.balance
```

The balance is read directly from the `accounts.balance` column. It is not recalculated from transaction history.

## Slide 11: Money Transfer Flow

```text
POST /api/transactions/transfer
 -> TransactionController.transfer()
 -> @PreAuthorize TRANSFER_MONEY
 -> TransactionService.transfer()
 -> validate amount/accounts
 -> load sender account
 -> verify sender ownership
 -> check balance/status
 -> load receiver
 -> calculate new balances
 -> update sender through AccountClient
 -> update receiver through AccountClient
 -> save debit + credit rows
 -> save audit
 -> publish Kafka event
 -> TransferResponse
```

Speaker note: "The normal transfer path is synchronous. Kafka publication is asynchronous, but it is not part of an atomic commit."

## Slide 12: Transfer Security and Consistency

Confirmed protections:

- JWT authentication
- `TRANSFER_MONEY` authority
- Sender account must belong to the authenticated customer
- Positive amount required
- Sender and receiver cannot be the same
- Sender and receiver cannot be blocked
- Insufficient balance is checked before the write

Important limitation:

- Debit and credit are separate service/database calls.
- `@Transactional` only covers transaction-service database work.
- Receiver failure triggers best-effort compensation.
- The Feign call reaches `AccountController.updateBalance()`, which directly assigns the balance and bypasses the stronger `AccountService.updateBalance()` lock path.

## Slide 13: Transaction History

```text
GET /api/transactions/me
 -> TransactionController.getMyTransactions()
 -> AccountClient.getMyAccounts()
 -> verify caller account numbers
 -> TransactionRepository query
 -> transactions ordered by createAt descending
```

A transfer produces:

- One `DEBIT` row for the sender.
- One `CREDIT` row for the receiver.
- One audit record for the transfer.

## Slide 14: Database Design

- Auth database: users, roles, permissions, `user_roles`, `role_permissions`, login audit.
- Customer database: Customer and KycDetails.
- Account database: accounts and `account_seq`.
- Transaction database: transactions and transaction audit.
- Cross-service IDs are logical references, not foreign keys.

Speaker note: "Separate databases preserve service ownership, but the application must enforce relationships through service calls."

## Slide 15: External and Platform Components

- Eureka: services register and discover each other.
- Config Server: Git-backed configuration service.
- Redis: OTP and metadata TTL, caching, lock support.
- Kafka: `account-created-topic` and `transaction-event` producers.
- Zookeeper: Kafka coordination.
- Setu WebClient: PAN/DigiLocker integration methods.

Qualification: no Kafka consumers were found, and the KYC flow is partly local/mock behavior.

## Slide 16: Known Limitations

- No distributed transaction for transfer.
- No reliable idempotency key for `/transactions/transfer`.
- Compensation can fail.
- Stale read/target write can lose concurrent updates.
- Internal balance endpoint lacks explicit service-only authorization.
- OTP is exposed/logged in development mode.
- RSA keys are generated in memory.
- KYC is not confirmed as a complete real verification flow.
- Kafka consumers were not found.
- Reversal does not clearly mark the original as reversed or prevent repeat reversal.
- Frontend/backend field mismatches exist.

## Slide 17: Future Improvements

1. Put all balance mutations behind one protected account-service command.
2. Use atomic conditional updates and consistent locking.
3. Add a client idempotency key for transfers.
4. Add a durable saga/outbox and reconciliation workflow.
5. Persist and rotate JWT signing keys.
6. Move secrets to a secret manager.
7. Remove development OTP exposure and logging.
8. Enforce service-to-service authorities.
9. Complete real KYC integration and ownership checks.
10. Add integration tests for concurrency and partial failure.

## Slide 18: Conclusion

- The project is a functional microservice banking prototype.
- Registration, OTP/JWT login, customer creation, account creation, balance lookup, transfer, and history are implemented.
- The main technical learning is coordinating data across independent services.
- The happy path works, but transfer consistency and production security need hardening.

Closing line:

"The project demonstrates the complete banking workflow while also showing why distributed money movement requires stronger atomicity, idempotency, recovery, and key-management controls than a normal CRUD application."

## Fast Viva Answers

**Why microservices?**

To separate authentication, customer, account, and transaction responsibilities and give each service independent persistence and deployment boundaries.

**Why API Gateway?**

It provides one frontend entry point for routing, JWT validation, CORS/filter behavior, and centralized request logging.

**Why Eureka?**

It provides service registration and discovery instead of requiring every service instance to be hardcoded.

**How does JWT work?**

The auth service verifies password and OTP, creates an RSA-signed JWT containing `userId` and `authorities`, and downstream services validate it as resource servers.

**How is a password stored?**

BCrypt hash through `SecurityBeanConfig.passwordEncoder()` and `UserService`.

**How does transfer happen?**

`TransactionService.transfer()` validates ownership and balance, makes sender and receiver account-service updates, writes debit/credit rows, audits, and publishes an event.

**Is transfer atomic?**

No. It spans separate HTTP calls and databases. Compensation is best effort.

**What happens if credit fails?**

The code tries to restore the sender balance. If compensation fails, the system logs a critical error and may leave an inconsistent state.

**Where should I demonstrate the key risk?**

Compare `TransactionService.transfer()`, `AccountClient.updateBalance()`, `AccountController.updateBalance()`, and `AccountService.updateBalance()`.

# APEXBANK KYC, Identity Verification & Login MFA Setup Guide

## 1. Overview
ApexBank incorporates an enterprise-grade multi-tier identity verification (KYC) and mandatory multi-factor email OTP authentication architecture across its Spring Boot microservices and React frontend.

---

## 2. Architecture & Flow

```
[ React / Vite Frontend ]
           |
           v
    [ API Gateway :8080 ]
           |
     +-----+------------------------------+
     |                                    |
     v                                    v
[ Auth Service :9000 ]           [ Customer Service :8082 ]
 - Email OTP Generation           - PAN Validation Provider
 - Redis Hashed Storage           - Aadhaar Offline e-KYC Adapter
 - Mandatory Login MFA            - Face Liveness Gestures
 - OAuth2 JWT Issuance            - Face Match Verification
 - Structured Audit Logs          - Customer & KYC State Machine
                                          |
                                          v
                                 [ Account Service :8083 ]
                                  - Guard: KYC_VERIFIED check
```

---

## 3. Mandatory Email OTP Authentication

### A. User Registration
1. User enters username, password, email, and metadata.
2. User requests Email OTP (`POST /api/kyc/email/send-otp`).
3. 6-digit cryptographic OTP is generated and SHA-256 hashed into Redis (`kyc:email-otp:{email}`, TTL: 5 minutes).
4. User submits the code (`POST /api/kyc/email/verify-otp`).
5. Only verified emails are allowed to complete account creation (`POST /user/register`).

### B. Login MFA Challenge Flow
1. User enters username and password (`POST /api/auth/login`).
2. Server verifies password hash.
3. On valid credentials, a unique UUID `challengeId` is generated and saved in Redis (`login:otp:{challengeId}`, TTL: 5 minutes).
4. A 6-digit OTP is sent to the user's registered email; the API returns `{"status": "OTP_REQUIRED", "challengeId": "...", "maskedEmail": "a****@gmail.com"}`. **No JWT is issued at this stage.**
5. User enters the OTP code (`POST /api/auth/login/verify-otp`).
6. Upon successful verification, the OAuth2 JWT token is minted and returned.
7. Resend cooldown (30s) and attempt rate limits (5 max attempts) are strictly enforced (`POST /api/auth/login/resend-otp`).

---

## 4. Multi-Stage KYC Verification

| Stage | Endpoint | Description | Demo vs Production Mode |
| :--- | :--- | :--- | :--- |
| **1. Email** | `POST /api/kyc/email/verify-otp` | 6-digit email OTP | Live via SMTP |
| **2. PAN** | `POST /api/kyc/pan/verify` | Format & checksum regex validation | `DEMO_VERIFIED` (Format Valid) / Provider Ready |
| **3. Aadhaar** | `POST /api/kyc/aadhaar/upload` | Offline e-KYC package (.xml/.zip) safe extraction | `DEMO_VERIFIED` (Reference ID generated) |
| **4. Face Liveness** | `POST /api/kyc/face/liveness/*` | Browser camera gesture challenges (`TURN_LEFT`, `BLINK`, `TURN_RIGHT`, `LOOK_UP`) | `DEMO FACE LIVENESS` (Client-side frame capture) |
| **5. Face Match** | `POST /api/kyc/face/match` | Compares live selfie against KYC demographic photo | Feature match simulation (PASS/FAIL) |

When all 5 stages succeed:
- KYC overall status becomes `KYC_VERIFIED`.
- Customer status becomes `ACTIVE`.
- Account service permits savings/current bank account opening.

---

## 5. Environment Variables & SMTP Setup

Copy `.env.example` to your deployment environment or set system environment variables:

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
KYC_PAN_PROVIDER=demo
KYC_AADHAAR_PROVIDER=demo
```

> **Note for Gmail:** Use an **App Password** generated from your Google Account Security settings.

---

## 6. Redis Keys Specification

| Key | TTL | Description |
| :--- | :--- | :--- |
| `login:otp:{challengeId}` | 5 min | SHA-256 hashed Login MFA OTP |
| `login:meta:{challengeId}` | 5 min | Metadata (username, email, IP, device) |
| `login:attempts:{challengeId}` | 5 min | Failed OTP attempt counter (max 5) |
| `login:cooldown:{challengeId}` | 30 sec | Resend cooldown timer |
| `kyc:email-otp:{email}` | 5 min | Registration Email OTP hash |
| `kyc:email-verified:{email}` | 30 min | Verified email session marker |
| `kyc:face:session:{sessionId}` | 5 min | Active face liveness challenge sequence |

---

## 7. Audit Logging Events

All security and identity events are tracked in the database without logging raw passwords, OTPs, or full Aadhaar numbers:
- `EMAIL_OTP_SENT`
- `EMAIL_VERIFIED`
- `PAN_VERIFICATION_ATTEMPT`
- `PAN_VERIFIED`
- `AADHAAR_VERIFICATION_ATTEMPT`
- `AADHAAR_VERIFIED`
- `FACE_LIVENESS_STARTED`
- `FACE_LIVENESS_PASSED`
- `FACE_MATCH_PASSED`
- `KYC_COMPLETED`
- `KYC_FAILED`
- `LOGIN_OTP_SENT`
- `LOGIN_OTP_VERIFIED`
- `LOGIN_SUCCESS`

---

## 8. Running the Application & Tests

### Build & Run Tests (Backend):
```bash
# From workspace root
./mvnw.cmd clean test -f banking-system-parent/pom.xml
```

### Build Frontend:
```bash
cd frontend
npm install
npm run build
```

### Starting the Microservices via Docker Compose:
```bash
docker compose up -d
```
Service startup order:
1. `eureka-server` (port 8761)
2. `config-server` (port 8888)
3. `postgres-auth`, `postgres-customer`, `postgres-account`, `postgres-transaction`, `redis`, `kafka`
4. `auth-service` (port 9000)
5. `customer-service` (port 8082)
6. `account-service` (port 8083)
7. `transaction-service` (port 8084)
8. `api-gateway` (port 8080)
9. `frontend` (port 5173 / web server)

---

## 9. Known Limitations & Production Readiness

1. **PAN Verification:** Demo mode checks regex structure and name normalization. Real-world deployment requires hooking NSDL / Income Tax Department API keys into `ProductionPanVerificationProvider`.
2. **Aadhaar e-KYC:** Offline XML/ZIP package demo mode generates safe reference IDs. Production requires UIDAI XML certificate digital signature validation.
3. **Face Liveness & Matching:** Browser webcam challenges execute locally. Production deployment can integrate certified biometric SDKs.
4. **Data Privacy:** Raw biometric files and plaintext OTPs are strictly excluded from permanent database storage.
# APEXBANK BASELINE TEST REPORT

**Generated Date:** 2026-10-08
**Java Version:** 21.0.12 (Eclipse Adoptium)
**Spring Boot Version:** 3.4.10 / Spring Cloud 2024.0.0
**Build Tool:** Maven 3.9.12 Wrapper (`./mvnw.cmd`)
**Frontend Tool:** Vite 8.2.2 / React 18

---

## 1. Module Compilation & Build Status

| Service / Module | Type | Compilation Status | Test Run Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `banking-system-parent` | Root POM | **SUCCESS** | N/A | Reactor parent |
| `config-server` | Spring Boot / Cloud Config | **SUCCESS** | PASS (0 tests) | Ready |
| `eureka-server` | Spring Boot / Eureka Discovery | **SUCCESS** | PASS (1 test) | Context Loads: PASS |
| `api-gateway` | Spring Cloud Gateway | **SUCCESS** | PASS (0 tests) | Routes configured |
| `auth-service` | Spring Boot / Auth Server / JWT | **SUCCESS** | PASS (0 tests) | OAuth2 + OTP Auth Provider |
| `customer-service` | Spring Boot / Customer & KYC | **SUCCESS** | PASS (0 tests) | Customer, Kyc Details |
| `account-service` | Spring Boot / Account Service | **SUCCESS** | PASS (0 tests) | Account Validation & DB |
| `transaction-service` | Spring Boot / Transaction Service | **SUCCESS** | PASS (0 tests) | Transfers & History |
| `frontend` | React + Vite | **SUCCESS** | Build PASS (349 kB bundle) | UI & Contexts |

---

## 2. Baseline Architecture Audit

### Identified Issues & Areas for Improvement:

1. **Frontend State Initialization in Register.jsx:**
   - In `Register.jsx`, `passwordRules` was computed from `formData.password` before `const [formData, setFormData] = useState(...)` was declared. This can cause runtime `ReferenceError` on certain bundler configurations.
2. **KYC Flow Incomplete Endpoints:**
   - `Face Liveness` and `Face Match` endpoints (`/api/kyc/face/liveness/*`, `/api/kyc/face/match`) and interactive UI components were pending implementation.
   - `kyc_verification` entity needed standard unified field naming (`user_id`, `pan_masked`, `pan_status`, `aadhaar_reference_id`, `aadhaar_status`, `email_status`, `face_status`, `liveness_status`, `overall_status`, `failure_reason`, `verified_at`, `created_at`, `updated_at`).
   - `POST /api/kyc/pan/verify` and `POST /api/kyc/aadhaar/upload` needed provider interfaces (`PanVerificationProvider`, `AadhaarVerificationProvider`) with demo and production-ready structure.
3. **Login OTP Flow Enhancement:**
   - Login challenge endpoint required distinct `challengeId` returned from `POST /api/auth/login` and stored in Redis under `login:otp:{challengeId}` with `POST /api/auth/login/verify-otp` and `POST /api/auth/login/resend-otp` while maintaining OAuth2 token backwards compatibility.
4. **Hardcoded API URLs:**
   - Hardcoded `http://localhost:8080/oauth2/token` in `authApi.js` needed refactoring to use centralized `API_BASE_URL` or `axiosClient`.
5. **Audit Logging Events:**
   - Audit event types needed to cover all lifecycle events: `EMAIL_OTP_SENT`, `EMAIL_VERIFIED`, `PAN_VERIFICATION_ATTEMPT`, `PAN_VERIFIED`, `AADHAAR_VERIFICATION_ATTEMPT`, `AADHAAR_VERIFIED`, `FACE_LIVENESS_STARTED`, `FACE_LIVENESS_PASSED`, `FACE_MATCH_PASSED`, `KYC_COMPLETED`, `KYC_FAILED`, `LOGIN_OTP_SENT`, `LOGIN_OTP_VERIFIED`, `LOGIN_SUCCESS`.

---

## 3. Baseline Verification Summary

- **Backend compilation:** All 8 modules compile with 0 errors.
- **Frontend build:** Production build passes with 0 syntax errors.
- **Core Microservices Architecture:** Preserved in its entirety without introducing unnecessary services or rewriting existing logic.

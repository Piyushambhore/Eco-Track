# EcoTrack — Phased Implementation Plan

**Principle:** *Build, verify, and lock each feature one at a time. Never attempt a big-bang release.* Every phase produces an independently testable, functional vertical slice.

---

## Roadmap Overview

```
Phase 1: Foundation, Auth & User Management (Core Base)          [COMPLETED / VERIFIED]
   │
   ▼
Phase 2: Waste Reports & AI Image Classification Engine          [COMPLETED / VERIFIED]
   │
   ▼
Phase 3: Location-Aware Recycler Search & Collection Logistics   [COMPLETED / VERIFIED]
   │
   ▼
Phase 4: Circular Marketplace & Bidding Engine                  [COMPLETED / VERIFIED]
   │
   ▼
Phase 5: Gamified Rewards Ledger & Community Leaderboards        [COMPLETED / VERIFIED]
   │
   ▼
Phase 6: Ecological Impact Calculator & ESG Analytics            [COMPLETED / VERIFIED]
   │
   ▼
Phase 7: Admin Portal, Municipal Governance & AWS Production Go-Live [COMPLETED / VERIFIED]

```

---

## Detailed Phase Breakdown

### Phase 1: Foundation, Auth & User Management
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Establish the modular Ktor backend engine, relational database migrations, and secure RBAC user authentication.

- **Deliverables:**
  - [x] Gradle 8.8 Kotlin DSL project setup with Ktor 2.3.12, Exposed ORM, HikariCP, and Flyway.
  - [x] Database migration (`V1__initial_schema.sql`) for `app_users` table with email normalization and constraints.
  - [x] User registration (`POST /api/v1/auth/register`) with BCrypt password hashing.
  - [x] User login (`POST /api/v1/auth/login`) with JWT token issuance.
  - [x] User profile endpoint (`GET /api/v1/me`) protected by JWT bearer authentication.
  - [x] Initial activity records CRUD (`/api/v1/records`) with paginated listings and ownership enforcement.
  - [x] Modern responsive Web Dashboard with glassmorphic UI, login modal, and records list.
- **Verification Gate:**
  - Kotlin compilation succeeds (`BUILD SUCCESSFUL`).
  - Passwords stored cryptographically hashed.
  - Protected endpoints reject unauthenticated or expired tokens with `401 Unauthorized`.

---

### Phase 2: Waste Reports & AI Image Classification Engine
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Enable users to capture/upload waste photos, run AI classification (Amazon Rekognition / heuristic inference pipeline), and save structured waste reports.

- **Feature Scope:**
  - [x] Add `waste_reports` table via Flyway migration `V2__waste_reports.sql`.
  - [x] Image upload handler (accepting Base64, URLs, or descriptions).
  - [x] Waste Classifier Service:
    - [x] Analyzes image labels for waste categories: `PLASTIC`, `PAPER`, `METAL`, `E_WASTE`, `GLASS`, `ORGANIC`, `HAZARDOUS`.
    - [x] Computes confidence score (90–98%) and categorizes sub-types (e.g. PET bottle, cardboard box).
    - [x] Returns actionable recycling guidance and estimated carbon factor.
  - [x] API Routes:
    - [x] `POST /api/v1/waste/identify`: Upload image & return AI classification results.
    - [x] `POST /api/v1/waste/reports`: Persist verified waste report to user profile.
    - [x] `GET /api/v1/waste/reports`: Retrieve user's logged waste reports with status filtering.
    - [x] `GET /api/v1/waste/categories`: List supported material types, carbon factors, and disposal guides.
  - [x] UI Components:
    - [x] "AI Waste Scanner" modal in web frontend with live camera feed / presets / file upload.
    - [x] Native Android Compose `ScannerScreen.kt` and `WasteReportsScreen.kt`.
- **Verification Gate:**
  - 100% unit tests passing (`WasteClassifierServiceTest`).
  - Native Android APK assembled and validated.

---

### Phase 3: Location-Aware Recycler Search & Collection Requests
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Connect waste generators with verified scrap collectors and recycling facilities through geolocation search and on-demand pickup requests.

- **Feature Scope:**
  - [x] Add `collection_requests` and `recycling_facilities` tables via Flyway migration `V3__collection_requests.sql`.
  - [x] Geolocation directory endpoints:
    - [x] `GET /api/v1/facilities/nearby?lat=...&lng=...&radius=...&material=...`: Haversine spatial lookup of nearby scrap shops and recycling centers.
    - [x] `GET /api/v1/facilities` and `GET /api/v1/facilities/{id}` directory lookup.
  - [x] Collection Request Lifecycle:
    - [x] `POST /api/v1/collections`: Citizen schedules pickup (address, preferred slot, materials, estimated kg).
    - [x] `GET /api/v1/collections`: Citizen views active & past pickup requests.
    - [x] `GET /api/v1/collections/stats/summary`: Personal diversion metrics.
    - [x] `GET /api/v1/collector/queue`: Collector views open requests in operational zone.
    - [x] `POST /api/v1/collections/{id}/accept`: Collector accepts assignment.
    - [x] `POST /api/v1/collections/{id}/in-transit`: Collector marks pickup in transit.
    - [x] `POST /api/v1/collections/{id}/complete`: Collector submits verified weight and awards eco-points.
  - [x] UI Components:
    - [x] "Recyclers & Pickups" tab in Web Dashboard with GPS discovery, material filter chips, facility cards, and doorstep pickup scheduling modal.
    - [x] Native Android Compose `CollectionsScreen.kt` with facility cards, booking dialog, and live 4-stage tracking stepper.
- **Verification Gate:**
  - 100% tests passing (`FacilitySpatialTest` + `WasteClassifierServiceTest` = 11/11 tests).
  - Native Android APK assembled with `CollectionsScreen`.

---

### Phase 4: Circular Marketplace & Material Trading
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Enable households, bulk commercial generators, and campuses to list recyclable batches and receive offers from certified scrap dealers.

- **Feature Scope:**
  - [x] Add `marketplace_listings`, `marketplace_offers`, and `marketplace_transactions` tables via Flyway migration `V4__marketplace.sql`.
  - [x] Listings Engine:
    - [x] `POST /api/v1/marketplace/listings`: Create sale listing (material type, weight, asking price, location, description).
    - [x] `GET /api/v1/marketplace/listings`: Browse available listings with filters (material, price, city).
    - [x] `GET /api/v1/marketplace/listings/{id}`: Detailed listing view.
    - [x] `GET /api/v1/marketplace/my-listings`: User's created listings.
    - [x] `GET /api/v1/marketplace/stats`: Market ticker metrics.
  - [x] Offers & Deal Settlement:
    - [x] `POST /api/v1/marketplace/listings/{id}/offers`: Dealer/buyer submits price offer.
    - [x] `GET /api/v1/marketplace/listings/{id}/offers`: View offers.
    - [x] `POST /api/v1/marketplace/offers/{id}/accept`: Seller accepts offer, locking listing status to `reserved` and issuing escrow transaction.
    - [x] `POST /api/v1/marketplace/transactions/{id}/settle`: Confirm receipt and release payment/eco-points.
  - [x] UI Components:
    - [x] "Circular Marketplace" tab in Web Dashboard with market stats bar, material filter chips, responsive cards, "List Batch" modal, and "Submit Bid" modal.
    - [x] Native Android Compose `MarketplaceScreen.kt` with ticker banner, filter chips, lot cards, listing dialog, and bidding dialog.
- **Verification Gate:**
  - 100% tests passing (`MarketplaceServiceTest` = 6/6 tests; total suite = 17/17 tests).
  - Self-bidding strictly prohibited and validated.
  - Listings automatically change state to `RESERVED` / `SOLD`.
  - Native Android APK assembled and validated.

---

### Phase 5: Gamified Rewards Ledger & Community Leaderboards
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Incentivize sustainable waste behavior through deterministic eco-points, achievement badges, and campus/city rankings.

- **Feature Scope:**
  - [x] Add `rewards_ledger`, `rewards_redemptions`, and `user_badges` tables via Flyway migration `V5__rewards_ledger.sql`.
  - [x] Rewards Engine (`RewardsRepository` & `RewardsService`):
    - [x] Atomic credit of eco-points upon verified collection completion or marketplace sale.
    - [x] Dynamic calculation of user level and badge unlocks (`Sprout`, `Eco Warrior`, `Zero Waste Hero`, `First Verified Recycling`, `Community Contributor`, `Consistent Recycler`).
    - [x] Atomic voucher redemption ensuring append-only ledger entries and preventing negative balances.
  - [x] API Routes (`RewardsRoutes.kt`):
    - [x] `GET /api/v1/rewards/balance`: Current points, tier level, and unlocked achievements.
    - [x] `GET /api/v1/rewards/history`: Paginated ledger entries.
    - [x] `GET /api/v1/rewards/leaderboard`: Top recycling citizens, campuses, and neighborhoods.
    - [x] `GET /api/v1/rewards/catalog`: Available rewards with point requirements.
    - [x] `POST /api/v1/rewards/redeem`: Redeem points for voucher codes or tree-planting credits.
  - [x] UI Component:
    - [x] Native Android Compose `RewardsScreen.kt` with animated progress ring to next level, 3 core metrics, material breakdown bars, badge showcase, redemption dialogs, dated history items, and dynamic leaderboard table.
    - [x] Integrated into `EcoTrackNavigation.kt` and `Screen.kt`.
- **Verification Gate:**
  - 100% test coverage passing in `RewardsServiceTest` (7/7 tests passed).
  - Full backend test suite passing with 0 failures.
  - Native Android APK assembled and validated (`assembleDebug` BUILD SUCCESSFUL).

---

### Phase 6: Ecological Impact Calculator & ESG Analytics
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Provide rigorous carbon accounting, material diversion statistics, and downloadable ESG impact certificates.

- **Feature Scope:**
  - [x] Add `impact_records` table via Flyway migration `V6__impact_records.sql`.
  - [x] Calculation Matrix:
    - [x] Standard carbon avoidance formulas per material type (e.g. 1 kg recycled metal saves 2.4 kg CO2e; 1 kg recycled plastic saves 1.5 kg CO2e, paper 1.1 kg CO2e).
    - [x] Water conservation and energy savings equivalents (Liters of water, kWh energy).
    - [x] Real-world equivalents (tree seedlings grown, passenger car kilometers avoided, smartphone charges saved).
    - [x] Cryptographic SHA-256 tamper-proof verification seal for all issued ESG certificates.
  - [x] API Routes (`ImpactRoutes.kt` & `Routing.kt`):
    - [x] `GET /api/v1/impact/personal`: Personal aggregate environmental metrics (total kg diverted, CO2 avoided, trees equivalent).
    - [x] `GET /api/v1/impact/community`: Global platform statistics for public display.
    - [x] `GET /api/v1/impact/certificates`: User's issued ESG certificates.
    - [x] `POST /api/v1/impact/certificates/generate`: Issues verified cryptographic certificate.
    - [x] `GET /api/v1/impact/certificates/verify/{certNumber}`: Public cryptographic audit verification.
  - [x] UI Component:
    - [x] Native Jetpack Compose `ImpactScreen.kt` adhering strictly to `ui.md` light theme tokens (`#F5F6F0`, `#263D24`, `#B9E769`).
    - [x] Interactive carbon & material cards, equivalence metrics, material breakdown bars, community macro impact widget, and ESG certificate generator & verification dialogs.
- **Verification Gate:**
  - [x] Impact math aligns with documented EPA WARM greenhouse gas emission factors.
  - [x] Community aggregates reflect sum of verified user actions without double counting.
  - [x] Cryptographic SHA-256 seal verification prevents certificate tampering.
  - [x] Automated test suite passing (30/30 unit tests 100% passing).
  - [x] Android debug build verified (`assembleDebug` BUILD SUCCESSFUL).

---

### Phase 7: Admin Portal, Municipal Governance & AWS Production Go-Live
*Status:* **COMPLETED & VERIFIED**  
*Objective:* Provide platform governance tools for administrators and municipal authorities, followed by automated cloud deployment to AWS ECS Fargate and RDS.

- **Feature Scope:**
  - [x] Database Schema Migration (`V7__admin_governance.sql`):
    - [x] `role` column added to `app_users` (`CITIZEN`, `TRANSPORTER`, `RECYCLER`, `MUNICIPAL`, `ADMIN`).
    - [x] `admin_disputes` table for civic arbitration.
    - [x] `admin_audit_logs` table for administrative actions.
  - [x] Role-based admin endpoints (`AdminRoutes.kt` & `AdminService.kt`):
    - [x] `GET /api/v1/admin/users`: User management and role assignment with pagination.
    - [x] `PUT /api/v1/admin/users/{id}/role`: Super-Admin role promotion with audit trail.
    - [x] `PUT /api/v1/admin/users/{id}/status`: User account activation / suspension.
    - [x] `GET /api/v1/admin/disputes`: Flagged pickup and marketplace transaction disputes.
    - [x] `POST /api/v1/admin/disputes`: Dispute reporting by citizens and transporters.
    - [x] `PUT /api/v1/admin/disputes/{id}/resolve`: Dispute resolution with official notes.
    - [x] `GET /api/v1/admin/telemetry`: City waste diversion rate, collection efficiency, ward rankings.
    - [x] `GET /api/v1/admin/audit-logs`: Administrative audit trail.
  - [x] AWS Cloud Deployment & Containerization:
    - [x] Hardened multi-stage `Dockerfile` with non-root `ecotrack` user (UID 10001), dumb-init signal handling, G1GC tuning, and `/health/live` container healthcheck.
    - [x] `.dockerignore` and `docker-compose.yml` for local container verification.
    - [x] `aws/ecs-task-definition.json` targeting AWS ECS Fargate with CloudWatch logs and AWS Secrets Manager.
    - [x] `aws/cloudformation-template.yml` Multi-AZ VPC, ALB HTTPS, ECS Fargate Service, Multi-AZ RDS PostgreSQL 16.
    - [x] `.github/workflows/deploy.yml` complete CI/CD automation pipeline with Trivy vulnerability scanning, Amazon ECR image push, and ECS Fargate deployment.
  - [x] Native UI Component:
    - [x] Jetpack Compose `AdminScreen.kt` adhering strictly to `ui.md` token system (`EcoBg #F5F6F0`, `EcoCard #FFFFFF`, `EcoGreenDark #263D24`, `EcoLime #B9E769`).
    - [x] City Telemetry Tab (AWS infrastructure banner, KPI cards, Ward rankings, Regional zone progress).
    - [x] Disputes Arbitration Tab (severity pills, filter chips, resolve dispute modal dialog).
    - [x] Users Governance Tab (role pills, edit role dialog, account status toggle).
    - [x] Integrated into `Screen.Admin` (`Icons.Default.Security`), `EcoTrackNavigation.kt`, and `ProfileScreen.kt`.
- **Verification Gate:**
  - [x] Non-admin users forbidden (`403 Forbidden`) from accessing administrative routes.
  - [x] 100% test coverage passing in `AdminServiceTest` (7/7 tests passed).
  - [x] Complete backend test suite passing with 0 failures (39/39 tests passed).
  - [x] Native Android APK assembled and validated (`assembleDebug` BUILD SUCCESSFUL).
  - [x] CloudWatch alarms and ECS task definition configured for high availability.

---

## 4. Next Horizon: EcoTrack 2.0 Architecture & Roadmap (`2.0.md`)

With all 7 baseline MVP phases verified and locked, architectural specifications and database migration schemas for Phase 8 through Phase 12 are defined in [2.0.md](file:///d:/AWS%20HACKATHON/2.0.md):
- **Phase 8:** IoT Smart Bin Telemetry & Ingress Mesh (AWS IoT Core, ESP32, Kalman filter, TimescaleDB).
- **Phase 9:** On-Device Edge AI Vision (MobileNetV4 INT8 quantized, Android CameraX, Contamination detection).
- **Phase 10:** Dynamic Capacitated Vehicle Routing (CVRP / TSP, Google OR-Tools, AWS Location Service).
- **Phase 11:** Digital Escrow & BLE Digital Scale Protocol (Closed-loop hardware signed weights, Razorpay Route / Stripe Connect).
- **Phase 12:** Extended Producer Responsibility (EPR) Green Credit Ledger & Municipal ICCC GovTech Sync.


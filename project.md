# EcoTrack — Project Requirements Document (PRD)

**Product Name:** EcoTrack  
**Tagline:** Turn Waste Into Worth.  
**Motto:** Less Waste Cleaner Cities | More Recycling Healthier Planet | Together for a Sustainable Future  
**Domain:** AI-Powered Waste Management, Collection Logistics, Material Recycling & Circular Economy  
**Version:** 1.0 (Production Roadmap)

---

## 1. Executive Summary & Vision

Rapid urbanization and consumption patterns have created an escalating solid waste crisis. Most recyclable materials end up in landfills or informal dumping sites due to lack of source segregation, limited transparency in collection networks, and absence of tangible citizen incentives.

**EcoTrack** is an end-to-end circular economy platform that bridges the gap between waste generators (citizens, campuses, businesses) and waste processors (scrap dealers, formal collectors, certified recyclers, municipal corporations). Powered by computer vision AI for instant waste classification and real-time logistics tracking, EcoTrack transforms everyday waste into measurable economic and ecological value.

---

## 2. Targeted Users & Persona Specifications

EcoTrack explicitly serves **7 distinct user roles and actors**:

| # | User Role | Core Needs & Goals | Key Platform Capabilities |
|---|-----------|-------------------|---------------------------|
| **1** | **Citizens & Households** | Easily identify recyclable items, schedule doorstep waste pickup, find nearest scrap buyers, earn redeemable rewards. | AI photo waste identification, on-demand pickup requests, drop-off directory, eco-point redemption, carbon impact counter. |
| **2** | **Students & Campuses** | Organize clean-up drives, audit institutional waste, compete in sustainability challenges, track campus impact. | Campus leaderboard, group reporting, volunteer event registration, environmental badge milestones. |
| **3** | **Scrap Dealers (Kabadiwalas / Aggregators)** | Discover steady supply of segregated recyclable material, bid on listings, streamline daily collection logistics. | Marketplace material catalog, transparent price discovery, buyer offers, location-based material alerts. |
| **4** | **Waste Collectors & Logistics Drivers** | Receive optimized collection routes, verify pickup loads, confirm handover with digital proof of service. | Driver task queue, GPS navigation, live status updater, QR/photo proof upload, weight confirmation. |
| **5** | **Certified Recyclers & Industrial Processors** | Procure bulk sorted feedstock (plastics, metals, e-waste), maintain compliance, issue chain-of-custody certificates. | Bulk transaction contracts, provenance tracking, digital recycling certificate generation, ESG auditing data. |
| **6** | **Municipal Authorities (ULBs / City Councils)** | Monitor neighborhood waste generation, track informal and formal collection rates, enforce waste segregation rules. | City-wide heatmaps, collector fleet coverage metrics, illegal dumping incident queue, regulatory compliance feeds. |
| **7** | **Platform Admins & Moderators** | Verify vendor and recycler credentials, resolve pickup disputes, manage reward tiers, monitor platform health. | User KYC verification, dispute resolution center, fraud detection, analytics telemetry, reward ledger audits. |

---

## 3. Core Functional Requirements

### 3.1 Module 1: AI Waste Identification & Classification
- **Camera / Image Capture:** Native camera input or photo upload (JPEG, PNG, WebP) with client-side image compression.
- **Automated Material Recognition:** Deep learning image analysis (Amazon Rekognition custom labels / Vision API) detecting:
  - *Plastic* (PET, HDPE, LDPE, PP, Other)
  - *Paper & Cardboard*
  - *Glass*
  - *Metals* (Aluminum, Tin, Copper, Iron)
  - *E-Waste* (Cables, PCBs, batteries, obsolete electronics)
  - *Organic / Compostable*
  - *Hazardous / Sanitary*
- **Classification Output:**
  - Detected category and sub-type
  - Confidence percentage (e.g., `PET Plastic — 94%`)
  - Disposal instructions (e.g., "Rinse, flatten, keep cap attached")
  - Estimated recyclability score & carbon savings factor (kg CO2e avoided)
- **Fallback Mechanism:** Manual classification dropdown if image quality or confidence is below 70%.

### 3.2 Module 2: Location-Aware Search & Recycling Hubs Directory
- **Interactive Map:** Geolocation lookup displaying verified waste collectors, scrap shops, and municipal drop-off centers within customizable radius (1 km – 25 km).
- **Filtering System:** Filter facilities by accepted waste categories, operating hours, buyback rates, and user ratings.
- **Facility Profile:** Operating address, contact information, verified green badge, user reviews, and accepted materials.

### 3.3 Module 3: Waste Collection Requests & Lifecycle Tracking
- **Request Creation:** Citizen selects identified waste report or specifies category, estimated weight/quantity, photo proof, pickup address, and preferred time slot.
- **Smart Dispatch:** Matches available certified collectors or scrap dealers within range based on proximity and material interest.
- **Lifecycle Statuses:**
  `SUBMITTED` &rarr; `MATCHED` &rarr; `ACCEPTED` &rarr; `COLLECTOR_EN_ROUTE` &rarr; `ARRIVED` &rarr; `VERIFIED_WEIGHED` &rarr; `COMPLETED` (or `CANCELLED` / `DISPUTED`).
- **Proof of Collection:** Dual verification via driver upload of scale weight photo or mutual QR code handoff.

### 3.4 Module 4: Circular Marketplace (Trading & Pricing)
- **Material Listings:** Households, bulk generators, or campus offices list segregated waste batches with quantity, asking price (or free for pickup), and location.
- **Bidding & Offers:** Verified scrap dealers and recyclers place offers on listings.
- **Transaction Settlement:** Status tracking from deal negotiation to fulfillment, recording final agreed price, net weight, and transaction receipt.

### 3.5 Module 5: Rewards Ledger & Gamification Engine
- **Eco-Points Allocation:** Deterministic credit of points based on verified activity:
  - 1 kg Plastic = 15 EcoPoints
  - 1 kg Paper/Cardboard = 8 EcoPoints
  - 1 kg Metal/Aluminum = 25 EcoPoints
  - 1 kg E-Waste = 50 EcoPoints
  - Attending campus cleanup / verified drop-off = 100 EcoPoints
- **Tiers & Badges:** `Seedling` &rarr; `Sprout` &rarr; `Eco Warrior` &rarr; `Planet Guardian` &rarr; `Zero Waste Champion`.
- **Leaderboards:** Weekly and all-time rankings across individuals, campuses, and neighborhoods.
- **Reward Marketplace:** Redemption for partner discount vouchers, municipal tax rebates, or tree plantation sponsorships.

### 3.6 Module 6: Ecological Impact Tracking & Carbon Ledger
- **Carbon Equations:** Automatic conversion of recovered material weights into estimated avoided greenhouse gases:
  - $\text{CO}_2\text{e avoided} = \text{Weight (kg)} \times \text{Emission Factor}$
- **Equivalency Metrics:** Display tangible real-world equivalents:
  - Trees planted equivalent
  - Car kilometers avoided
  - Liters of clean water conserved
  - Kilowatt-hours of energy saved
- **Impact Certificates:** Downloadable PDF summaries for university ESG audits or personal sustainability portfolios.

### 3.7 Module 7: Admin & Governance Portal
- **User & Provider KYC:** Review licensing documents of scrap dealers and municipal contractors.
- **Dispute Resolution:** Arbitrate discrepancy between reported weight vs verified weight.
- **System Telemetry:** Live operations dashboard showing pickup completion rates, active hot-spots, and tonnage diverted from landfills.

---

## 4. Non-Functional Requirements (NFRs)

- **Security & Privacy:**
  - Token-based stateless authentication (JWT with HMAC-SHA256 / RS256).
  - Passwords hashed using BCrypt (cost factor 12) or Argon2id.
  - Zero storage of precise GPS coordinates beyond pickup lifecycle; approximate center coordinates stored for search.
  - Role-Based Access Control (RBAC) enforced on every API route (`CITIZEN`, `COLLECTOR`, `DEALER`, `MUNICIPAL`, `ADMIN`).
- **Performance & Scalability:**
  - API response latency $P_{95} < 120\text{ms}$ for non-AI endpoints.
  - Asynchronous background execution for image classification and notification dispatch.
  - Connection pooling with HikariCP (max 10–20 connections per container).
- **Data Integrity & Consistency:**
  - Strict relational foreign keys, non-null constraints, and ACID transactions for financial and points ledgers.
  - Database schema migrations automated via Flyway.
- **Observability:**
  - Structured JSON access and error logs with correlation `requestId`.
  - Health probe endpoints `/health/live` and `/health/ready` for container orchestrator integration.

---

## 5. Acceptance Criteria Checklist

- [x] Backend runs on JVM 17+ with high-performance non-blocking Ktor framework.
- [x] PostgreSQL relational schema model supports all core domain entities.
- [x] Full user authentication flow with secure password hashing and JWT issuance.
- [ ] AI waste identification integration with image payload handling.
- [ ] Collection request scheduling and provider status advancement.
- [ ] Circular marketplace listings and transaction flow.
- [ ] Dynamic points ledger computation and leaderboard rankings.
- [ ] Carbon impact calculator and aggregate metrics generation.

# EcoTrack — System Architecture & Technical Specifications

**Version:** 1.0 (Production Blueprint)  
**Target Cloud:** Amazon Web Services (AWS) — Mumbai Region (`ap-south-1`)  
**Core Framework:** Kotlin / Ktor Modular Monolith  
**Primary Database:** Amazon RDS for PostgreSQL (Multi-AZ)  

---

## 1. High-Level Architecture Diagram

```mermaid
flowchart TB
    subgraph Clients["User Devices & Channels"]
        Web["Web Dashboard (Responsive Desktop / PWA)"]
        Mobile["Mobile App (iOS / Android)"]
        Field["Field Devices (QR / Barcode Scanner, GPS)"]
    end

    subgraph Edge["Edge & Ingress Security"]
        R53["Route 53 (DNS)"]
        CF["CloudFront (CDN Static Assets)"]
        WAF["AWS WAF (Web Application Firewall)"]
        ALB["Application Load Balancer (ALB / Ingress HTTPS)"]
    end

    subgraph VPC["AWS VPC — ap-south-1 (10.0.0.0/16)"]
        subgraph PublicSubnet["Public Subnets (AZ-A & AZ-B)"]
            NAT["NAT Gateway (Outbound Internet)"]
            Bastion["Bastion Host (Secure Maintenance)"]
        end

        subgraph AppSubnet["Private Application Subnets (AZ-A & AZ-B)"]
            ECS["ECS Fargate: EcoTrack Ktor Monolith Containers"]
            AutoScaler["Application Auto Scaling (CPU / Memory / Target Tracking)"]
        end

        subgraph DataSubnet["Private Data Subnets (AZ-A & AZ-B)"]
            RDSPri[("Amazon RDS PostgreSQL 16 (Primary)")]
            RDSSec[("Amazon RDS PostgreSQL 16 (Standby Replica)")]
            ElastiCache[("Amazon ElastiCache Redis (Cache / Throttling)")]
        end
    end

    subgraph AIML["AI & Asset Layer"]
        S3[("Amazon S3 (Waste Photos & Certificates)")]
        Rekognition["Amazon Rekognition (Computer Vision Classification)"]
    end

    subgraph ManagedAWS["Observability & Messaging"]
        CloudWatch["Amazon CloudWatch (Logs & Metrics)"]
        SecretsMgr["AWS Secrets Manager (Environment Secrets)"]
        SNS["Amazon SNS (SMS & Alerts)"]
        SQS["Amazon SQS (Async Event Queue)"]
        EventBridge["Amazon EventBridge (Scheduled Tasks)"]
    end

    subgraph External["External Services"]
        GoogleMaps["Google Maps API (Geocoding & Matrix)"]
        EmailGateway["Amazon SES / SendGrid (Transactional Email)"]
        PaymentGateway["Razorpay / Stripe (Escrow Payments)"]
        MunicipalAPIs["Municipal ULB Waste Portals"]
    end

    %% Client Connections
    Web & Mobile & Field --> R53
    R53 --> CF
    CF --> WAF
    WAF --> ALB
    ALB --> ECS

    %% Application Layer Interactions
    ECS --> S3
    ECS --> Rekognition
    ECS --> RDSPri
    RDSPri -. Synchronous Replication .-> RDSSec
    ECS --> ElastiCache
    ECS --> SecretsMgr
    ECS --> CloudWatch
    ECS --> SQS
    SQS --> SNS
    EventBridge --> ECS

    %% Outbound
    ECS --> NAT
    NAT --> GoogleMaps & EmailGateway & PaymentGateway & MunicipalAPIs
```

---

## 2. AWS Cloud Infrastructure Specification

### 2.1 Network Topology (VPC Design)
- **VPC CIDR:** `10.0.0.0/16` deployed across 2 Availability Zones (`ap-south-1a`, `ap-south-1b`).
- **Subnet Segmentation:**
  - `Public Subnet` (`10.0.1.0/24`, `10.0.2.0/24`): Houses internet-facing ALB and NAT Gateways.
  - `Private App Subnet` (`10.0.10.0/24`, `10.0.20.0/24`): Dedicated exclusively to ECS Fargate task instances. Direct inbound traffic from internet is prohibited. Only ingress permitted from ALB security group on TCP `8080`.
  - `Private Data Subnet` (`10.0.50.0/24`, `10.0.60.0/24`): Houses RDS PostgreSQL and ElastiCache. Only ingress permitted from ECS security group on TCP `5432` and `6379`.
- **Egress:** NAT Gateway handles outbound traffic from ECS tasks for external APIs (Google Maps, Twilio, SES).

### 2.2 Compute & Container Orchestration
- **Runtime:** AWS ECS Fargate with containerized Kotlin application on JDK 17 LTS (Alpine / Eclipse Temurin base).
- **Task Scaling Policy:** Target Tracking scaling triggered when average CPU utilization exceeds 70% or active request count per target exceeds 500.
- **Health Checks:**
  - ALB Target Group: HTTP `GET /health/ready` (interval: 15s, timeout: 5s, healthy threshold: 2, unhealthy threshold: 3).
  - ECS Container Healthcheck: `CMD-SHELL curl -f http://localhost:8080/health/live || exit 1`.

### 2.3 Storage & Database Tier
- **Relational DB:** Amazon RDS for PostgreSQL 16 (Multi-AZ enabled for high availability and automatic failover).
- **Object Storage:** Private Amazon S3 bucket (`ecotrack-assets-prod`) encrypted with AWS KMS. Enforces strict presigned URL issuance for both uploads and temporary photo previews.

---

## 3. Database Schema (Key 7 Domain Tables)

The production schema is built around 7 core tables handling the circular economy lifecycle:

```mermaid
erDiagram
    USERS ||--o{ WASTE_REPORTS : creates
    USERS ||--o{ COLLECTION_REQUESTS : requests
    USERS ||--o{ MARKETPLACE_LISTINGS : lists
    USERS ||--o{ REWARDS_LEDGER : earns
    USERS ||--o{ TRANSACTIONS : participates
    COLLECTION_REQUESTS ||--o{ IMPACT_RECORDS : generates
    MARKETPLACE_LISTINGS ||--o{ TRANSACTIONS : settled_in

    USERS {
        uuid id PK
        string email UK
        string password_hash
        string display_name
        string phone
        string role "CITIZEN, COLLECTOR, DEALER, MUNICIPAL, ADMIN"
        string avatar_url
        string status "active, disabled, pending"
        timestamptz created_at
        timestamptz updated_at
    }

    WASTE_REPORTS {
        uuid id PK
        uuid user_id FK
        string category "plastic, paper, metal, e_waste, glass, organic, hazardous"
        string sub_type
        string photo_url
        decimal latitude
        decimal longitude
        decimal estimated_quantity
        string unit "kg, g, liters, count"
        decimal ai_confidence
        string status "identified, scheduled, collected, rejected"
        timestamptz created_at
    }

    COLLECTION_REQUESTS {
        uuid id PK
        uuid user_id FK
        uuid collector_id FK
        uuid report_id FK
        string waste_type
        decimal quantity
        string unit
        string status "SUBMITTED, MATCHED, ACCEPTED, EN_ROUTE, COMPLETED, CANCELLED"
        string pickup_address
        decimal pickup_lat
        decimal pickup_lng
        timestamptz scheduled_at
        timestamptz completed_at
        string verification_proof_url
        timestamptz created_at
    }

    MARKETPLACE_LISTINGS {
        uuid id PK
        uuid seller_user_id FK
        string material_type
        decimal quantity
        string unit
        decimal unit_price
        string currency
        string status "ACTIVE, RESERVED, SOLD, CANCELLED"
        string location_name
        timestamptz created_at
    }

    TRANSACTIONS {
        uuid id PK
        uuid listing_id FK
        uuid buyer_user_id FK
        uuid seller_user_id FK
        decimal final_quantity
        decimal total_price
        string payment_status "PENDING, ESCROWED, RELEASED, REFUNDED"
        string status "INITIATED, SETTLED, DISPUTED"
        timestamptz created_at
    }

    REWARDS_LEDGER {
        uuid id PK
        uuid user_id FK
        string activity_type "WASTE_RECYCLED, PICKUP_COMPLETED, CAMPUS_DRIVE, REFERRAL"
        integer points
        string description
        uuid reference_id
        timestamptz created_at
    }

    IMPACT_RECORDS {
        uuid id PK
        uuid user_id FK
        uuid collection_request_id FK
        string material_type
        decimal material_weight_kg
        decimal co2_saved_kg
        decimal water_saved_liters
        decimal energy_saved_kwh
        string calculation_method
        timestamptz created_at
    }
```

---

## 4. End-to-End User Flow & Sequence Diagram

### 4.1 Collection Request & Verification Flow

```mermaid
sequenceDiagram
    autonumber
    actor Citizen as Citizen / User
    participant App as Web / Mobile UI
    participant Gateway as Ktor API Gateway
    participant CollectionSvc as Collection Service
    participant Rekognition as AWS Rekognition
    participant DB as RDS PostgreSQL
    actor Collector as Waste Collector

    Citizen->>App: 1. Snaps photo of waste & enters address
    App->>Gateway: 2. Upload photo & request AI Classification
    Gateway->>Rekognition: 3. Detect labels & confidence
    Rekognition-->>Gateway: 4. Returns: "Plastic (PET) - 93%"
    Gateway-->>App: 5. Displays classified category & CO2 factor
    
    Citizen->>App: 6. Confirms & clicks "Schedule Pickup"
    App->>Gateway: 7. POST /api/v1/collections (payload + location)
    Gateway->>DB: 8. Insert collection_request (status: SUBMITTED)
    Gateway->>CollectionSvc: 9. Trigger dispatch matcher
    
    CollectionSvc->>Collector: 10. Push notification of nearby pickup
    Collector->>App: 11. Collector accepts pickup
    App->>Gateway: 12. POST /api/v1/collections/{id}/accept
    Gateway->>DB: 13. Update status: ACCEPTED, collector_id assigned
    Gateway-->>Citizen: 14. Real-time alert: "Collector Assigned"

    Collector->>Citizen: 15. Arrives on site & inspects material
    Collector->>App: 16. Enters scale weight & snaps verification photo
    App->>Gateway: 17. POST /api/v1/collections/{id}/complete (weight, proof)
    Gateway->>DB: 18. Update status: COMPLETED
    Gateway->>DB: 19. Insert REWARDS_LEDGER (points credited to Citizen)
    Gateway->>DB: 20. Insert IMPACT_RECORDS (CO2 saved computed)
    Gateway-->>Citizen: 21. Toast alert: "Pickup verified! +150 EcoPoints earned"
```

---

## 5. Technology Stack Summary

| Layer | Selected Technology | Technical Justification |
|---|---|---|
| **Programming Language** | Kotlin 1.9+ / JDK 17 LTS | Type-safety, null-safety, coroutine concurrency, low memory footprint compared to Spring. |
| **Server Framework** | Ktor Server 2.3.12 | Lightweight, non-blocking asynchronous architecture, modular plugin ecosystem. |
| **HTTP Engine** | Netty Engine | Industry-proven high throughput, low latency non-blocking I/O. |
| **Database ORM/Query** | JetBrains Exposed 0.52.0 | Idiomatic Kotlin SQL DSL, type-safe query construction, fine-grained transaction control. |
| **Connection Pooling** | HikariCP 5.1.0 | Fast, lightweight connection pool with proactive connection validation. |
| **Migrations** | Flyway 10.15.0 | Deterministic, version-controlled SQL migrations executed on startup. |
| **Security / Auth** | Auth0 JWT + BCrypt | Stateless, signed claims with HMAC-SHA256, cryptographically salted user credentials. |
| **Cloud Host** | AWS Mumbai (`ap-south-1`) | Minimal latency for target Indian regions, compliant with national data localization norms. |
| **Containerization** | Docker + ECS Fargate | Serverless container execution, zero EC2 OS patching overhead, instant vertical/horizontal scaling. |
| **Client UI (Compose Multiplatform / Android)** | Kotlin + Compose Multiplatform / Jetpack Compose with Material 3 (`androidx.compose.material3`) | 100% unified Kotlin ecosystem sharing DTOs and business logic with Ktor backend; Material 3 dynamic color theming, declarative components, and cross-platform capability across Android, Desktop, and Web. |

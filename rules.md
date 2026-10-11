# EcoTrack — Engineering Rules & Architecture Guardrails

**Purpose:** Enforce technical discipline, prevent anti-patterns, maintain strict security, and ensure reproducible code quality across the EcoTrack engineering lifecycle.

---

## 1. What to Build vs. What to Avoid

### 1.1 Architecture & Modularity

|  WHAT TO BUILD |  WHAT TO AVOID |
|---|---|
| **Modular Monolith**: Clean separation between `auth`, `records`, `collections`, `marketplace`, `rewards`, and `impact` packages within a single deployable Ktor binary. | **Premature Microservices**: Do NOT split into separate network-separated microservices, Kafka message brokers, or Kubernetes (EKS) clusters before user traffic demands it. |
| **Thin HTTP Route Handlers**: Route handlers only unpack request JSON, validate input types, invoke domain service, and return DTO response. | **Fat Controllers / Routes**: Never write database queries or multi-step business logic directly inside Ktor `routing {}` blocks. |
| **Domain Services for Business Logic**: Services enforce state machines (e.g. collection status transitions), permissions, and calculations. | **Anemic Models / Leaky Entities**: Never return raw database rows or Exposed Table objects directly to the client API response. |
| **Database Migrations (Flyway)**: Every schema addition, index, or constraint must exist in a numbered SQL file (`V1__...`, `V2__...`). | **Manual Schema Editing**: Never run manual `CREATE TABLE` or `ALTER TABLE` commands directly in production or staging. |

---

### 1.2 Security & Authentication Guardrails

|  WHAT TO BUILD |  WHAT TO AVOID |
|---|---|
| **Stateless JWT with Expiration**: Access tokens with short TTL (e.g. 1 hour) containing subject (`userId`) and assigned `role`. | **Client-Provided User IDs**: Never trust `userId` passed in request bodies or query params. Always extract the authenticated identity from the JWT principal. |
| **Server-Side Authorization on Every Route**: Validate that caller owns the resource or has `ADMIN` / `COLLECTOR` role before any mutation. | **Insecure Direct Object References (IDOR)**: Never fetch a record by ID alone (`WHERE id = ?`) without also enforcing ownership (`WHERE id = ? AND owner_id = ?`). |
| **Strong Password Hashing**: BCrypt with work factor $\ge 12$ or Argon2id with unique cryptographic salt per user. | **Plaintext or Weak Hashes**: Never use MD5, SHA-1, SHA-256 without salt, or reversible encryption for user passwords. |
| **AWS Secrets Manager & Environment Variables**: Inject database URLs, JWT secret keys, and AWS credentials via runtime environment variables. | **Hardcoded Credentials**: Never commit API keys, database passwords, or JWT secrets to Git repositories, Dockerfiles, or frontend scripts. |
| **CORS & Rate Limiting**: Restrict CORS origins to known frontend domains; enforce rate limits on login (`/api/v1/auth/login`) and register. | **Permissive Wildcards**: Never use `allowHost("*")` or wildcard origins with credentialed requests in production. |

---

### 1.3 Data Access & Transaction Rules

|  WHAT TO BUILD |  WHAT TO AVOID |
|---|---|
| **Parameterized Queries**: Always use Exposed DSL or JDBC `PreparedStatement` with bound variables. | **Raw SQL String Concatenation**: Never build queries with string interpolation (`"SELECT * WHERE email = '$email'"`). |
| **Short, Atomic Transactions**: Enclose multi-step writes (e.g. verifying pickup + crediting points + logging impact) in a single `transaction {}`. | **Long-Running Transactions**: Never make network HTTP calls (to Rekognition, Google Maps, or Stripe) while holding an active database transaction open. |
| **UUIDs as Primary Keys**: Generate random UUIDv4 identifiers for all public-facing resources. | **Sequential Integer Auto-Increment IDs**: Do NOT expose incremental IDs (e.g., `/records/142`) that allow attackers to enumerate user volume and records. |
| **Explicit Timestamps in UTC**: Store all temporal values as `TIMESTAMPTZ` and manipulate exclusively via `java.time.OffsetDateTime` or `Instant`. | **Floating Local Times**: Never use `TIMESTAMP WITHOUT TIME ZONE` or server-local system clocks. |
| **Capped Pagination**: Every list endpoint must accept and cap `pageSize` (default 20, max 100) and require deterministic sorting. | **Unbounded Queries**: Never perform unpaginated `.selectAll().toList()` on tables that grow indefinitely with user activity. |

---

### 1.4 Client UI Standards (Kotlin + Material 3)

|  WHAT TO BUILD |  WHAT TO AVOID |
|---|---|
| **Material 3 Design System (`androidx.compose.material3`)**: Modern Material You design system with dynamic color palette, curated emerald/forest tones, high-contrast surfaces, and dark theme support. | **Outdated Material 2 or Plain Web Views**: Never use deprecated Material 2 components or raw unstyled webviews when building client interfaces. |
| **Declarative Compose Architecture**: `Scaffold`, `NavigationBar`, `ElevatedCard`, `FilterChip`, `LinearProgressIndicator`, and `FloatingActionButton` adhering to Material 3 motion and guidelines. | **Hardcoded Spacing & Ad-Hoc Styling**: Avoid magic numbers; use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes`. |
| **Unified Kotlin Data Layer**: Direct sharing of Kotlin Serialization DTOs between the Ktor backend and Compose client via shared code or common client module. | **Redundant Serializers / Multiple Languages**: Avoid duplicating data definitions across separate disconnected language runtimes when one Kotlin codebase can power both. |
| **Material 3 Motion & Feedback**: Animated transitions, haptic/snackbar feedback on pickup requests, and smooth progress indicators for AI classification confidence. | **Static / Abrupt State Jumps**: Avoid jarring instant DOM or UI snaps without loading indicators or status feedback. |

---

### 1.5 Cloud & Cost Management Guardrails

|  WHAT TO BUILD |  WHAT TO AVOID |
|---|---|
| **ECS Fargate Serverless Compute**: Deploy container tasks with fine-tuned vCPU (0.25–0.5 vCPU) and RAM (0.5–1 GB) for minimal cost during development. | **Over-Provisioned EC2 Instances**: Never run costly idle GPU or large EC2 instances 24/7 for low-traffic student hackathon tiers. |
| **VPC S3 Gateway Endpoint**: Use free Gateway Endpoints for S3 access to avoid paying NAT Gateway data processing charges on image uploads. | **Unrestricted Public Storage**: Never make S3 buckets publicly readable. Always enforce private ACLs and issue temporary Presigned URLs. |
| **Structured JSON Logs with CloudWatch Retention**: Set CloudWatch log group retention to 7 or 14 days for development/staging environments. | **Indefinite Log Retention**: Never leave default "Never Expire" on high-volume access log groups. |
| **Automated Database Backups**: Enable 7-day automated RDS point-in-time recovery and snapshot testing. | **Untested Backups**: Never assume backups work without running a restore verification drill. |

---

## 2. API Contract & Error Format Convention

All EcoTrack endpoints must strictly adhere to the unified JSON error format:

```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "The requested collection request was not found.",
    "requestId": "c4b123e4-89b1-49b1-a7b2-8b4382098421",
    "details": [
      {
        "field": "id",
        "reason": "Must be a valid UUID corresponding to an existing record"
      }
    ]
  }
}
```

### Prohibited in API Responses:
- Never return Java/Kotlin stack traces to HTTP callers.
- Never return SQL statements or database error codes (`PSQLException`, constraint names).
- Never return `500 Internal Server Error` for predictable client validation errors. Use `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, or `422 Unprocessable Entity`.

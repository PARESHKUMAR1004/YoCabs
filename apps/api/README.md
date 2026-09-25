# YoCabs API

Spring Boot 4 / Java 21 modular monolith (DDD + clean architecture) backing the tourist, travel-partner,
driver and admin applications. PostgreSQL is authoritative; Flyway owns the schema.

## Run it

```bash
docker compose -f ../../infrastructure/docker/docker-compose.yml up -d postgres   # PostgreSQL on :5432
./gradlew bootRun                                                                  # API on :8080
./gradlew test                                                                     # needs Docker (Testcontainers)
```

From VS Code use the Spring Boot Dashboard (see `.vscode/launch.json`). Health: `GET /actuator/health`.

Create the first super administrator by setting `YOCABS_BOOTSTRAP_ADMIN_EMAIL` / `YOCABS_BOOTSTRAP_ADMIN_PASSWORD`
(min. 12 characters) before first start. There are no default credentials.

In development the login OTP is printed to the log (`[DEV OTP] code 123456 for ******3210`).
Request files for every endpoint live in [`/http`](../../http); start with `http/Auth/auth.http`.

## Modules

| Module | Responsibility |
|---|---|
| `identity` | OTP login (tourist/partner/driver), admin password login, JWT access tokens, rotating refresh tokens, partner self-registration, partner staff, own profile (`/profile`) |
| `travelpartner`, `vehicle` | Partner aggregate + service areas, fleet, composable eligibility rules; vehicle details, admin-managed facility catalogue, photos (shown to tourists once approved) |
| `pricing` | Per vehicle + trip type configuration, strategy-based pricing engine (one way / round trip / rental) |
| `routing` | `RouteCalculationService` port: `haversine` (default), `osrm`, `fixed` (demo) |
| `tripsearch` | `POST /trip-search`, and re-validation of a selected option at action time |
| `triprequest` | Persisted tourist trip request (created only when the tourist commits) |
| `negotiation` | One offer, one counter, accept/reject, configurable expiry |
| `booking` | Booking with price snapshot, 5% token, hold expiry, double-booking protection, cancellation/refund policy, driver assignment, trip start/complete |
| `payment` | Gateway port, signed idempotent webhooks, append-only transaction ledger, refunds |
| `driver`, `document` | Drivers; private document storage with admin verification |
| `review` | One review per completed booking; partner rating |
| `settlement` | Append-only partner ledger and payouts |
| `notification` | In-app notifications (event driven) + `PushSender` port |
| `admin`, `audit` | Partner verification gate, dashboard, users, audit trail, payments / refunds / cancellations views |
| `report` | Partner reports: earnings, trips, vehicles, drivers, cancellations |
| `support` | Help & support tickets (tourists, partners, drivers) worked by admins |

Flow: `search -> trip request -> [negotiate] -> book (hold) -> pay token -> confirmed -> driver -> trip -> settle -> review`.

## Security model

* Stateless JWT (HS256, 30 min) + rotating refresh tokens (reuse of a rotated token revokes the whole session).
* Roles: `TOURIST`, `PARTNER_OWNER`, `PARTNER_STAFF`, `DRIVER`, `ADMIN`, `SUPER_ADMIN`.
* Ownership is enforced server-side on every partner-scoped route (a supplied UUID is never trusted).
* Public: `/auth/**`, `POST /trip-search`, `/actuator/health`, the signed payment webhook. Everything else needs a token.
* Rate limiting (per IP) on login and search; in-memory, so use a shared store if you run several instances.
* Every response carries `X-Correlation-Id`; errors use `{code, message, correlationId}`.
* OTP codes are stored only as HMACs; admin accounts lock after repeated failures; secrets are never logged.

## Configuration

See `src/main/resources/application.yml` (documented defaults) and `application-prod.yml`.
The `prod` profile **refuses to start** with the development JWT secret or the sandbox payment gateway.

| Variable | Purpose |
|---|---|
| `YOCABS_JWT_SECRET` | >= 32 bytes; required in prod |
| `DB_URL` `DB_USER` `DB_PASSWORD` | PostgreSQL (prod profile) |
| `YOCABS_PAYMENT_GATEWAY` `YOCABS_PAYMENT_WEBHOOK_SECRET` | Payment provider adapter and webhook secret |
| `YOCABS_ROUTING_PROVIDER` `YOCABS_OSRM_BASE_URL` | `osrm` (recommended) or `haversine` |
| `YOCABS_STORAGE_PATH` | Document storage directory (mount a persistent volume) |
| `yocabs.booking.*` | token %, commission %, hold minutes, free-cancellation hours |
| `yocabs.negotiation.expiry-minutes` | Offer / counter-offer validity |
| `yocabs.verification.required-partner-documents` | Document types that must be approved before a partner can go live |

## Hosted testing (Railway)

A hosted deployment for testers runs the **`staging`** profile (`application-staging.yml`). It is
production-shaped (real database, secrets from the environment, HTTPS in front) but deliberately
keeps two development conveniences: one-time codes are written to the log, and the sandbox payment
gateway stays available. The `prod` profile refuses to start with either. **Never point real
customers at `staging`.**

One Railway project, two services:

| Service | Notes |
| --- | --- |
| `Postgres` | Railway's managed Postgres. |
| `api` | Built from `Dockerfile.railway` (compiles from source; `Dockerfile` stays for CI, which packages a pre-built jar). |

Set on `api` (database values use Railway references, so the password is never copied):

```
RAILWAY_DOCKERFILE_PATH=Dockerfile.railway   # required: railway.json's dockerfilePath is ignored
SPRING_PROFILES_ACTIVE=staging
DB_URL=jdbc:postgresql://${{Postgres.RAILWAY_PRIVATE_DOMAIN}}:5432/${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
YOCABS_JWT_SECRET=<64+ random characters>
YOCABS_BOOTSTRAP_ADMIN_EMAIL=<admin email>
YOCABS_BOOTSTRAP_ADMIN_PASSWORD=<strong password>
YOCABS_ROUTING_PROVIDER=osrm                 # osrm | google | haversine
YOCABS_CORS_ALLOWED_ORIGINS=<admin console origin, once it is hosted>
```

Deploy from this directory with `railway up -s api --ci`, then `railway domain -s api` for the
HTTPS URL. Migrations run on startup. To read a tester's one-time code, use
`railway logs -s api` and look for `[DEV OTP]`.

Known limits of the hosted setup:

- **Uploaded documents live on the container's disk and are lost on every redeploy.** Attach a
  volume (and run the container as a user that can write to it) or add object storage before
  testers upload anything they need to keep.
- OSRM's public demo server is fine for testing, not for launch. Use `google` (Routes API, billed,
  server-side key `YOCABS_GOOGLE_ROUTES_API_KEY`) or a self-hosted OSRM.
- `railway.json` uses the deprecated config-as-code format. It still works until 2026-12-01.

## Business rules (settlement and refund rules confirmed by the product owner, 2026-09-19)

These are explicit, isolated, configurable rules:

1. **Settlement model.** YoCabs collects the 5% token; the partner collects the rest. On trip completion the ledger
   nets `token - commission`: a surplus is owed to the partner, a shortfall is owed *by* the partner
   (`SettlementService.onBookingCompleted` is the only place to change this).
2. **Cancellation/refund policy.** Partner/admin cancellations always refund the token; a tourist is refunded up to
   `free-cancellation-hours` before the trip starts, not afterwards.
3. **Partner verification.** Which documents are mandatory is configuration (`required-partner-documents`), empty by default.
4. **Negotiation.** One offer per tourist per partner per trip request, ever (DB-enforced); a counter must lie strictly
   between the offer and the listed price.
5. **Availability.** Vehicles are blocked per date range by confirmed bookings and unexpired unpaid holds.

## What still needs external credentials or a provider choice

| Port | Provided today | Needed for production |
|---|---|---|
| `OtpSender` | `LoggingOtpSender` (dev only) | SMS/WhatsApp provider adapter |
| `PaymentGateway` | `SandboxPaymentGateway` (signed webhooks, dev only; refused in prod) | Razorpay/PhonePe/... adapter |
| `RouteCalculationService` | `haversine` estimator, `osrm` adapter | Point `osrm` at your own server or add a Google/Mapbox adapter |
| `DocumentStorage` | Local filesystem | S3-compatible private bucket adapter (recommended when running >1 instance) |
| `PushSender` | Logging no-op | FCM/APNs adapter |

Other production items: run a shared rate-limit store for multi-instance deployments, add a scheduler lock
(e.g. ShedLock) for the hold/negotiation expiry jobs when scaling out, and configure backups for PostgreSQL and the
document store.

# Banking Core

Backend-only digital banking engine built with Spring Boot 4 / Java 21. Customers
register, open accounts, and move money; every movement is recorded as an
immutable double-entry ledger posting that triggers an event-driven email
notification.

This repository is a multi-module Maven project (a "P0" learning/portfolio
build). **Stage 1 (Identity) is the only part that is implemented and runnable
today** — registration, login, and JWT-protected endpoints in
`account-service`. Everything else is scaffolded but not yet built. See
[Project status](#project-status) below.

## Modules

| Module | Port | Status |
|---|---|---|
| `account-service` | 8081 | **Implemented** — identity (register/login/JWT) |
| `transaction-service` | 8082 | Scaffold only |
| `notification-service` | 8083 | Scaffold only |
| `api-gateway` | 8080 | Scaffold only |

## Prerequisites

Install these before cloning:

- **JDK 21** (verify with `java -version`)
- **Docker** (Docker Desktop or OrbStack), running before you start the app or
  run tests. Testcontainers and the local infra stack both need it.
- Nothing else — the repo ships its own Maven wrapper (`./mvnw`), so a local
  Maven install is not required.

No IDE is required to run or test the service, but IntelliJ is recommended if
you want to use the `.http` request files described below.

## 1. Clone and build

```bash
git clone <this-repo-url>
cd booking-core
./mvnw clean verify
```

`./mvnw clean verify` compiles all four modules and runs their test suites.
`account-service`'s tests spin up disposable Postgres and Kafka containers via
Testcontainers automatically — this is the fastest way to confirm the clone
and your Docker setup both work, before you try to run anything by hand.

Expect this to take a minute or two the first time (Docker images are pulled),
and end with:

```
[INFO] Reactor Summary for booking-core 0.0.1-SNAPSHOT:
[INFO]
[INFO] booking-core ....................................... SUCCESS
[INFO] account-service .................................... SUCCESS
[INFO] api-gateway ........................................ SUCCESS
[INFO] transaction-service ................................ SUCCESS
[INFO] notification-service ............................... SUCCESS
[INFO] BUILD SUCCESS
```

If this fails, see [Troubleshooting](#troubleshooting) before going further.

## 2. Start local infrastructure

`account-service` needs a real, reachable Postgres database to actually run
(tests don't need this step — only running the app by hand does).

```bash
docker compose up -d
```

This starts:

| Service | Purpose | Where to check it |
|---|---|---|
| Postgres 17 | Database for all services (`account_db`, `txn_db`, `notification_db`) | `localhost:5432` |
| Kafka | Event bus (not used by Stage 1 yet, started for parity with later stages) | `localhost:9092` |
| Mailpit | Fake SMTP server + web UI for viewing "sent" emails (not used yet) | http://localhost:8025 |

Stop everything with `docker compose down` (add `-v` to also wipe the database
volume).

## 3. Run account-service

```bash
./mvnw -pl account-service spring-boot:run
```

Or run the packaged jar:

```bash
./mvnw -pl account-service -am package -DskipTests
java -jar account-service/target/account-service-0.0.1-SNAPSHOT.jar
```

On a clean database, Flyway runs its migration automatically on startup — you
should see `Successfully applied 1 migration` in the logs. The app is then
reachable at `http://localhost:8081`.

You'll also see a warning like:

```
No JWT key pair configured; generating an ephemeral RSA key pair for this run...
```

This is expected for local/dev runs — see [Configuration](#configuration).

### Health check

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

## 4. Testing the API (for QA)

A ready-to-use request collection is checked in at
[`requests/account-service.http`](requests/account-service.http). It covers
every endpoint and includes both the happy path and the expected
failure cases (duplicate email, bad password, missing token, etc.).

**In IntelliJ IDEA / any JetBrains IDE:** open the file and click the green
"run" arrow next to each request — no extra setup needed.

**From the command line**, equivalent `curl` calls:

```bash
# Register
curl -i -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"qa@example.com","password":"s3cret-password"}'

# Login
curl -i -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"qa@example.com","password":"s3cret-password"}'
# -> copy the "accessToken" value from the response

# Call a protected endpoint
curl -i http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer <paste accessToken here>"

# Public key set (for token verification tooling)
curl -i http://localhost:8081/.well-known/jwks.json
```

### API reference (Stage 1)

| Method | Path | Auth required | Success | Failure cases |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | No | `201 Created` | `400` invalid email/password, `409` email already registered |
| `POST` | `/api/v1/auth/login` | No | `200 OK` + access token | `401` wrong password or unknown email |
| `GET` | `/api/v1/auth/me` | Yes (Bearer token) | `200 OK` + profile | `401` missing/invalid/expired token |
| `GET` | `/.well-known/jwks.json` | No | `200 OK` + public key set | — |
| `GET` | `/actuator/health` | No | `200 OK` | — |

Every error response has this shape:

```json
{
  "timestamp": "2026-11-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/v1/auth/login"
}
```

## Running on a second machine (for QA)

1. Install JDK 21 and Docker on that machine.
2. Clone the repo.
3. `./mvnw clean verify` — confirms the toolchain works end to end.
4. `docker compose up -d` — starts Postgres/Kafka/Mailpit.
5. `./mvnw -pl account-service spring-boot:run` — starts the API on port 8081.
6. Open `requests/account-service.http` in IntelliJ, or use the `curl`
   commands above, to exercise the API.

No further setup is needed — every config value has a local default (see
below), so there's no secrets file to copy over for this stage.

## Configuration

`account-service` reads configuration from environment variables, each with a
working local default baked in
([`application.yml`](account-service/src/main/resources/application.yml)).
There is no committed `.env` file; [`.env.example`](.env.example) documents
the variables if you want to override any of them (e.g. point at a different
database).

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/account_db` | Matches `docker-compose.yml` |
| `DB_USERNAME` | `account` | |
| `DB_PASSWORD` | `account` | |
| `SERVER_PORT` | `8081` | |
| `JWT_ISSUER` | `account-service` | Embedded in every issued token |
| `JWT_ACCESS_TOKEN_TTL` | `PT15M` | ISO-8601 duration |
| `JWT_PRIVATE_KEY_LOCATION` | *(blank)* | e.g. `file:/path/to/private.pem`. Blank = generate an ephemeral key pair at startup |
| `JWT_PUBLIC_KEY_LOCATION` | *(blank)* | Must be set together with the private key |

**Note on the ephemeral key pair:** if you don't set the two `JWT_*_KEY_LOCATION`
variables, a new RSA key pair is generated every time the app starts. Tokens
issued before a restart become invalid after it. This is fine for local
QA/dev sessions but is called out explicitly so it isn't mistaken for a bug —
if you see previously-issued tokens suddenly rejected after restarting the
service, that's why.

## Running tests only

```bash
./mvnw clean verify                 # all modules
./mvnw -pl account-service test     # account-service only
```

Tests require Docker to be running (Testcontainers starts disposable
Postgres/Kafka instances per test run — separate from the `docker compose`
stack above).

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Failed to connect to the docker API` during build/tests | Docker isn't running | Start Docker Desktop/OrbStack, wait for it to finish starting, retry |
| `Failed to determine a suitable driver class` when running (not testing) the app | No reachable Postgres at `DB_URL` | Run `docker compose up -d` first |
| Build fails with Java version errors | Wrong JDK active | `java -version` must report 21; set `JAVA_HOME` accordingly |
| Port `8081` / `5432` / `9092` / `8025` already in use | Another process is using the port | Stop the other process, or change `SERVER_PORT` / edit `docker-compose.yml` |
| A previously issued token is now rejected after a restart | Ephemeral JWT key regenerated on startup | Expected in dev; log in again, or configure `JWT_PRIVATE_KEY_LOCATION`/`JWT_PUBLIC_KEY_LOCATION` for a stable key |

## Project status

| Stage | Scope | Status |
|---|---|---|
| Stage 1 — Identity | Register, login, JWT issuance, protected endpoints | **Done** (`account-service`) |
| Stage 2 — Accounts | Open account, list/view accounts | Not started |
| Stage 3 — Ledger | Deposit/withdraw/transfer, double-entry postings, concurrency safety | Not started |
| Stage 4 — Idempotency | `Idempotency-Key` support for safe retries | Not started |
| Stage 5 — Events | Transactional outbox, Kafka publishing | Not started |
| Stage 5b — Notifications | Email on each posting, consumer dedup | Not started |
| Stage 6 — Operations | Dockerfiles per service, full docker-compose, smoke test | Not started |
| Stage 7 — Gateway | Spring Cloud Gateway routing, JWT validation at the edge | Not started |

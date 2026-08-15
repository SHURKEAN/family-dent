# Family Dent Clinic Management System

A Turkish-language Spring Boot application for patient records, conflict-safe appointments, public appointment requests, weekly dentist availability, patient history, and clinical visit notes.

## Included

- Session login with Admin, Dentist, and Receptionist roles
- Patient creation, editing, search, profiles, notes, archiving, and restoration
- Appointment creation, rescheduling, cancellation, completion, and no-show tracking
- PostgreSQL exclusion constraints preventing overlapping dentist bookings
- Public booking requests using name, phone, reason, dentist, date, and time
- Receptionist approval/rejection with automatic patient and appointment creation
- Configurable dentist working hours and appointment duration
- Structured visit notes and patient appointment history
- BCrypt passwords, CSRF, Content Security Policy, secure production cookies, and security headers
- Persistent booking abuse limits and login throttling
- Admin-visible authentication and data-change audit events
- Flyway migrations, health probes, Prometheus metrics, automated backups, and CI tests
- Production Docker stack with Caddy-managed HTTPS and private application/database networking

## Requirements

- Java 21
- PostgreSQL 14 or newer
- Maven 3.9+ or the included Maven wrapper
- Docker Desktop/Compose for the supplied local database

## Run locally

Create an ignored local configuration file, replace both placeholders with
unique random values, and keep that file only on your computer:

```powershell
Copy-Item .env.example .env
notepad .env
```

If you already have a populated PostgreSQL Docker volume, use its existing
database password in `.env`. Start the local PostgreSQL container:

```powershell
docker compose up -d
```

Then start Spring Boot:

```powershell
$env:JAVA_HOME = (Resolve-Path ".tools\jdk21\jdk-21.0.12+8").Path
.\mvnw.cmd spring-boot:run
```

Open:

- Staff login: <http://localhost:8080/login>
- Public booking: <http://localhost:8080/randevu-al>

The existing local database retains its current users. For a brand-new empty local database, explicitly provide a one-time bootstrap administrator before the first start:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_ADMIN_USERNAME = "your-admin-name"
$env:BOOTSTRAP_ADMIN_PASSWORD = "a-unique-password-of-at-least-12-characters"
```

After the account is created, clear those variables and manage passwords from **Çalışanlar**. Editing bootstrap values does not modify an existing database account.

The local database password and request-fingerprint key are loaded from the
ignored `.env` file. No real credential belongs in `application*.yml`, Compose
files, documentation, or Git.

## Tests

Run the H2-backed unit and security suite:

```powershell
.\mvnw.cmd test
```

CI additionally runs Flyway against PostgreSQL 17 and builds the production container. The workflow is in `.github/workflows/ci.yml`.

## Production deployment

Follow [docs/PRODUCTION.md](docs/PRODUCTION.md). The production profile requires external secret files and refuses known or weak credentials. The supplied production Compose stack exposes only Caddy on ports 80/443; Java and PostgreSQL do not publish host ports.

Do not copy local credentials into production, expose port 5432, or publish the application directly on port 8080.

## Main production configuration

| Variable | Production behavior |
|---|---|
| `APP_DOMAIN` | Required real hostname used by Caddy for automatic HTTPS |
| `ACME_EMAIL` | Required certificate-administration email |
| `POSTGRES_DB` / `POSTGRES_USER` | Required database identity |
| `DB_PASSWORD` | Loaded from Docker secret; at least 16 characters |
| `FINGERPRINT_SECRET` | Loaded from Docker secret; at least 32 characters |
| `BOOTSTRAP_ADMIN_PASSWORD` | Loaded from Docker secret; at least 12 characters |
| `SESSION_TIMEOUT` | Authenticated session lifetime; default `45m` |
| `LOGIN_MAXIMUM_FAILURES` | Account/client failures before a temporary block; default `5` |
| `BOOKING_MAXIMUM_REQUESTS_PER_IP_PER_HOUR` | Public submission allowance; default `10` |
| `AUDIT_RETENTION_DAYS` | Security audit retention; default `365` |
| `BACKUP_INTERVAL_SECONDS` | Backup frequency; default `86400` |
| `BACKUP_RETENTION_DAYS` | On-server backup retention; default `30` |

## Role permissions

| Capability | Admin | Receptionist | Dentist |
|---|:---:|:---:|:---:|
| View patients, appointments, and schedule | Yes | Yes | Yes |
| Add/edit/archive patients | Yes | Yes | No |
| Create/reschedule/cancel appointments | Yes | Yes | No |
| Review public appointment requests | Yes | Yes | No |
| Manage dentist working hours | Yes | Yes | No |
| Complete or mark no-show | Yes | Yes | Assigned visits |
| Add/edit visit notes | Yes | No | Assigned completed visits |
| Manage employees and audit events | Yes | No | No |

# Family Dent production deployment

The production stack contains Caddy, the Spring Boot application, PostgreSQL, and an automated backup worker. Only Caddy publishes host ports. The application and database stay on Docker networks, and PostgreSQL's network is marked internal.

## 1. Prepare the server and domain

Use a Linux server with Docker Engine and the Docker Compose plugin. Point the domain's DNS `A` record (and `AAAA` when IPv6 is configured) to the server. Allow inbound TCP ports 80 and 443 and UDP 443. Do not expose ports 8080 or 5432.

Caddy obtains and renews the TLS certificate automatically after DNS is correct and ports 80/443 are reachable.

## 2. Create production configuration

Copy the example and replace every placeholder:

```bash
cp .env.production.example .env.production
mkdir -p secrets /srv/family-dent/backups
chmod 700 secrets /srv/family-dent/backups
```

Create these files with unique values:

```text
secrets/db_password.txt
secrets/bootstrap_admin_password.txt
secrets/fingerprint_secret.txt
```

The database password should be at least 16 characters, the bootstrap password at least 12, and the fingerprint key at least 32 random characters. The application deliberately refuses to start under the `prod` profile when these requirements are not met. Secret `.txt` files and `.env.production` are ignored by Git.

## 3. Validate and start

```bash
docker compose --env-file .env.production -f compose.production.yaml config --quiet
docker compose --env-file .env.production -f compose.production.yaml up -d --build
docker compose --env-file .env.production -f compose.production.yaml ps
docker compose --env-file .env.production -f compose.production.yaml logs --tail=100 app caddy
```

Open `https://YOUR_DOMAIN/login`. After the first login:

1. Reset the administrator password from **Çalışanlar**.
2. Set `BOOTSTRAP_ADMIN_ENABLED=false` in `.env.production`.
3. Recreate only the application container:

```bash
docker compose --env-file .env.production -f compose.production.yaml up -d --no-deps app
```

The bootstrap settings never rename or change an account already stored in PostgreSQL.

## 4. Health, metrics, and logs

Docker checks `/actuator/health/readiness` inside the application container. Caddy also removes unhealthy application instances from proxying. Spring Boot exposes health and Prometheus metrics internally; Caddy returns 404 for every public `/actuator` URL.

Useful checks:

```bash
docker compose --env-file .env.production -f compose.production.yaml ps
docker compose --env-file .env.production -f compose.production.yaml exec app curl --fail http://localhost:8080/actuator/health
docker compose --env-file .env.production -f compose.production.yaml logs --since=30m app caddy postgres backup
```

Admins can inspect the last 250 authentication and data-change events under **Denetim Kayıtları**. Events retain pseudonymized client fingerprints and no submitted passwords or request bodies. The default retention is 365 days and is configurable with `AUDIT_RETENTION_DAYS`.

## 5. Backups and restore drills

The backup service creates a verified, compressed PostgreSQL archive immediately at startup and then every `BACKUP_INTERVAL_SECONDS`. Old archives are removed after `BACKUP_RETENTION_DAYS`. Each archive has a SHA-256 checksum.

Create an extra backup:

```bash
docker compose --env-file .env.production -f compose.production.yaml exec backup /bin/sh /scripts/backup.sh once
```

Copy backups to a separate machine or encrypted object-storage account. A directory on the same server does not protect against server loss. Periodically perform a restore drill on a non-production database.

Restoring overwrites the selected database. Stop writers, identify the exact archive, and run:

```bash
docker compose --env-file .env.production -f compose.production.yaml stop caddy app backup
docker compose --env-file .env.production -f compose.production.yaml --profile tools run --rm \
  -e CONFIRM_RESTORE=RESTORE_FAMILY_DENT restore /backups/family-dent-YYYYMMDDTHHMMSSZ.dump
docker compose --env-file .env.production -f compose.production.yaml up -d
```

## 6. Updates and rollback preparation

Before every update, take a backup and save the deployed Git commit identifier. Then rebuild the app container:

```bash
docker compose --env-file .env.production -f compose.production.yaml exec backup /bin/sh /scripts/backup.sh once
docker compose --env-file .env.production -f compose.production.yaml build --pull app
docker compose --env-file .env.production -f compose.production.yaml up -d --no-deps app
```

Check application and Caddy logs after Flyway completes. Flyway migrations are forward-only; database rollback requires restoring the pre-update backup.

## User-owned deployment inputs

The repository is ready for these external inputs, which are intentionally not stored here:

- The real domain and DNS access
- The production server or hosting account
- Three production secret values
- An off-server backup destination
- Clinic-approved privacy notice, retention policy, and local health-data compliance decisions

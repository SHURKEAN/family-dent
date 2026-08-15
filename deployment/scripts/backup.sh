#!/bin/sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
INTERVAL="${BACKUP_INTERVAL_SECONDS:-86400}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
PASSWORD_FILE="${DB_PASSWORD_FILE:-/run/secrets/db_password}"

if [ ! -r "$PASSWORD_FILE" ]; then
    echo "Database password secret is missing: $PASSWORD_FILE" >&2
    exit 1
fi

case "$INTERVAL" in *[!0-9]*|'') echo "BACKUP_INTERVAL_SECONDS must be a positive integer" >&2; exit 1;; esac
case "$RETENTION_DAYS" in *[!0-9]*|'') echo "BACKUP_RETENTION_DAYS must be a positive integer" >&2; exit 1;; esac
if [ "$INTERVAL" -lt 1 ] || [ "$RETENTION_DAYS" -lt 1 ]; then
    echo "Backup interval and retention must be greater than zero" >&2
    exit 1
fi

export PGPASSWORD="$(cat "$PASSWORD_FILE")"
mkdir -p "$BACKUP_DIR"

create_backup() {
    timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
    final_file="$BACKUP_DIR/family-dent-$timestamp.dump"
    temporary_file="$final_file.incomplete"

    echo "Creating PostgreSQL backup: $final_file"
    pg_dump --format=custom --compress=6 --no-owner --no-privileges --file="$temporary_file"
    pg_restore --list "$temporary_file" >/dev/null
    mv "$temporary_file" "$final_file"
    sha256sum "$final_file" >"$final_file.sha256"
    find "$BACKUP_DIR" -type f \( -name 'family-dent-*.dump' -o -name 'family-dent-*.dump.sha256' \) \
        -mtime "+$RETENTION_DAYS" -delete
    echo "Backup completed: $final_file"
}

if [ "${1:-once}" = "loop" ]; then
    while true; do
        create_backup
        sleep "$INTERVAL"
    done
fi

create_backup

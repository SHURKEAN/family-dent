#!/bin/sh
set -eu

PASSWORD_FILE="${DB_PASSWORD_FILE:-/run/secrets/db_password}"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: restore.sh /backups/family-dent-YYYYMMDDTHHMMSSZ.dump" >&2
    exit 2
fi
if [ "${CONFIRM_RESTORE:-}" != "RESTORE_FAMILY_DENT" ]; then
    echo "Restore refused. Set CONFIRM_RESTORE=RESTORE_FAMILY_DENT after stopping the app service." >&2
    exit 3
fi
if [ ! -r "$PASSWORD_FILE" ]; then
    echo "Database password secret is missing: $PASSWORD_FILE" >&2
    exit 1
fi
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Backup file not found: $BACKUP_FILE" >&2
    exit 1
fi

export PGPASSWORD="$(cat "$PASSWORD_FILE")"
pg_restore --list "$BACKUP_FILE" >/dev/null
pg_restore --clean --if-exists --no-owner --no-privileges --exit-on-error --dbname="$PGDATABASE" "$BACKUP_FILE"
echo "Restore completed from: $BACKUP_FILE"

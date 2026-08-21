#!/usr/bin/env sh
set -eu

backup_dir=${1:-./backups}
retention_days=${BACKUP_RETENTION_DAYS:-7}
env_file=${APP_ENV_FILE:-.env}

case "$retention_days" in *[!0-9]*|'') printf '%s\n' "[backup] ERROR: BACKUP_RETENTION_DAYS 必须是数字" >&2; exit 1 ;; esac
mkdir -p "$backup_dir"
stamp=$(date +%Y%m%d_%H%M%S)
final_file=$backup_dir/lishuiwan_$stamp.sql.gz
temp_file=$final_file.tmp
trap 'rm -f "$temp_file"' EXIT HUP INT TERM

docker compose --env-file "$env_file" exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers lishuiwan' \
  | gzip -9 > "$temp_file"
gzip -t "$temp_file"
mv "$temp_file" "$final_file"
trap - EXIT HUP INT TERM
find "$backup_dir" -name 'lishuiwan_*.sql.gz' -mtime "+$retention_days" -delete
printf '%s\n' "$final_file"

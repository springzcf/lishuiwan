#!/usr/bin/env sh
set -eu
backup_dir="${1:-./backups}"
mkdir -p "$backup_dir"
stamp="$(date +%Y%m%d_%H%M%S)"
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers lishuiwan' | gzip > "$backup_dir/lishuiwan_$stamp.sql.gz"
find "$backup_dir" -name 'lishuiwan_*.sql.gz' -mtime +7 -delete
echo "$backup_dir/lishuiwan_$stamp.sql.gz"

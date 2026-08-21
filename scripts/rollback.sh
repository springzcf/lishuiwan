#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(dirname "$script_dir")
env_file=${APP_ENV_FILE:-$project_dir/.env}
releases_dir=$project_dir/releases
current_link=$project_dir/current

info() { printf '%s\n' "[rollback] $*"; }
fail() { printf '%s\n' "[rollback] ERROR: $*" >&2; exit 1; }
env_value() { awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print; exit }' "$env_file"; }
as_root() { if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi; }

[ -L "$current_link" ] || fail "当前版本链接不存在"
current_target=$(readlink "$current_link")
requested=${1:-}
if [ -n "$requested" ]; then
  case "$requested" in *[!A-Za-z0-9_.-]*) fail "版本参数包含非法字符" ;; esac
  target=$releases_dir/$(basename "$requested")
else
  target=$(find "$releases_dir" -mindepth 1 -maxdepth 1 -type d -name '20????????_??????' | sort -r | awk -v current="$current_target" '$0 != current { print; exit }')
fi
[ -n "$target" ] && [ -s "$target/api.jar" ] && [ -f "$target/h5/index.html" ] && [ -f "$target/admin/index.html" ] || fail "没有找到可用的回滚版本"
[ "$target" != "$current_target" ] || fail "目标版本已经是当前版本"

stamp=$(date +%Y%m%d_%H%M%S)
next_link=$project_dir/.rollback-$stamp
ln -s "$target" "$next_link"
mv -Tf "$next_link" "$current_link"
info "切换到 $(basename "$target")，重启 API"
as_root systemctl restart lishuiwan-api

api_port=$(env_value SERVER_PORT)
api_port=${api_port:-8080}
elapsed=0
while [ "$elapsed" -lt 120 ]; do
  if curl -fsS "http://127.0.0.1:${api_port}/actuator/health/readiness" >/dev/null 2>&1; then
    info "回滚成功：$(basename "$target")"
    exit 0
  fi
  sleep 3
  elapsed=$((elapsed + 3))
done

info "目标版本启动失败，恢复原版本"
restore_link=$project_dir/.restore-$stamp
ln -s "$current_target" "$restore_link"
mv -Tf "$restore_link" "$current_link"
as_root systemctl restart lishuiwan-api || true
fail "回滚版本健康检查失败"

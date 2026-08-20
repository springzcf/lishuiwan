#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(dirname "$script_dir")
cd "$project_dir"

env_file=${APP_ENV_FILE:-.env}
backup_dir=${BACKUP_DIR:-./backups}
wait_seconds=${DEPLOY_WAIT_SECONDS:-180}

info() { printf '%s\n' "[deploy] $*"; }
fail() { printf '%s\n' "[deploy] ERROR: $*" >&2; exit 1; }
env_value() {
  awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print; exit }' "$env_file"
}
require_value() {
  value=$(env_value "$1")
  [ -n "$value" ] || fail "$1 未配置"
  case "$value" in
    *change-me*|*example.com*|wx0000000000000000) fail "$1 仍是示例值" ;;
  esac
}
compose() { docker compose --env-file "$env_file" "$@"; }

command -v docker >/dev/null 2>&1 || fail "未安装 Docker"
docker compose version >/dev/null 2>&1 || fail "未安装 Docker Compose v2"
docker info >/dev/null 2>&1 || fail "当前用户无权访问 Docker，请加入 docker 组或使用有权限的部署账号"
[ -f "$env_file" ] || fail "缺少 $env_file，请先复制 .env.example 并填写正式配置"

for key in DB_PASSWORD MYSQL_ROOT_PASSWORD REDIS_PASSWORD JWT_SECRET WECHAT_APP_ID WECHAT_APP_SECRET; do
  require_value "$key"
done

initial_admin_password=$(env_value INITIAL_ADMIN_PASSWORD)
if [ -n "$initial_admin_password" ]; then
  require_value INITIAL_ADMIN_USERNAME
  require_value INITIAL_ADMIN_PASSWORD
  [ "${#initial_admin_password}" -ge 12 ] || fail "INITIAL_ADMIN_PASSWORD 至少需要 12 个字符"
fi

jwt_secret=$(env_value JWT_SECRET)
[ "${#jwt_secret}" -ge 32 ] || fail "JWT_SECRET 至少需要 32 个字符"
[ "$(env_value DEV_LOGIN_ENABLED)" = "false" ] || fail "生产环境 DEV_LOGIN_ENABLED 必须为 false"
[ "$(env_value MOCK_PAYMENT_ENABLED)" = "false" ] || fail "生产环境 MOCK_PAYMENT_ENABLED 必须为 false"

if [ "$(env_value WECHAT_OFFICIAL_ENABLED)" = "true" ]; then
  for key in WECHAT_OFFICIAL_APP_ID WECHAT_OFFICIAL_APP_SECRET WECHAT_OFFICIAL_OAUTH_CALLBACK_URL H5_BASE_URL; do
    require_value "$key"
  done
  case "$(env_value WECHAT_OFFICIAL_OAUTH_CALLBACK_URL)" in https://*) ;; *) fail "WECHAT_OFFICIAL_OAUTH_CALLBACK_URL 必须使用 HTTPS" ;; esac
  case "$(env_value H5_BASE_URL)" in https://*) ;; *) fail "H5_BASE_URL 必须使用 HTTPS" ;; esac
fi

if [ "$(env_value WECHAT_PAY_ENABLED)" = "true" ]; then
  for key in WECHAT_PAY_MCH_ID WECHAT_PAY_SERIAL_NO WECHAT_PAY_API_V3_KEY WECHAT_PAY_NOTIFY_URL; do
    require_value "$key"
  done
  case "$(env_value WECHAT_PAY_NOTIFY_URL)" in https://*) ;; *) fail "WECHAT_PAY_NOTIFY_URL 必须使用 HTTPS" ;; esac
  private_key=$(env_value WECHAT_PAY_PRIVATE_KEY_PATH)
  [ "$private_key" = "/run/secrets/apiclient_key.pem" ] || fail "容器内商户私钥路径应为 /run/secrets/apiclient_key.pem"
  [ -s secrets/apiclient_key.pem ] || fail "缺少 secrets/apiclient_key.pem"
fi

info "校验 Docker Compose 配置"
compose config --quiet

mysql_id=$(compose ps -q mysql 2>/dev/null || true)
if [ -n "$mysql_id" ] && [ "$(docker inspect -f '{{.State.Running}}' "$mysql_id" 2>/dev/null || true)" = "true" ]; then
  info "部署前备份数据库"
  APP_ENV_FILE="$env_file" sh scripts/backup.sh "$backup_dir"
else
  info "首次部署，当前没有运行中的数据库，跳过备份"
fi

info "构建生产镜像"
compose build --pull
info "启动服务并清理失效容器"
compose up -d --remove-orphans

info "等待 API 健康，最长 ${wait_seconds} 秒"
elapsed=0
while [ "$elapsed" -lt "$wait_seconds" ]; do
  api_id=$(compose ps -q api 2>/dev/null || true)
  if [ -n "$api_id" ]; then
    api_health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$api_id" 2>/dev/null || true)
    [ "$api_health" = "healthy" ] && break
    [ "$api_health" = "exited" ] && break
  fi
  sleep 3
  elapsed=$((elapsed + 3))
done

api_id=$(compose ps -q api 2>/dev/null || true)
api_health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$api_id" 2>/dev/null || true)
if [ "$api_health" != "healthy" ]; then
  compose ps >&2
  compose logs --tail=120 api >&2
  fail "API 未在规定时间内进入 healthy 状态"
fi

web_id=$(compose ps -q web 2>/dev/null || true)
[ -n "$web_id" ] || fail "Web 容器未创建"
[ "$(docker inspect -f '{{.State.Running}}' "$web_id")" = "true" ] || fail "Web 容器未运行"

http_port=$(env_value HTTP_PORT)
http_port=${http_port:-80}
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://127.0.0.1:${http_port}/h5/activity" >/dev/null || fail "H5 活动入口探测失败"
  curl -fsS "http://127.0.0.1:${http_port}/admin/" >/dev/null || fail "管理端入口探测失败"
fi

compose ps
info "部署成功：活动 /h5/activity，我的 /h5/mine，管理端 /admin/"

#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(dirname "$script_dir")
cd "$project_dir"

env_file=${APP_ENV_FILE:-.env}
backup_dir=${BACKUP_DIR:-./backups}
wait_seconds=${DEPLOY_WAIT_SECONDS:-180}
release_keep_count=${RELEASE_KEEP_COUNT:-}

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
as_root() {
  if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi
}
service_action() { as_root systemctl "$@" lishuiwan-api; }

[ -f "$env_file" ] || fail "缺少 $env_file，请先复制 .env.example 并填写正式配置"
for command_name in docker java mvn node npm npx curl systemctl nginx; do
  command -v "$command_name" >/dev/null 2>&1 || fail "未安装 $command_name，请先运行 sudo sh scripts/install-host.sh"
done
docker compose version >/dev/null 2>&1 || fail "未安装 Docker Compose v2"
docker info >/dev/null 2>&1 || fail "当前用户无权访问 Docker，请加入 docker 组后重新登录"
java -version 2>&1 | grep -Eq 'version "17[.]|openjdk 17[.]' || fail "生产 API 必须使用 Java 17"
node -e 'const [a,b]=process.versions.node.split(".").map(Number);process.exit(a>20||(a===20&&b>=19)?0:1)' || fail "前端构建需要 Node.js >= 20.19，建议使用 Node.js 22"
as_root test -f /etc/systemd/system/lishuiwan-api.service || fail "systemd 服务未安装，请先运行 sudo sh scripts/install-host.sh"

for key in DB_PASSWORD MYSQL_ROOT_PASSWORD REDIS_PASSWORD JWT_SECRET PUBLIC_DOMAIN SERVER_ADDRESS DB_URL DB_USERNAME REDIS_HOST UPLOAD_DIR; do
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
[ "$(env_value DEV_LOGIN_ENABLED)" = false ] || fail "生产环境 DEV_LOGIN_ENABLED 必须为 false"
[ "$(env_value MOCK_PAYMENT_ENABLED)" = false ] || fail "生产环境 MOCK_PAYMENT_ENABLED 必须为 false"
[ "$(env_value SPRING_PROFILES_ACTIVE)" = prod ] || fail "SPRING_PROFILES_ACTIVE 必须为 prod"
[ "$(env_value SERVER_ADDRESS)" = 127.0.0.1 ] || fail "生产 API 的 SERVER_ADDRESS 必须为 127.0.0.1"
[ "$(env_value REDIS_HOST)" = 127.0.0.1 ] || fail "宿主机部署的 REDIS_HOST 必须为 127.0.0.1"
case "$(env_value DB_URL)" in jdbc:mysql://127.0.0.1:*) ;; *) fail "宿主机部署的 DB_URL 必须连接 127.0.0.1" ;; esac

if [ "$(env_value WECHAT_OFFICIAL_ENABLED)" = true ]; then
  for key in WECHAT_OFFICIAL_APP_ID WECHAT_OFFICIAL_APP_SECRET WECHAT_OFFICIAL_OAUTH_CALLBACK_URL H5_BASE_URL; do
    require_value "$key"
  done
  case "$(env_value WECHAT_OFFICIAL_OAUTH_CALLBACK_URL)" in https://*) ;; *) fail "WECHAT_OFFICIAL_OAUTH_CALLBACK_URL 必须使用 HTTPS" ;; esac
  case "$(env_value H5_BASE_URL)" in https://*) ;; *) fail "H5_BASE_URL 必须使用 HTTPS" ;; esac
fi

if [ "$(env_value WECHAT_PAY_ENABLED)" = true ]; then
  for key in WECHAT_PAY_MCH_ID WECHAT_PAY_SERIAL_NO WECHAT_PAY_API_V3_KEY WECHAT_PAY_NOTIFY_URL WECHAT_PAY_PRIVATE_KEY_PATH; do
    require_value "$key"
  done
  case "$(env_value WECHAT_PAY_NOTIFY_URL)" in https://*) ;; *) fail "WECHAT_PAY_NOTIFY_URL 必须使用 HTTPS" ;; esac
  private_key=$(env_value WECHAT_PAY_PRIVATE_KEY_PATH)
  [ -s "$private_key" ] || fail "微信支付商户私钥不存在或为空：$private_key"
fi

release_keep_count=${release_keep_count:-$(env_value RELEASE_KEEP_COUNT)}
release_keep_count=${release_keep_count:-5}
case "$release_keep_count" in *[!0-9]*|'') fail "RELEASE_KEEP_COUNT 必须是数字" ;; esac
[ "$release_keep_count" -ge 2 ] || fail "RELEASE_KEEP_COUNT 至少为 2，才能保留回滚版本"

info "校验并启动 MySQL、Redis（Docker 不再构建 Java 或 Web 镜像）"
compose config --quiet
mysql_id=$(compose ps -q mysql 2>/dev/null || true)
if [ -n "$mysql_id" ] && [ "$(docker inspect -f '{{.State.Running}}' "$mysql_id" 2>/dev/null || true)" = true ]; then
  info "部署前备份数据库"
  APP_ENV_FILE="$env_file" BACKUP_RETENTION_DAYS="$(env_value BACKUP_RETENTION_DAYS)" sh scripts/backup.sh "$backup_dir"
else
  info "首次部署，当前没有运行中的数据库，跳过备份"
fi
compose up -d --remove-orphans mysql redis

info "等待 MySQL、Redis 健康，最长 ${wait_seconds} 秒"
elapsed=0
mysql_health=
redis_health=
while [ "$elapsed" -lt "$wait_seconds" ]; do
  mysql_id=$(compose ps -q mysql 2>/dev/null || true)
  redis_id=$(compose ps -q redis 2>/dev/null || true)
  mysql_health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$mysql_id" 2>/dev/null || true)
  redis_health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$redis_id" 2>/dev/null || true)
  [ "$mysql_health" = healthy ] && [ "$redis_health" = healthy ] && break
  sleep 3
  elapsed=$((elapsed + 3))
done
[ "$mysql_health" = healthy ] || { compose logs --tail=100 mysql >&2; fail "MySQL 未进入 healthy 状态"; }
[ "$redis_health" = healthy ] || { compose logs --tail=100 redis >&2; fail "Redis 未进入 healthy 状态"; }

info "在宿主机构建 Spring Boot API"
mvn -B -s lishuiwan-api/settings.xml -f lishuiwan-api/pom.xml clean package
jar_file=$(find lishuiwan-api/target -maxdepth 1 -type f -name 'lishuiwan-api-*.jar' ! -name '*.original' | head -n 1)
[ -n "$jar_file" ] && [ -s "$jar_file" ] || fail "未找到 API JAR 构建产物"

info "在宿主机构建服务号 H5"
npm_registry=$(env_value NPM_REGISTRY)
npm_registry=${npm_registry:-https://registry.npmmirror.com}
npm_config_registry="$npm_registry" npm ci --prefix h5
npm run build --prefix h5
info "在宿主机构建管理端"
CI=true npm_config_registry="$npm_registry" npx --yes pnpm@11.19.0 --dir admin-web install --frozen-lockfile
npm_config_registry="$npm_registry" npx --yes pnpm@11.19.0 --dir admin-web run build

stamp=$(date +%Y%m%d_%H%M%S)
releases_dir=$project_dir/releases
release_dir=$releases_dir/$stamp
current_link=$project_dir/current
old_target=
[ -L "$current_link" ] && old_target=$(readlink "$current_link")

info "创建版本目录 $release_dir"
mkdir -p "$release_dir/h5" "$release_dir/admin"
install -m 0644 "$jar_file" "$release_dir/api.jar"
cp -R h5/dist/. "$release_dir/h5/"
cp -R admin-web/dist/. "$release_dir/admin/"
chmod -R a+rX "$release_dir"

next_link=$project_dir/.current-$stamp
ln -s "$release_dir" "$next_link"
mv -Tf "$next_link" "$current_link"

info "重启 API 并等待健康检查"
service_action restart
elapsed=0
api_port=$(env_value SERVER_PORT)
api_port=${api_port:-8080}
while [ "$elapsed" -lt "$wait_seconds" ]; do
  if curl -fsS "http://127.0.0.1:${api_port}/actuator/health/readiness" >/dev/null 2>&1; then break; fi
  sleep 3
  elapsed=$((elapsed + 3))
done

if ! curl -fsS "http://127.0.0.1:${api_port}/actuator/health/readiness" >/dev/null 2>&1; then
  as_root journalctl -u lishuiwan-api -n 120 --no-pager >&2 || true
  if [ -n "$old_target" ]; then
    info "新版本健康检查失败，自动回滚到 $old_target"
    rollback_link=$project_dir/.rollback-$stamp
    ln -s "$old_target" "$rollback_link"
    mv -Tf "$rollback_link" "$current_link"
    service_action restart || true
  else
    service_action stop || true
  fi
  fail "API 未在规定时间内启动，新版本保留在 $release_dir 供排查"
fi

info "重新生成并加载 Nginx 配置"
APP_ENV_FILE="$env_file" sh scripts/configure-nginx.sh
domain=$(env_value PUBLIC_DOMAIN)
curl -fsS -H "Host: $domain" http://127.0.0.1/h5/activity >/dev/null || fail "H5 活动入口探测失败"
curl -fsS -H "Host: $domain" http://127.0.0.1/admin/ >/dev/null || fail "管理端入口探测失败"

info "清理超出保留数量的历史发布版本"
find "$releases_dir" -mindepth 1 -maxdepth 1 -type d -name '20????????_??????' | sort -r | awk -v keep="$release_keep_count" 'NR > keep' | while IFS= read -r old_release; do
  [ -n "$old_release" ] && rm -rf -- "$old_release"
done

compose ps
service_action --no-pager status || true
public_scheme=http
if [ -n "$(env_value SSL_CERTIFICATE)" ] && [ -n "$(env_value SSL_CERTIFICATE_KEY)" ]; then public_scheme=https; fi
info "部署成功：${public_scheme}://$domain/h5/activity、${public_scheme}://$domain/h5/mine、${public_scheme}://$domain/admin/"
[ "$public_scheme" = https ] || info "注意：当前仍是临时 HTTP，微信服务号和微信支付上线前必须配置 HTTPS 证书"

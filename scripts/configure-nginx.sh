#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(dirname "$script_dir")
env_file=${APP_ENV_FILE:-$project_dir/.env}

info() { printf '%s\n' "[nginx] $*"; }
fail() { printf '%s\n' "[nginx] ERROR: $*" >&2; exit 1; }
env_value() {
  awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print; exit }' "$env_file"
}
as_root() {
  if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi
}
escape_sed() { printf '%s' "$1" | sed 's/[|&\\]/\\&/g'; }

[ -f "$env_file" ] || fail "缺少 $env_file"
command -v nginx >/dev/null 2>&1 || fail "未安装 Nginx，请先运行 sudo sh scripts/install-host.sh"

domain=$(env_value PUBLIC_DOMAIN)
api_port=$(env_value SERVER_PORT)
certificate=$(env_value SSL_CERTIFICATE)
certificate_key=$(env_value SSL_CERTIFICATE_KEY)
api_port=${api_port:-8080}

[ -n "$domain" ] || fail "PUBLIC_DOMAIN 未配置"
case "$domain" in
  *[!A-Za-z0-9.-]*|.*|*.) fail "PUBLIC_DOMAIN 格式不正确，只填写域名，不要包含协议或路径" ;;
esac
case "$api_port" in *[!0-9]*|'') fail "SERVER_PORT 必须是端口数字" ;; esac

template=$project_dir/deploy/nginx-http.conf.template
if [ -n "$certificate" ] || [ -n "$certificate_key" ]; then
  [ -n "$certificate" ] && [ -n "$certificate_key" ] || fail "SSL_CERTIFICATE 和 SSL_CERTIFICATE_KEY 必须同时配置"
  as_root test -s "$certificate" || fail "证书不存在或为空：$certificate"
  as_root test -s "$certificate_key" || fail "证书私钥不存在或为空：$certificate_key"
  template=$project_dir/deploy/nginx-https.conf.template
  info "检测到证书，生成 HTTPS 配置"
else
  info "证书路径为空，生成临时 HTTP 配置；微信服务号和支付上线前必须启用 HTTPS"
fi

tmp_file=$(mktemp)
trap 'rm -f "$tmp_file"' EXIT HUP INT TERM
sed \
  -e "s|__PUBLIC_DOMAIN__|$(escape_sed "$domain")|g" \
  -e "s|__PROJECT_DIR__|$(escape_sed "$project_dir")|g" \
  -e "s|__API_PORT__|$(escape_sed "$api_port")|g" \
  -e "s|__PROXY_PARAMS__|/etc/nginx/lishuiwan-proxy-params.conf|g" \
  -e "s|__SSL_CERTIFICATE__|$(escape_sed "$certificate")|g" \
  -e "s|__SSL_CERTIFICATE_KEY__|$(escape_sed "$certificate_key")|g" \
  "$template" > "$tmp_file"

config_path=/etc/nginx/conf.d/lishuiwan.conf
previous_config=$(mktemp)
had_previous=false
if as_root test -f "$config_path"; then
  as_root cp "$config_path" "$previous_config"
  had_previous=true
fi
as_root install -m 0644 "$project_dir/deploy/nginx-proxy-params.conf" /etc/nginx/lishuiwan-proxy-params.conf
as_root install -m 0644 "$tmp_file" "$config_path"
if ! as_root nginx -t; then
  if [ "$had_previous" = true ]; then
    as_root install -m 0644 "$previous_config" "$config_path"
  else
    as_root rm -f "$config_path"
  fi
  as_root nginx -t >/dev/null 2>&1 || true
  rm -f "$previous_config"
  fail "Nginx 配置校验失败，已恢复原配置"
fi
rm -f "$previous_config"
as_root systemctl enable nginx >/dev/null
as_root systemctl reload nginx 2>/dev/null || as_root systemctl restart nginx
info "Nginx 配置已生效：$domain"

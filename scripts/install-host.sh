#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(dirname "$script_dir")
env_file=${APP_ENV_FILE:-$project_dir/.env}
node_version=${NODE_VERSION:-22.14.0}

info() { printf '%s\n' "[install] $*"; }
fail() { printf '%s\n' "[install] ERROR: $*" >&2; exit 1; }
env_value() {
  awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print; exit }' "$env_file"
}
as_root() {
  if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi
}
escape_sed() { printf '%s' "$1" | sed 's/[|&\\]/\\&/g'; }

[ -f "$env_file" ] || fail "缺少 $env_file，请先复制 .env.example 并填写配置"
command -v docker >/dev/null 2>&1 || fail "未安装 Docker；Docker 只用于 MySQL 和 Redis"
docker compose version >/dev/null 2>&1 || fail "未安装 Docker Compose v2"

java_ok=false
if command -v java >/dev/null 2>&1 && command -v javac >/dev/null 2>&1 \
    && java -version 2>&1 | grep -Eq 'version "17[.]|openjdk 17[.]' \
    && javac -version 2>&1 | grep -Eq '^javac 17[.]'; then
  java_ok=true
fi

maven_ok=false
if command -v mvn >/dev/null 2>&1; then
  detected_maven_version=$(mvn -version 2>/dev/null | awk 'NR == 1 { print $3 }')
  detected_maven_major=$(printf '%s' "$detected_maven_version" | awk -F. '{print $1}')
  detected_maven_minor=$(printf '%s' "$detected_maven_version" | awk -F. '{print $2}')
  case "$detected_maven_major.$detected_maven_minor" in
    .|*[!0-9.]*) ;;
    *)
      if [ "$detected_maven_major" -gt 3 ] || { [ "$detected_maven_major" -eq 3 ] && [ "$detected_maven_minor" -ge 6 ]; }; then
        maven_ok=true
      fi
      ;;
  esac
fi

nginx_ok=false
command -v nginx >/dev/null 2>&1 && nginx_ok=true
curl_ok=false
command -v curl >/dev/null 2>&1 && curl_ok=true
xz_ok=false
command -v xz >/dev/null 2>&1 && xz_ok=true

if [ "$java_ok" = true ] && [ "$maven_ok" = true ] && [ "$nginx_ok" = true ] \
    && [ "$curl_ok" = true ] && [ "$xz_ok" = true ]; then
  info "Java 17 JDK、Maven、Nginx、curl、xz 已安装，跳过系统软件安装"
else
  info "仅安装缺失或版本不符合要求的宿主机软件"
  if command -v apt-get >/dev/null 2>&1; then
    set --
    [ "$java_ok" = true ] || set -- "$@" openjdk-17-jdk-headless
    [ "$maven_ok" = true ] || set -- "$@" maven
    [ "$nginx_ok" = true ] || set -- "$@" nginx
    [ "$curl_ok" = true ] || set -- "$@" curl ca-certificates
    [ "$xz_ok" = true ] || set -- "$@" xz-utils
    as_root apt-get update
    as_root env DEBIAN_FRONTEND=noninteractive apt-get install -y "$@"
  elif command -v dnf >/dev/null 2>&1; then
    set --
    [ "$java_ok" = true ] || set -- "$@" java-17-openjdk-devel
    [ "$maven_ok" = true ] || set -- "$@" maven
    [ "$nginx_ok" = true ] || set -- "$@" nginx
    [ "$curl_ok" = true ] || set -- "$@" curl ca-certificates
    [ "$xz_ok" = true ] || set -- "$@" xz
    as_root dnf install -y "$@"
  elif command -v yum >/dev/null 2>&1; then
    set --
    [ "$java_ok" = true ] || set -- "$@" java-17-openjdk-devel
    [ "$maven_ok" = true ] || set -- "$@" maven
    [ "$nginx_ok" = true ] || set -- "$@" nginx
    [ "$curl_ok" = true ] || set -- "$@" curl ca-certificates
    [ "$xz_ok" = true ] || set -- "$@" xz
    as_root yum install -y "$@"
  else
    fail "不支持当前软件包管理器，请手动安装缺失的 Java 17 JDK、Maven、Nginx、curl 或 xz"
  fi
fi

java -version 2>&1 | grep -Eq 'version "17[.]|openjdk 17[.]' || fail "当前 java 不是 Java 17"
command -v mvn >/dev/null 2>&1 || fail "Maven 安装失败"
maven_version=$(mvn -version | awk 'NR == 1 { print $3 }')
maven_major=$(printf '%s' "$maven_version" | awk -F. '{print $1}')
maven_minor=$(printf '%s' "$maven_version" | awk -F. '{print $2}')
case "$maven_major.$maven_minor" in *[!0-9.]*) fail "无法识别 Maven 版本：$maven_version" ;; esac
if [ "$maven_major" -lt 3 ] || { [ "$maven_major" -eq 3 ] && [ "$maven_minor" -lt 6 ]; }; then
  fail "Maven $maven_version 版本过低，需要 Maven 3.6 或更高版本"
fi

node_ok=false
if command -v node >/dev/null 2>&1; then
  node_major=$(node -p 'Number(process.versions.node.split(".")[0])')
  node_minor=$(node -p 'Number(process.versions.node.split(".")[1])')
  if [ "$node_major" -gt 20 ] || { [ "$node_major" -eq 20 ] && [ "$node_minor" -ge 19 ]; }; then
    node_ok=true
  fi
fi

if [ "$node_ok" != true ]; then
  machine=$(uname -m)
  case "$machine" in
    x86_64) node_arch=x64 ;;
    aarch64|arm64) node_arch=arm64 ;;
    *) fail "暂不支持自动安装 Node.js 的 CPU 架构：$machine" ;;
  esac
  node_name=node-v${node_version}-linux-${node_arch}
  node_url=https://nodejs.org/dist/v${node_version}
  temp_dir=$(mktemp -d)
  trap 'rm -rf "$temp_dir"' EXIT HUP INT TERM
  info "从 Node.js 官方站点安装 Node.js $node_version"
  curl -fL --retry 3 -o "$temp_dir/$node_name.tar.xz" "$node_url/$node_name.tar.xz"
  curl -fL --retry 3 -o "$temp_dir/SHASUMS256.txt" "$node_url/SHASUMS256.txt"
  expected=$(awk -v file="$node_name.tar.xz" '$2 == file { print $1; exit }' "$temp_dir/SHASUMS256.txt")
  [ -n "$expected" ] || fail "Node.js 官方校验文件中没有找到 $node_name.tar.xz"
  actual=$(sha256sum "$temp_dir/$node_name.tar.xz" | awk '{print $1}')
  [ "$actual" = "$expected" ] || fail "Node.js 安装包 SHA-256 校验失败"
  as_root mkdir -p /opt
  as_root tar -xJf "$temp_dir/$node_name.tar.xz" -C /opt
  for binary in node npm npx corepack; do
    as_root ln -sfn "/opt/$node_name/bin/$binary" "/usr/local/bin/$binary"
  done
  rm -rf "$temp_dir"
  trap - EXIT HUP INT TERM
else
  info "Node.js $(node --version) 已满足要求，跳过 Node.js 安装"
fi

node -e 'const [a,b]=process.versions.node.split(".").map(Number);process.exit(a>20||(a===20&&b>=19)?0:1)' \
  || fail "Node.js 安装后版本仍不符合要求"
command -v npm >/dev/null 2>&1 && command -v npx >/dev/null 2>&1 || fail "npm 或 npx 不可用"

app_user=$(env_value APP_RUN_USER)
app_user=${app_user:-${SUDO_USER:-$(id -un)}}
[ "$app_user" != root ] || fail "APP_RUN_USER 禁止使用 root"
id "$app_user" >/dev/null 2>&1 || fail "APP_RUN_USER 对应的系统用户不存在：$app_user"
app_group=$(id -gn "$app_user")
java_bin=$(command -v java)

info "创建持久化目录并收紧敏感文件权限"
as_root mkdir -p "$project_dir/releases" "$project_dir/data/uploads" "$project_dir/backups" "$project_dir/secrets"
as_root chown -R "$app_user:$app_group" "$project_dir/data" "$project_dir/backups" "$project_dir/secrets"
as_root chmod 0750 "$project_dir/data" "$project_dir/data/uploads" "$project_dir/backups" "$project_dir/secrets"
as_root chown "$app_user:$app_group" "$env_file"
as_root chmod 0600 "$env_file"
if [ -e "$project_dir/secrets/apiclient_key.pem" ]; then
  as_root chown "$app_user:$app_group" "$project_dir/secrets/apiclient_key.pem"
  as_root chmod 0600 "$project_dir/secrets/apiclient_key.pem"
fi

info "安装 systemd 服务"
tmp_service=$(mktemp)
trap 'rm -f "$tmp_service"' EXIT HUP INT TERM
sed \
  -e "s|__APP_USER__|$(escape_sed "$app_user")|g" \
  -e "s|__APP_GROUP__|$(escape_sed "$app_group")|g" \
  -e "s|__PROJECT_DIR__|$(escape_sed "$project_dir")|g" \
  -e "s|__JAVA_BIN__|$(escape_sed "$java_bin")|g" \
  "$project_dir/deploy/lishuiwan-api.service.template" > "$tmp_service"
as_root install -m 0644 "$tmp_service" /etc/systemd/system/lishuiwan-api.service
as_root systemctl daemon-reload
as_root systemctl enable lishuiwan-api >/dev/null

APP_ENV_FILE="$env_file" sh "$project_dir/scripts/configure-nginx.sh"
info "宿主机初始化完成；现在运行 sh scripts/deploy.sh 发布应用"

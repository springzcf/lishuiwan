# 丽水湾会员管理系统

生产级单门店会员卡系统，包含微信原生小程序（顾客端 + 现场管理端）、移动 H5、Vue 3 Web 管理端、Spring Boot API、MySQL、Redis 与 Nginx。

## 目录

- `lishuiwan-api/`：Java 17 / Spring Boot 3.5 后端，Flyway 自动建库，微信支付 APIv3。
- `admin-web/`：Vue 3 / Element Plus 运营管理端。
- `h5/`：Vue 3 移动端会员服务，复用小程序业务接口，可作为浏览器入口。
- `miniprogram/`：微信原生小程序。
- `deploy/`、`scripts/`：宿主机 systemd/Nginx 配置与发布脚本；`docker-compose.yml` 仅运行 MySQL、Redis。
- `docs/`：需求与详细设计文档。

## 本地验证

完整的本地联调步骤见 [`docs/本地调试指南.md`](docs/本地调试指南.md)。本地模式支持开发微信账号、开发手机号、模拟支付、图片上传访问和 Vue 热更新，不需要真实微信支付即可跑通购买、发卡与核销主流程。

```bash
cd lishuiwan-api
mvn -s ../.mvn/settings.xml test

cd ../admin-web
pnpm install
pnpm build

cd ../h5
npm ci
npm run build
```

小程序首次导入微信开发者工具后，执行“工具 → 构建 npm”，然后将 `project.config.json` 的 `appid` 与 `env.js` 的 `apiBase` 改为正式值。

## 生产部署

云服务器首次部署、HTTPS 反向代理和一键更新步骤见 [`docs/云服务器一键部署.md`](docs/云服务器一键部署.md)。完成首次配置后，日常发布只需：

```bash
git pull --ff-only && sh scripts/deploy.sh
```

1. 准备已备案域名、HTTPS 证书、微信小程序及服务号 AppID/AppSecret、微信支付商户号、APIv3 密钥、商户证书序列号和 `apiclient_key.pem`。
2. `cp .env.example .env`，生成强随机密码并替换所有占位值。
3. 将商户私钥放到 `secrets/apiclient_key.pem`，权限设为仅部署用户可读。
4. 首次执行 `sudo sh scripts/install-host.sh`，安装 Java 17、Maven、Node.js、Nginx 和 systemd 服务；在 `.env` 填写证书路径。
5. 执行 `sh scripts/deploy.sh`；Docker 只启动 MySQL、Redis，API 由宿主机 systemd 运行，静态网页由宿主机 Nginx 提供。
6. 服务号“活动”和“我的”菜单分别使用 `/h5/activity`、`/h5/mine`；登录 `/admin/`，使用 `.env` 中的初始管理员账号配置商品、活动及现场角色；创建正式管理员后应删除初始密码环境变量并轮换密码。
7. 按 [`docs/服务号H5上线指南.md`](docs/服务号H5上线指南.md) 绑定服务号与小程序、配置网页授权域名、服务号菜单和支付授权目录。
8. 微信公众平台配置 request 合法域名和支付回调域名，均必须为 HTTPS；小程序 `env.js` 必须指向同一正式 API 域名。
9. 如启用订阅消息，在微信后台申请核销/发卡模板，将模板 ID 同时写入 `.env` 与小程序 `env.js`；模板中的事项、时间字段名分别通过 `WECHAT_TEMPLATE_THING_KEY`、`WECHAT_TEMPLATE_TIME_KEY` 配置。

生产配置会强制关闭开发登录和模拟支付；未提供安全的 JWT/数据库密码时，`prod` 启动会直接失败。微信参数会在首次调用微信能力时校验并返回明确错误。

## 微信云托管部署

后端目录已经按微信云托管 Spring Boot 模板适配，可直接将 `lishuiwan-api` 作为构建根目录发布：

1. 在微信云托管创建服务，代码目录选择 `lishuiwan-api`，构建方式选择 Dockerfile，监听端口填写 `80`。
2. 模板部署会读取 `container.config.json`，创建 `lishuiwan` 数据库；服务启动后 Flyway 会自动创建和升级业务表。
3. 云托管自动注入的 `MYSQL_ADDRESS`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 可直接被应用识别，也仍兼容原有的 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
4. 首次部署可使用云数据库密码派生 JWT 签名密钥；正式运营请单独配置至少 32 字节的 `JWT_SECRET`。
5. 使用微信登录前配置小程序 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`；启用服务号 H5 时还要配置 `WECHAT_OFFICIAL_*` 与 `H5_BASE_URL`。OAuth 一次性票据和订阅消息需要可访问的 Redis：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`。
6. 如需初始化后台管理员，配置 `INITIAL_ADMIN_USERNAME`、长度至少 12 位的 `INITIAL_ADMIN_PASSWORD` 和可选的 `INITIAL_ADMIN_NAME`。

手动上传代码包时，`container.config.json` 不会覆盖控制台已有的服务设置；请确认控制台端口仍为 `80`。容器本地文件不是持久存储，正式环境的上传文件应另接对象存储或持久卷。

## 备份与恢复

每日执行 `sh scripts/backup.sh /安全的备份目录`，默认保留 7 天。恢复前先停 API 写入，再执行：

```bash
gzip -dc backups/lishuiwan_YYYYMMDD_HHMMSS.sql.gz | docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" lishuiwan'
```

上线前至少完成一次备份恢复演练。数据库卷、备份目录、微信商户私钥应另外做主机级加密备份。

## 上线检查

- `.env`、`secrets/` 未进入版本库，JWT/数据库/Redis 密码均为独立强随机值。
- `MOCK_PAYMENT_ENABLED=false`、`DEV_LOGIN_ENABLED=false`。
- 微信支付回调可公网访问，回调签名、商户号、AppID、金额均由后端校验。
- 管理端只通过 HTTPS 访问，服务器防火墙不公开 MySQL、Redis 和宿主机 API 端口。
- 已创建监控告警、每日备份和恢复演练记录。

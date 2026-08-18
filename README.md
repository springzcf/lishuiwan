# 丽水湾会员管理系统

生产级单门店会员卡系统，包含微信原生小程序（顾客端 + 现场管理端）、Vue 3 Web 管理端、Spring Boot API、MySQL、Redis 与 Nginx。

## 目录

- `lishuiwan-api/`：Java 17 / Spring Boot 3.5 后端，Flyway 自动建库，微信支付 APIv3。
- `admin-web/`：Vue 3 / Element Plus 运营管理端。
- `miniprogram/`：微信原生小程序。
- `deploy/`、`docker-compose.yml`：生产部署配置。
- `docs/`：需求与详细设计文档。

## 本地验证

完整的本地联调步骤见 [`docs/本地调试指南.md`](docs/本地调试指南.md)。本地模式支持开发微信账号、开发手机号、模拟支付、图片上传访问和 Vue 热更新，不需要真实微信支付即可跑通购买、发卡与核销主流程。

```bash
cd lishuiwan-api
mvn -s ../.mvn/settings.xml test

cd ../admin-web
pnpm install
pnpm build
```

小程序首次导入微信开发者工具后，执行“工具 → 构建 npm”，然后将 `project.config.json` 的 `appid` 与 `env.js` 的 `apiBase` 改为正式值。

## 生产部署

1. 准备已备案域名、HTTPS 证书、微信小程序 AppID/AppSecret、微信支付商户号、APIv3 密钥、商户证书序列号和 `apiclient_key.pem`。
2. `cp .env.example .env`，生成强随机密码并替换所有占位值。
3. 将商户私钥放到 `secrets/apiclient_key.pem`，权限设为仅部署用户可读。
4. 配置 HTTPS：可在云负载均衡/CDN 终止 TLS；若由本机 Nginx 终止，参照 `deploy/nginx-ssl.conf.example` 挂载证书。
5. 执行 `docker compose up -d --build`，确认 `docker compose ps` 中 MySQL、Redis、API 均为 healthy。
6. 登录 `/admin/`，使用 `.env` 中的初始管理员账号配置商品、活动及小程序现场角色；创建正式管理员后应删除初始密码环境变量并轮换密码。
7. 微信公众平台配置 request 合法域名和支付回调域名，均必须为 HTTPS；小程序 `env.js` 必须指向同一正式 API 域名。
8. 如启用订阅消息，在微信后台申请核销/发卡模板，将模板 ID 同时写入 `.env` 与小程序 `env.js`；模板中的事项、时间字段名分别通过 `WECHAT_TEMPLATE_THING_KEY`、`WECHAT_TEMPLATE_TIME_KEY` 配置。

生产配置会强制关闭开发登录和模拟支付；未替换 JWT 密钥或缺少微信参数时，`prod` 启动会直接失败。

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
- 管理端只通过 HTTPS 访问，服务器防火墙不公开 MySQL、Redis、API 容器端口。
- 已创建监控告警、每日备份和恢复演练记录。

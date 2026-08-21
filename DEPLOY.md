# 「纸页之间」上线部署指南

本文档面向「纸页之间」博客（Spring Boot 3.2 + Vue 3 + MySQL 8）的生产环境部署。
按本文操作即可将项目从本地开发环境搬到一台云服务器上对外提供服务。

> 前提：已阅读 `README.md`，本地可以正常跑起来（后端 8080 + 前端 5173）。

---

## 0. 整体架构

最常见的低成本方案：**一台云服务器 + 一个域名**，前端静态文件与后端 API 都放在同一台服务器，由 nginx 统一对外服务：

```
用户浏览器 →  https://你的域名:443  (nginx)
   ├── /        → frontend/dist 静态文件（Vue 构建产物）
   └── /api/*   → 反向代理到 127.0.0.1:8080（Spring Boot）
MySQL 8（只监听本机 127.0.0.1，不对外暴露）
```

---

## 1. 准备服务器和域名

| 事项 | 说明 |
|---|---|
| 云服务器 | 阿里云 / 腾讯云「轻量应用服务器」，2核2G 起步即可，系统选 Ubuntu 22.04 或 Debian 12 |
| 域名 | 阿里云 / 腾讯云 / Namesilo 均可，一年几十元 |
| ICP 备案 | 服务器在国内 → 域名**必须备案**（约 1~2 周）；不想备案就买香港/海外服务器 |
| 安全组/防火墙 | 放行 80、443 端口；22（SSH）保留但不要对所有人开放 |
| DNS 解析 | 域名添加 A 记录，指向服务器公网 IP |

---

## 2. 安装服务器环境

```bash
# Ubuntu / Debian 示例
sudo apt update
sudo apt install -y openjdk-17-jdk mysql-server nginx
```

确认版本：

```bash
java -version      # 应为 17
mysql --version    # 应为 8.x
nginx -v
```

---

## 3. 初始化数据库

```sql
# 以 root 登录 MySQL 后执行（密码请换成强密码）
CREATE DATABASE IF NOT EXISTS paper_pages DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'paper'@'localhost' IDENTIFIED BY '换成强密码';
GRANT ALL PRIVILEGES ON paper_pages.* TO 'paper'@'localhost';
FLUSH PRIVILEGES;
```

> 建议使用专用账号 `paper`（仅限本机访问），不要用 root 跑应用。
> 表结构无需手工建——后端首次启动时 JPA 会自动建表并导入示例数据。

---

## 4. 本地打包并上传

在本地项目根目录执行：

```bash
# 后端 → 生成 backend/target/paper-pages-backend-1.0.0.jar
cd backend
mvn -DskipTests package

# 前端 → 生成 frontend/dist/ 静态文件
cd frontend
npm run build
```

把两个产物上传到服务器（后端建议放 `/opt/paper-pages/`）：

```bash
# 示例：scp 上传
scp backend/target/paper-pages-backend-1.0.0.jar  root@你的服务器IP:/opt/paper-pages/
scp -r frontend/dist                              root@你的服务器IP:/opt/paper-pages/dist
```

也可用宝塔面板 / FileZilla 等图形化工具上传。

---

## 5. 配置并启动后端（systemd 管理）

> ⚠️ **本项目 `application.yml` 中已无明文数据库密码**，上线时必须通过环境变量提供，
> 否则应用无法连接数据库。建议用 systemd 管理后端，实现开机自启 + 崩溃自动重启。

创建服务文件 `/etc/systemd/system/paper-pages.service`：

```ini
[Unit]
Description=Paper Pages Backend
After=network.target mysql.service

[Service]
User=www-data
WorkingDirectory=/opt/paper-pages
ExecStart=/usr/bin/java -jar /opt/paper-pages/paper-pages-backend-1.0.0.jar
Environment=DB_USERNAME=paper
Environment=DB_PASSWORD=你的数据库强密码
Environment=JWT_SECRET=用 openssl rand -hex 32 生成的随机串
Environment=ADMIN_USERNAME=admin
Environment=ADMIN_PASSWORD=你的后台强密码
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

生成随机 JWT 密钥：

```bash
openssl rand -hex 32
```

启动并验证：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now paper-pages
sudo systemctl status paper-pages          # 查看状态
curl http://localhost:8080/api/site        # 应返回 {"code":0,...}
```

查看日志：

```bash
journalctl -u paper-pages -f
```

首次启动会自动：建表 → 创建博主账号 → 导入示例文章与关于页文字（与本地首次运行行为一致）。

---

## 6. 配置 nginx + HTTPS

创建站点配置 `/etc/nginx/sites-available/paper-pages`：

```nginx
server {
    listen 80;
    server_name 你的域名.com;
    return 301 https://$host$request_uri;   # 强制跳转 HTTPS
}

server {
    listen 443 ssl;
    server_name 你的域名.com;

    # 证书（用 certbot 自动申请，见下）
    ssl_certificate     /etc/letsencrypt/live/你的域名.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/你的域名.com/privkey.pem;

    root /opt/paper-pages/dist;             # 前端构建产物
    index index.html;

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Vue Router（history 模式）回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

启用站点：

```bash
sudo ln -s /etc/nginx/sites-available/paper-pages /etc/nginx/sites-enabled/
sudo nginx -t && sudo nginx -s reload
```

申请免费 HTTPS 证书（Let's Encrypt，自动续期）：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d 你的域名.com
```

---

## 7. 上线验证与安全清单

浏览器打开 `https://你的域名.com`，逐一验证：

- [ ] 首页文章流、分页、筛选
- [ ] 站内搜索、标签页
- [ ] 文章详情（目录、阅读计数、点赞、相关推荐）
- [ ] 评论区发表评论（昵称/内容/30 秒限频）
- [ ] 后台登录 `/admin/login` → 仪表盘 / 文章管理 / 写文章 / 标签管理 / 站点设置

安全清单：

- [ ] `JWT_SECRET`、`DB_PASSWORD`、`ADMIN_PASSWORD` 全部通过环境变量注入（仓库内无明文密码）
- [ ] 后台管理员使用强密码（`ADMIN_PASSWORD` 设置强密码，或首次登录后尽快修改）
- [ ] MySQL 仅监听 `127.0.0.1`（勿在公网开放 3306 端口）
- [ ] 防火墙仅开放 22 / 80 / 443
- [ ] HTTPS 强制跳转已生效
- [ ] nginx 响应头增加安全头（可选加固）：

```nginx
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "SAMEORIGIN" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

> 评论已内置长度校验 + 30 秒限频；公开部署若担心刷屏，可后续在前端加验证码或接入第三方评论。

---

## 8. 日常更新流程

```bash
# 本地打包
cd backend && mvn -DskipTests package
cd frontend && npm run build

# 上传 jar 与 dist 后
sudo systemctl restart paper-pages      # 后端重启生效（前端替换 dist 即可，无需重启）
sudo nginx -t && sudo nginx -s reload   # 仅改动 nginx 配置时需要
```

---

## 9. 备选方案

| 方案 | 说明 |
|---|---|
| 宝塔面板 | 图形化安装环境 / 配置 nginx / 上传文件，适合不想敲命令；注意面板本身要设强密码、及时更新 |
| 前端白嫖 + 后端轻量 | 前端静态站放 Vercel / Cloudflare Pages（免费），后端放香港/新加坡轻量服务器；国内访问速度一般 |
| Docker 一键部署 | 编写 `docker-compose.yml`（mysql + backend + nginx 三容器），一条命令启动全部环境，环境一致性最好 |
| 海外 VPS | 不想 ICP 备案选这个，其余流程完全一致 |

---

## 10. 本项目部署要点速查

- 后端端口：`8080`（nginx 反代目标）
- 前端端口：开发 `5173`；生产为 `dist/` 静态文件
- 环境变量：`DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` / `ADMIN_USERNAME` / `ADMIN_PASSWORD`
- 数据库：`paper_pages`（utf8mb4），JPA 自动建表，无需手工导入 schema
- 生产构建产物：`backend/target/paper-pages-backend-1.0.0.jar`、`frontend/dist/`

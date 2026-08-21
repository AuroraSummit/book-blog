# 纸页之间 · 读书与观影手记

一个安静的个人博客：读书笔记 + 观影感受。前端 Vue 3，后端 Spring Boot + MySQL，前后端分离，访客可搜索、阅读、发表评论（博主审核），博主可登录后台管理文章与评论。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 · Vite · Vue Router · Pinia · Axios · ECharts · Vitest |
| 后端 | Spring Boot 3.2 · Spring Security · Spring Data JPA · JWT (jjwt) · Maven |
| 数据库 | MySQL 8（utf8mb4） |
| 认证 | 博主账号 + JWT（Bearer Token，7 天有效） |

## 目录结构

```
book-blog/
├── backend/                  # Spring Boot 后端（端口 8080）
│   ├── src/main/java/com/paperpages/
│   │   ├── config/           # DataInitializer（首次启动自动建博主/导文章/导关于）
│   │   ├── controller/       # REST 接口
│   │   ├── dto/              # 请求/响应模型（统一 Result 包装）
│   │   ├── entity/           # Article / Comment / AdminUser / Setting
│   │   ├── exception/        # 全局异常处理
│   │   ├── repository/       # JPA 数据访问
│   │   ├── security/         # JWT 工具 + Spring Security 配置
│   │   ├── service/          # 业务逻辑
│   │   └── util/             # JSON 工具
│   ├── src/main/resources/
│   │   ├── application.yml   # 数据源 / JWT / 博主账号配置
│   │   └── seed/             # 示例文章与关于文字（脚本自动生成）
│   └── schema.sql            # 参考建表脚本
├── frontend/                 # Vue 3 前端（开发端口 5173）
│   └── src/
│       ├── api/              # axios 封装（统一 /api、token、解包）
│       ├── assets/           # 纸感样式系统
│       ├── components/       # SiteHeader / ArticleItem / PaginationBar ...
│       ├── router/           # 前台 + 后台路由（登录守卫）
│       ├── stores/           # Pinia 登录态
│       ├── utils/            # 日期/高亮/正文解析（含单测）
│       └── views/            # 前台页面 + admin 后台页面
├── legacy/                   # 原纯静态版（归档，可独立打开预览）
└── scripts/extract-seed.mjs  # 从 legacy 数据重新生成种子 JSON
```

## 快速开始

### 0. 前置要求
- JDK 17+、Maven 3.6+
- Node.js 18+（本项目在 Node 26 验证）
- MySQL 8（本机已建 `paper_pages` 库则无需操作）

### 1. 初始化数据库（可选，JPA 会自动建表）
```sql
-- 使用 MySQL 客户端执行
CREATE DATABASE IF NOT EXISTS paper_pages DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 启动后端（默认 8080）
```bash
cd backend
mvn spring-boot:run
# 或打成 jar：mvn -DskipTests package && java -jar target/paper-pages-backend-1.0.0.jar
```
首次启动会自动：创建博主账号、导入 11 篇示例文章、导入关于页文字。

配置覆盖（环境变量，避免明文写库）：
| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `lwp20040411` | 数据库密码（请改成你自己的） |
| `JWT_SECRET` | 内置开发密钥 | 生产环境务必覆盖 |
| `ADMIN_USERNAME` | `admin` | 博主账号 |
| `ADMIN_PASSWORD` | `admin123` | 博主密码（首次启动加密入库，请尽快修改） |

### 3. 启动前端（开发模式，默认 5173）
```bash
cd frontend
npm install
npm run dev
```
浏览器打开 http://localhost:5173 ，`/api` 已代理到后端 8080。

### 4. 后台入口
- 地址：http://localhost:5173/admin/login
- 默认账号：`admin` / `admin123`

## 功能清单

**前台（访客可交互）**
- 首页文章流：分类筛选（全部/读书笔记/观影感受）+ 分页，筛选与页码同步到 URL，刷新/前进后退保持；「随便翻翻」随机一文
- 读书 / 影评分类页
- 站内全文检索：标题/摘要/标签/正文，关键词高亮（先转义再渲染，防 XSS）
- 标签体系：标签聚合页（/tags 标签云）+ 按标签浏览（/tag/:name）+ 文章条目/详情页标签直达
- 文章详情：轻量标记渲染（`## ` 小标题、`> ` 引用）、目录大纲、阅读时长估算、上一篇/下一篇、阅读计数（会话去重）
- 点赞：每篇可点「喜欢」，HttpOnly Cookie 标识访客去重（幂等），前台与后台均展示
- 相关阅读：按共享标签推荐 3 篇
- 评论区：访客即时发表（昵称/内容长度受限 + 同文 30 秒限频），发布后立即可见，无需审核
- 夜间模式：纸色 ↔ 深褐主题一键切换（localStorage 记忆）
- 关于页（后台可编辑文字）

**后台（博主，JWT 认证）**
- 登录 / 退出
- 仪表盘：文章/草稿/评论/点赞统计卡片 + 访客趋势折线 + 发表柱状 + 标签分布饼图 + 最受欢迎（阅读 TOP5）/ 点赞榜 TOP5
- 文章管理：全部/已发布/草稿筛选，发布、编辑、删除
- 写文章：新建/编辑（发布或存草稿），正文按行分段
- 标签管理：标签统计、重命名（同名自动合并）、删除（从所有文章移除）
- 站点设置：站名/副标题/公告/关于文字，保存后前台即时生效

## 主要 API

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/auth/login` | 登录，返回 JWT | 公开 |
| GET | `/api/auth/me` | 校验 token | 登录 |
| GET | `/api/articles?type=&tag=&page=&size=` | 文章分页列表（可按类型/标签过滤） | 公开 |
| GET | `/api/articles/search?q=` | 全文检索 | 公开 |
| GET | `/api/articles/random` | 随机一篇文章 | 公开 |
| GET | `/api/articles/{slug}` | 文章详情 | 公开 |
| GET | `/api/articles/{slug}/related?limit=` | 相关推荐 | 公开 |
| POST | `/api/articles/{slug}/views` | 阅读计数 +1 | 公开 |
| POST | `/api/articles/{slug}/like` | 点赞（cookie 去重，幂等） | 公开 |
| GET | `/api/articles/{slug}/liked` | 查询已赞状态与点赞数 | 公开 |
| GET | `/api/articles/{slug}/comments` | 评论列表（即时可见） | 公开 |
| POST | `/api/articles/{slug}/comments` | 发表评论（限频） | 公开 |
| GET | `/api/tags` | 标签云（含文章数） | 公开 |
| GET | `/api/site` | 站点公开信息（站名/副标题/公告） | 公开 |
| GET | `/api/about` | 关于文字 | 公开 |
| GET | `/api/admin/articles?status=` | 文章管理列表（含草稿） | 登录 |
| GET | `/api/admin/articles/{slug}` | 单篇详情（编辑回填） | 登录 |
| POST | `/api/admin/articles` | 新建文章 | 登录 |
| PUT | `/api/admin/articles/{slug}` | 编辑文章 | 登录 |
| PATCH | `/api/admin/articles/{slug}/status?status=` | 发布/转草稿 | 登录 |
| DELETE | `/api/admin/articles/{slug}` | 删除文章（连带评论与点赞） | 登录 |
| GET | `/api/admin/tags` | 标签管理列表 | 登录 |
| PUT | `/api/admin/tags/{name}` | 重命名标签（同名合并） | 登录 |
| DELETE | `/api/admin/tags/{name}` | 删除标签 | 登录 |
| GET | `/api/admin/settings` | 站点设置读取 | 登录 |
| PUT | `/api/admin/settings` | 站点设置更新 | 登录 |
| GET | `/api/admin/stats` | 仪表盘统计（含热门榜/标签分布） | 登录 |

统一响应：`{ "code": 0, "message": "ok", "data": ... }`，`code=0` 成功。

## 测试与构建

```bash
# 前端单元测试（日期/高亮/正文解析）
cd frontend && npm test

# 前端生产构建（输出 frontend/dist）
npm run build

# 后端编译
cd backend && mvn -DskipTests package
```

## 生产部署建议

- 修改 `application.yml` 中的数据库密码、JWT 密钥（或使用环境变量），并修改博主默认密码
- 前端 `npm run build` 产物可用任意静态服务器托管；若与后端同域部署，可去掉 vite 代理并把后端静态资源指向 `dist`（或使用 nginx 将 `/api` 反代到 8080、其余指向 `dist`）
- 上线前为 `dist` 配置 HTTPS，并在响应头增加 CSP（如 `default-src 'self'`）与 `X-Content-Type-Options: nosniff`
- 评论已做长度校验 + 同文 30 秒限频；公开部署若担心刷屏，可在前端加验证码或接入第三方评论

## 相对原静态版（legacy/）的改进

- ✅ 安全：修复原 `article.js` 上下篇标题 `innerHTML` 拼接导致的 XSS 隐患；全部文本经 Vue 插值/工具函数转义渲染
- ✅ 分页：修复原筛选切换不清除 `?page` 的问题；筛选与页码完整同步 URL
- ✅ 计数：阅读计数会话级去重，刷新不再重复 +1
- ✅ 架构：Vue3 组件化消除 5 个页面重复的页眉/页脚/导航；数据层换为真实 REST + MySQL，localStorage 演示逻辑移除
- ✅ 互动：评论改为即时发布（去除审核流）；新增文章点赞（Cookie 去重）、标签体系、相关推荐、随机一文
- ✅ 后台：评论审核替换为 站点设置 / 标签管理 / 热门统计（含标签分布图）

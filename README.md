# AI Platform — 商业文档自动化处理平台

基于 AI 的分布式文档识别与数据提取平台，支持文档上传 → OCR 识别 → 版面分析 → 表格提取 → LLM 字段抽取 → 校验反思 → 人工审核的全流程闭环。

---

## 一、架构总览

```
┌─ Vue 3 前端 (Port 3000) ────────────────────────────────┐
│  25 页面 / 16 API 模块 / Mock 可切换                      │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP /api/* (JWT Bearer Token)
                       ▼
┌─ Nginx (Port 80) ──────────────────────────────────────┐
│  /api/prompt/*         → Python:8000                    │
│  /api/finetune/*       → Python:8000                    │
│  /api/models/*         → Python:8000                    │
│  /api/ai/*             → Python:8000                    │
│  /api/*                → Java:8080                      │
└──────────────┬──────────────────────┬──────────────────┘
               │                      │
               ▼                      ▼
┌─ Java Spring Boot :8080 ──┐  ┌─ Python FastAPI :8000 ────────┐
│  业务 REST API              │  │  AI 推理 API                   │
│                             │  │                               │
│  • 认证/授权 (JWT+RBAC)     │  │  • 文档预处理 (OpenCV)          │
│  • 用户/角色/权限 CRUD      │  │  • OCR 识别 (PaddleOCR)         │
│  • 文档管理 (上传/列表)     │  │  • 版面分析 + 表格重建          │
│  • Pipeline 状态管理        │  │  • LangGraph Agent 编排        │
│  • 元数据/模板 CRUD         │  │  • LLM 调用 (DeepSeek/GLM)      │
│  • 审核工作台               │  │  • 提示词版本管理               │
│  • 操作日志 / Agent 追踪    │  │  • 自动化评测                   │
│  • 仪表盘聚合               │  │  • LoRA 微调                    │
│  • 文件存储代理 (MinIO)     │  │  • 模型资产管理                 │
│                             │  │                               │
│  ┌─ RocketMQ Producer ──┐  │  │  ┌─ RocketMQ Consumer ──┐     │
│  │ 发送 AI 处理请求 ──────> │  │  │ 接收 AI 处理请求      │     │
│  └──────────────────────┘  │  │  └──────────────────────┘     │
│  ┌─ RocketMQ Consumer ─┐  │  │  ┌─ RocketMQ Producer ──┐     │
│  │ 接收 AI 处理结果 ◄────── │  │  │ 发送 AI 处理结果      │     │
│  └──────────────────────┘  │  │  └──────────────────────┘     │
└────────────────────────────┘  └────────────────────────────────┘
               │                             │
               ▼                             ▼
┌─ PostgreSQL ──┐  ┌─ MinIO ──┐  ┌─ Redis ──┐  ┌─ RocketMQ ───────────┐
│  业务数据      │  │  文件存储  │  │  缓存     │  │  NameServer + Broker │
│  28 表         │  │  Bucket   │  │  Session  │  │  2 Topic:            │
│               │  │  documents│  │           │  │  pipeline-task       │
│               │  │          │  │           │  │  pipeline-result     │
└───────────────┘  └──────────┘  └──────────┘  └──────────────────────┘
```

### 请求路由

前端所有请求发往 **Nginx :80**，Nginx 根据路径前缀路由：

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/prompt/*` | Python :8000 | 提示词版本 + 评测 |
| `/api/finetune/*` | Python :8000 | 微调任务 |
| `/api/models/*` | Python :8000 | 模型资产 |
| `/api/ai/*` | Python :8000 | AI 管线（预处理/OCR/版面/表格） |
| `/api/*` | Java :8080 | 认证、用户、文档、Pipeline、审核等全部业务 |

### 异步通信

```
Java Producer ── pipeline-task ──→ RocketMQ ──→ Python Consumer
  (发送 AI 请求)                                  (执行 LangGraph 管线)
                                                      │
Java Consumer ◄── pipeline-result ◄── RocketMQ ◄── Python Producer
  (更新 Pipeline/     ↑                            (回写处理结果)
   Document 状态)     └── 双向 Topic ─────────────┘
```

---

## 二、技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| **前端** | Vue 3 (Composition API) + TypeScript + Element Plus + Pinia + Vue Router + ECharts + Axios + Vite 5 | — |
| **网关** | Nginx — 反向代理 + 路由分发 | — |
| **Java 后端** | Spring Boot 3.2 + MyBatis-Plus 3.5 + Spring Security + JWT + MinIO Client + RocketMQ Starter | JDK 21 |
| **Python AI** | FastAPI 0.115 + SQLAlchemy 2.0 async + PaddleOCR 2.10 + OpenCV 4.11 + LangGraph 0.2 + httpx + SiliconFlow | Python 3.11 |
| **基础设施** | PostgreSQL 16 + Redis 7 + MinIO + RocketMQ 5.3.1 | Docker |

---

## 三、快速启动

### 前提条件

- **Docker** & Docker Compose
- **Node.js** 18+
- **JDK** 21+
- **Python** 3.11+

### 1. 启动基础设施

```bash
cd ai-platform
docker compose up -d postgres redis minio minio-init rocketmq-namesrv rocketmq-broker
```

### 2. 初始化数据库

```bash
psql -U postgres -h localhost -d biz_form_recog_db -f init-db.sql
```

### 3. 启动 Java 后端

```bash
cd backend-java
mvn spring-boot:run
# → http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

### 4. 启动 Python AI 服务

```bash
cd backend-python
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
# → http://localhost:8000
# Swagger: http://localhost:8000/docs
```

### 5. 启动 Nginx 网关

```bash
# 开发环境：前端 Vite dev server 自带代理，可跳过 Nginx
# 生产/联调环境：
docker compose up -d nginx
# → http://localhost:80
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
# → http://localhost:3000
```

### 一键全栈

```bash
docker compose up -d
```

### 验证

| 服务 | 地址 | 凭证 |
|------|------|------|
| 前端 (Dev) | `http://localhost:3000` | — |
| Nginx 网关 | `http://localhost:80` | — |
| Java Swagger | `http://localhost:8080/swagger-ui.html` | — |
| Python Swagger | `http://localhost:8000/docs` | — |
| PostgreSQL | `localhost:5432` | `postgres / jinjin` |
| Redis | `localhost:6379` | 密码：`redis123` |
| MinIO Console | `http://localhost:9001` | `minioadmin / minioadmin123` |

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | 任意 6 位以上 | 普通用户 |
| `zhaoliu` | 任意 6 位以上 | 审计员 |

---

## 四、项目结构

```
ai-platform/
├── frontend/                          # Vue 3 前端
│   └── src/
│       ├── api/                       # 16 个 API 模块
│       ├── components/                # 5 个组件 (HeaderBar, Sidebar, FieldCompare, TraceGantt, TraceTimeline)
│       ├── composables/               # 3 个组合函数 (useAuth, usePermission, useTable)
│       ├── directives/                # 1 个指令 (v-permission)
│       ├── layout/                    # 1 个布局 (DefaultLayout)
│       ├── mock/                      # Mock 数据层 (DEV 自动启用)
│       ├── router/                    # 路由配置 + 导航守卫
│       ├── stores/                    # 3 个 Pinia store (user, app, permission)
│       ├── utils/                     # 2 个工具 (auth, request)
│       └── views/                     # 33 个 Vue 文件 (25 路由页面)
│
├── backend-java/                      # Spring Boot 业务后端
│   └── src/main/java/com/platform/
│       ├── controller/                # 16 个 Controller
│       ├── service/                   # 19 个 Service
│       ├── mapper/                    # 29 个 MyBatis-Plus Mapper
│       ├── entity/                    # 29 个实体 (6 个 package)
│       ├── dto/                       # 12 个 DTO
│       ├── config/                    # 6 个配置类 (Security, CORS, MinIO, AI Client, MyBatisPlus)
│       ├── security/                  # 3 个安全类 (JwtTokenProvider, JwtAuthFilter, UserDetailsService)
│       └── exception/                 # 3 个异常类 (BusinessException, ErrorCode, GlobalExceptionHandler)
│
├── backend-python/                    # FastAPI AI 服务
│   └── app/
│       ├── main.py                    # 应用入口 (含 lifespan RocketMQ 启停)
│       ├── config.py                  # pydantic-settings 配置
│       ├── database.py                # 异步 SQLAlchemy 引擎
│       ├── models/                    # 3 个模型文件 (11 个 ORM 类)
│       ├── schemas/                   # 1 个通用 schema (ApiResponse + PageResult)
│       ├── routers/                   # 4 个路由模块
│       │   ├── ai.py                  # AI 处理 (preprocess/ocr/layout/table/pipeline)
│       │   ├── prompt.py              # 提示词版本管理
│       │   ├── finetune.py            # 微调任务
│       │   └── models_router.py       # 模型资产管理
│       ├── services/                  # 14 个服务模块
│       │   ├── pipeline_service.py    # 7 阶段线性管线
│       │   ├── graph_service.py       # LangGraph 状态图管线
│       │   ├── graph_state.py         # PipelineState 类型定义
│       │   ├── preprocess_service.py  # OpenCV 去噪/纠偏
│       │   ├── ocr_service.py         # PaddleOCR 识别
│       │   ├── layout_service.py      # PPStructureV3 版面分析
│       │   ├── table_service.py       # 表格提取
│       │   ├── llm_service.py         # SiliconFlow GLM-5.2 调用
│       │   ├── prompt_service.py      # 提示词模板引擎
│       │   ├── validation_service.py  # LLM 字段校验
│       │   ├── reflection_service.py  # 低置信度反思纠正
│       │   ├── model_loader.py        # PPStructureV3 单例加载
│       │   ├── rocketmq_consumer.py   # RocketMQ 消费者 (接收 AI 请求)
│       │   └── rocketmq_producer.py   # RocketMQ 生产者 (回写处理结果)
│       └── prompts/                   # 4 个默认提示词模板
│
├── nginx/                             # Nginx 网关配置
│   └── nginx.conf                     # 反向代理路由规则
├── rocketmq/                          # RocketMQ 配置
│   └── broker.conf                    # Broker 配置 (autoCreateTopic)
├── docker-compose.yml                 # 全栈容器编排 (9 个服务)
├── init-db.sql                        # 28 表 DDL + 种子数据
├── CLAUDE.md                          # 开发者全栈文档
└── README.md                          # 本文件
```

---

## 五、核心功能

### 5.1 AI 文档处理流水线

```
上传文档 → Preprocess → Layout → OCR → Table → LLM Extract → Validate → Reflect
  │            │           │       │       │          │             │          │
  │         去噪/纠偏   版面区域   文字   表格     GLM-5.2      格式/逻辑    低置信度
  │                      检测    识别   提取    字段抽取        校验     二次纠正
  ▼
人工审核工作台 → 字段确认/驳回/修正 → 操作历史追溯
```

### 5.2 两种管线模式

| | 线性管线 | LangGraph 状态图 |
|---|---|---|
| **端点** | `POST /api/ai/pipeline/run` | `POST /api/ai/pipeline/graph` |
| **引擎** | pipeline_service.py | graph_service.py |
| **流程** | 7 阶段顺序执行 | route → extract → validate ⇄ reflect → done |
| **validate↔reflect** | 固定 1 次 | 条件循环，最多 2 轮 |
| **适用** | 简单文档 | 复杂文档，需多轮纠正 |

### 5.3 异步消息架构

```
POST /api/documents/{id}/pipeline  (Java Controller)
  │
  ├─ 更新 doc.status = "queued"
  └─ 返回 {taskId, status: "queued"}   ← 立即返回 <100ms
        │
        ▼
Java RocketMQ Producer  ── pipeline-task ──→  RocketMQ Broker
                                                  │
                                                  ▼
                                         Python RocketMQ Consumer
                                           │
                                           ├─ 下载文件 (MinIO)
                                           ├─ LangGraph 管线处理
                                           └─ 结果写 DB
                                                  │
                                                  ▼
                                         Python RocketMQ Producer
                                           │
                                    pipeline-result
                                           │
                                           ▼
Java RocketMQ Consumer  ◄────────────────────┘
  │
  ├─ 更新 pipeline.status = "completed" | "failed"
  └─ 更新 document.status
```

### 5.4 审核工作台

- 字段对比视图：OCR 原文 vs AI 提取值，一键确认/驳回/修正
- 快捷键：Enter 确认 / Esc 跳过
- 批量操作：多字段批量确认/驳回
- 操作历史：完整审核链路追溯

---

## 六、API 端点

### Nginx 路由规则

```
/api/auth/*            → Java:8080    (认证)
/api/users/*           → Java:8080    (用户管理)
/api/roles/*           → Java:8080    (角色管理)
/api/permissions/*     → Java:8080    (权限管理)
/api/documents/*       → Java:8080    (文档管理)
/api/pipeline/*        → Java:8080    (Pipeline 状态)
/api/metadata/*        → Java:8080    (元数据)
/api/templates/*       → Java:8080    (模板)
/api/review/*          → Java:8080    (审核工作台)
/api/operation-logs/*  → Java:8080    (操作日志)
/api/agent-traces/*    → Java:8080    (Agent 追踪)
/api/dashboard/*       → Java:8080    (仪表盘)
/api/settings/*        → Java:8080    (系统设置)
/api/ai/*              → Python:8000  (AI 处理)
/api/prompt/*          → Python:8000  (提示词)
/api/finetune/*        → Python:8000  (微调)
/api/models/*          → Python:8000  (模型资产)
```

### Java 业务 API (:8080)

| 方法 | 端点 | 说明 |
|------|------|------|
| `POST` | `/api/auth/login` | 登录 (JWT) |
| `GET/POST` | `/api/users` | 用户管理 (8 端点) |
| `GET/POST` | `/api/roles` | 角色管理 (8 端点) |
| `GET` | `/api/permissions/tree` | 权限树 |
| `POST` | `/api/documents/upload` | 文件上传 (MinIO) |
| `POST` | `/api/documents/{id}/preprocess` | 同步预处理 |
| `POST` | `/api/documents/{id}/pipeline` | **异步提交管线** (RocketMQ) |
| `GET` | `/api/pipeline` | 管线列表（状态查询） |
| `GET` | `/api/pipeline/{id}` | 管线详情（提取/校验/反思） |
| `POST` | `/api/pipeline/run` | 同步运行管线 |
| `POST` | `/api/pipeline/run-async` | **异步运行管线** (RocketMQ) |
| `GET/POST` | `/api/review` | 审核任务 |
| `POST` | `/api/review/{id}/fields/{fid}/approve` | 确认字段 |
| `POST` | `/api/review/{id}/fields/{fid}/correct` | 修正字段 |
| `GET/PUT` | `/api/settings/{category}` | 系统设置 |
| `GET` | `/api/operation-logs/export` | CSV 导出日志 |
| `GET` | `/api/dashboard/stats` | 仪表盘统计 |

### Python AI API (:8000)

| 方法 | 端点 | 说明 |
|------|------|------|
| `POST` | `/api/ai/preprocess` | 图像预处理 |
| `POST` | `/api/ai/ocr` | OCR 识别 |
| `POST` | `/api/ai/layout` | 版面分析 |
| `POST` | `/api/ai/table` | 表格提取 |
| `POST` | `/api/ai/pipeline/run` | 7 阶段线性管线 |
| `POST` | `/api/ai/pipeline/graph` | LangGraph 状态图管线 |
| `GET/POST` | `/api/prompt/versions` | 提示词版本管理 |
| `POST` | `/api/prompt/versions/{id}/rollback` | 回滚版本 (带 diff) |
| `GET` | `/api/prompt/versions/diff` | 版本对比 |
| `POST` | `/api/prompt/evaluation/run` | 评测运行 |
| `GET/POST` | `/api/finetune/tasks` | 微调任务 |
| `GET/POST` | `/api/models` | 模型资产管理 |

---

## 七、配置说明

### 端口分配

| 服务 | 端口 |
|------|------|
| Nginx 网关 | 80 |
| 前端 Vite (Dev) | 3000 |
| Java 后端 | 8080 |
| Python AI | 8000 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| MinIO API | 9000 |
| MinIO Console | 9001 |
| RocketMQ NameServer | 9876 |
| RocketMQ Broker | 10909 / 10911 |

### 环境变量

**Java 后端** (`application-dev.yml`)：
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/biz_form_recog_db
spring.datasource.username: postgres
spring.datasource.password: jinjin
rocketmq.name-server: localhost:9876
ai.python.base-url: http://localhost:8000
minio.endpoint: http://localhost:9000
minio.access-key: minioadmin
minio.secret-key: minioadmin123
```

**Python AI** (`.env`)：
```env
DB_HOST=localhost
DB_USER=postgres
DB_PASSWORD=jinjin
DB_NAME=biz_form_recog_db
SILICONFLOW_API_KEY=sk-xxxxxxxx
SILICONFLOW_MODEL=zai-org/GLM-5.2
# MinIO (RocketMQ consumer 下载文件用)
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET=documents
# RocketMQ
ROCKETMQ_NAMESRV=localhost:9876
ROCKETMQ_CONSUMER_GROUP=python-consumer-group
ROCKETMQ_CONSUMER_TOPIC=pipeline-task
ROCKETMQ_PRODUCER_GROUP=python-producer-group
ROCKETMQ_RESULT_TOPIC=pipeline-result
```

---

## 八、权限体系

38 项权限码，三层控制（路由守卫 + DOM 指令 + 菜单过滤）：

```
document:upload/delete    pipeline:list    review:list/create
metadata:list/export      prompt:version/evaluation
finetune:list             model:list       user:list/create/delete
role:list                 log:operation/agent    settings:view
```

---

## 九、开发历史

| Phase | 内容 | 关键成果 |
|-------|------|----------|
| 1–9 | 基础架构 | 16 Controller, 28 表, 前端 16 API 模块, 98% 测试通过 |
| 10 | 基础设施 | MinIO 对接, PaddleOCR, Docker Compose 全栈 |
| 11 | 对接 | Java↔Python AI 全流程打通, Pipeline 7 阶段 |
| 12 | AI 完善 | 提示词系统, Validation, Reflection, 单例模型 |
| 13 | 前端 | 4 视图去桩, 8 URL 修复, 11 函数补齐 |
| 14 | 工程化 | 7 Service, Settings API, 4 端点去桩 |
| 15 | 异步 | LangGraph 状态图, RocketMQ 单向 (Java→Java→Python) |
| 16 | 架构对齐 | Nginx 网关, Python↔Java 双向 MQ, Vite 代理分流, 架构图完全对齐 |

---

## 十、已知限制

| 限制 | 影响 | 解决方案 |
|------|------|----------|
| PaddlePaddle 3.0 Windows CPU ONEDNN | OCR/layout/table segfault | `FLAGS_use_onednn=0` 或 Linux Docker |
| RocketMQ Python 客户端 | `rocketmq-client-python==2.0.0` 需编译 C++ 扩展 | Docker 环境自动解决；Windows 需 WSL |

---

## 十一、文档索引

- **[CLAUDE.md](CLAUDE.md)** — 开发者全栈文档（架构、配置、Phase 详情、权限 38 项速查）
- **[全栈开发计划.md](全栈开发计划.md)** — 项目总体规划
- **[数据库设计文档.md](数据库设计文档.md)** — 28 表 DDL 与关系
- **[summary-t4.md](summary-t4.md)** — Phase 9 集成测试报告 (48 端点 97.9%)

# AI Platform — 全栈开发文档

> 最后更新: 2026-07-04 | Phase 9 集成测试通过 | 15 Controller ~100 端点

---

## 一、项目概述

商业文档自动化处理平台，三端架构：

| 层 | 技术栈 | 端口 | 状态 |
|----|--------|------|------|
| **前端** | Vue 3 + TypeScript + Element Plus + Vite 5 | 3000 | ✅ 完成 |
| **Java 后端** | Spring Boot 3.2.7 + MyBatis-Plus 3.5.7 + PostgreSQL | 8080 | ✅ 15 Controller / ~100 端点 |
| **Python AI** | FastAPI + PaddleOCR + OpenCV + 硅基流动 GLM-5.2 | 8000 | ⚠️ 框架完成，PaddleOCR 待安装 |

---

## 二、启动命令

```bash
# 数据库
psql -U postgres -h localhost -d biz_form_recog_db

# Java 后端 (:8080)
cd backend-java
mvn spring-boot:run

# Python AI 服务 (:8000)
cd backend-python
uvicorn app.main:app --reload --port 8000

# 前端 (:3000)
cd frontend
npm run dev         # Mock 默认开启
npm run build       # 生产构建
```

### 一键启动（全栈）
```bash
# 终端 1
cd backend-java && mvn spring-boot:run

# 终端 2（可选，待 PaddleOCR 安装）
cd backend-python && uvicorn app.main:app --reload --port 8000

# 终端 3
cd frontend && npm run dev
```

---

## 三、关键配置

### 数据库
```
Host: localhost:5432
User: postgres
Password: jinjin
DB: biz_form_recog_db
连接: psql -U postgres -h localhost -d biz_form_recog_db
```

### Redis
```
Host: localhost:6379
Password: (无)
```

### 默认账号（真实后端）
| 用户 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 超级管理员 |

### 默认账号（前端 Mock 层）
| 用户名 | 角色 | 权限 |
|--------|------|------|
| `admin` | 超级管理员 (admin) | 所有权限 |
| `zhangsan` | 普通用户 (user) | 基本操作权限 |
| `zhaoliu` | 审计员 (auditor) | 查看日志和链路 |

- Mock 层密码：**任意 6 位以上字符**
- `wangwu`（审计员）状态为禁用

---

## 四、前端（frontend/）

### 技术栈
- **Vue 3** (Composition API + `<script setup>`)
- **TypeScript**
- **Element Plus** 2.7 + `@element-plus/icons-vue`
- **Pinia** 状态管理
- **Vue Router** 4（createWebHistory 模式）
- **ECharts 5** + `vue-echarts` 图表
- **Axios** HTTP 请求
- **Sass** 样式
- **Vite 5** 构建工具

### 项目结构
```
frontend/
├── src/
│   ├── api/                  # API 接口层 (14 个文件)
│   │   ├── agent-trace.ts    # Agent 链路追踪
│   │   ├── auth.ts           # 登录/登出/刷新 token/获取当前用户
│   │   ├── document.ts       # 文档管理（上传/列表/详情/预处理/版面分析/OCR/表格）
│   │   ├── finetune.ts       # 微调任务
│   │   ├── metadata.ts       # 元数据（字段 CRUD/分组/导出/Webhook）
│   │   ├── model.ts          # 模型资产管理 + 绑定
│   │   ├── operation-log.ts  # 操作日志
│   │   ├── permission.ts     # 权限管理
│   │   ├── pipeline.ts       # 流水线（列表/详情/提取/验证/反思）
│   │   ├── prompt.ts         # 提示词版本 + 自动化评测
│   │   ├── review.ts         # 审核工作台（字段确认/驳回/修正/批量操作）
│   │   ├── role.ts           # 角色管理
│   │   ├── template.ts       # 模板配置
│   │   └── user.ts           # 用户管理
│   ├── components/
│   │   ├── layout/
│   │   │   ├── HeaderBar.vue   # 顶部栏（用户信息、登出）
│   │   │   └── Sidebar.vue     # 侧边栏导航（动态权限菜单、v-permission 过滤）
│   │   ├── review/
│   │   │   └── FieldCompare.vue # 审核字段对比（Enter=确认 / Esc=跳过）
│   │   └── trace/
│   │       ├── TraceGantt.vue    # Agent 链路甘特图
│   │       └── TraceTimeline.vue # Agent 链路时间线/树形视图
│   ├── composables/           # Vue 组合函数
│   │   ├── useAuth.ts         # 认证相关（登录状态、权限判断、登出）
│   │   ├── usePermission.ts   # 权限判断
│   │   └── useTable.ts        # 通用表格逻辑（分页、搜索、刷新、重置）
│   ├── directives/
│   │   └── permission.ts      # v-permission 指令（无权限时移除 DOM）
│   ├── layout/
│   │   └── DefaultLayout.vue  # 默认布局（侧边栏 + 顶栏 + 内容区）
│   ├── mock/                  # Mock 数据层（仅在 DEV 模式自动启用）
│   │   ├── index.ts           # Axios adapter 拦截器，覆盖所有 14 个模块的 API
│   │   └── data.ts            # 所有 Mock 数据
│   ├── router/
│   │   └── index.ts           # 路由配置 + 导航守卫（token 检查 + 权限验证）
│   ├── stores/
│   │   ├── app.ts             # 应用状态（侧边栏折叠、主题、语言）
│   │   ├── permission.ts      # 权限状态
│   │   └── user.ts            # 用户状态（token、用户信息、角色、权限）
│   └── views/
│       ├── dashboard/         # 仪表盘（ECharts 图表）
│       ├── document/          # 文档管理
│       ├── finetune/          # 微调任务
│       ├── login/             # 登录页面
│       ├── log/               # Agent 链路追踪 + 操作日志
│       ├── metadata/          # 元数据管理
│       ├── models/            # 模型资产
│       ├── pipeline/          # 处理流水线
│       ├── prompt/            # 提示词工程
│       ├── review/            # 审核工作台
│       ├── settings/          # 系统设置
│       ├── system/            # 用户/角色/权限管理 + 403/404
│       └── template/          # 模板配置
├── .env.development           # VITE_API_BASE_URL=/api
├── .env.production
└── vite.config.ts             # @ 别名、端口 3000、API 代理到 :8080、代码分包
```

### Mock 层说明
- `main.ts` 中 `import.meta.env.DEV` 时自动调用 `setupMock()`
- 替换 Axios `defaults.adapter` 拦截所有请求返回假数据
- 覆盖全部 14 个模块，未匹配路由 fall through 到真实 API
- 网络延迟：100-300ms 随机
- 启动时控制台打印: `[Mock] Mock API layer enabled — 默认账号: admin / 任意6位以上密码`

### 路由与权限（三层控制）
1. **路由守卫** — 无 token 重定向 `/login`，无权限跳转 `/403`
2. **v-permission 指令** — 无权限时从 DOM 移除元素
3. **Sidebar 菜单过滤** — `hasXxxAccess` computed 控制整组显隐

admin 角色拥有所有权限（`roles.includes('admin')` 跳过权限检查）。

---

## 五、Java 后端（backend-java/）

### Controller 全貌（15 个，~100 端点）

| Controller | 路径 | 端点数 | 状态 |
|------------|------|--------|------|
| AuthController | `/api/auth/*` | 4 | ✅ |
| DashboardController | `/api/dashboard/*` | 5 | ✅ 真实数据 |
| UserController | `/api/users/*` | 8 | ✅ PATCH 修复 |
| RoleController | `/api/roles/*` | 8 | ✅ |
| PermissionController | `/api/permissions/*` | 7 | ✅ |
| DocumentController | `/api/documents/*` | 8 | ✅ +4 子资源 |
| PipelineController | `/api/pipeline/*` | 2 | ✅ 字段名对齐 |
| MetadataController | `/api/metadata/*` | 12 | ✅ |
| TemplateController | `/api/templates/*` | 5 | ✅ |
| ReviewController | `/api/review/*` | 8 | ✅ 重构 |
| OperationLogController | `/api/operation-logs/*` | 4 | ✅ |
| AgentTraceController | `/api/agent-traces/*` | 2 | ✅ |
| PromptController | `/api/prompt/*` | 8 | 🆕 Phase 5 |
| FinetuneController | `/api/finetune/*` | 4 | 🆕 Phase 5 |
| ModelController | `/api/models/*` | 9 | 🆕 Phase 5 |

### API 响应格式
```ts
{ code: 200 | 0, data: T, message: string }
```
Axios 响应拦截器检查 `code !== 0 && code !== 200` 时走错误处理。

---

## 六、Python AI 服务（backend-python/）

### 技术栈
- FastAPI + uvicorn
- SQLAlchemy 2.0 async + asyncpg
- PaddleOCR + OpenCV
- 硅基流动 `zai-org/GLM-5.2` LLM API (OpenAI 兼容)
- pydantic-settings 环境配置

### 端点
- `/api/ai/*` — 文档 AI 处理（OCR / 提取 / 校验 / 反思）
- `/api/prompt/*` — 提示词管理
- `/api/finetune/*` — 微调任务
- `/api/models/*` — 模型资产

### 目录结构
```
backend-python/
├── app/
│   ├── main.py           # FastAPI 入口
│   ├── config.py         # pydantic-settings 配置
│   ├── database.py       # SQLAlchemy Base + engine
│   ├── models/           # 11 个数据模型（统一 Base）
│   ├── routers/          # 路由模块
│   └── services/         # 业务逻辑
├── prompts/              # LLM 提示词模板
│   ├── field_extraction_default.txt
│   ├── validation_default.txt
│   ├── ocr_correction_default.txt
│   └── table_extraction_default.txt
├── .env                  # 环境变量（不提交）
├── Dockerfile            # python:3.11-slim
└── requirements.txt
```

### 关键修复记录
- SQLAlchemy Base 已统一（3 个 model 文件从 `database.Base` 导入）
- 硬编码密钥已移除（从 `.env` / 环境变量读取）
- Dockerfile 已创建（含 OpenCV 系统依赖）

---

## 七、权限码速查（完整 38 项）

```
# 文档
document:upload     document:list     document:create     document:update     document:delete

# 流水线
pipeline:list       pipeline:create   pipeline:update     pipeline:delete

# 元数据
metadata:list       metadata:update   metadata:export

# 模板
template:list       template:create   template:update     template:delete

# 审核
review:list         review:create     review:update       review:delete

# 提示词
prompt:version      prompt:evaluation

# 微调
finetune:list       finetune:create   finetune:update     finetune:delete

# 模型
model:list          model:create      model:update        model:delete

# 用户
user:list           user:create       user:update         user:delete

# 角色
role:list           role:create       role:update         role:delete

# 权限
permission:list     permission:create permission:update   permission:delete

# 日志 + 设置
log:operation       log:agent         settings:view
```

---

## 八、Phase 1-9 变更汇总

### Phase 1：API 契约对齐（6 项修复）
| 修复 | 文件 | 内容 |
|------|------|------|
| Document 搜索参数 | `DocumentController.java` + `DocumentService.java` | `fileName` → `keyword` |
| User API HTTP 方法 | `UserController.java` | `@PutMapping` → `@PatchMapping` |
| 权限前缀移除 | `AuthService.java` + 12 Controller（57 处） | `ROLE_xxx` → `xxx` |
| Pipeline 字段名 | `PipelineController.java` | `startedAt`→`startTime`, `finishedAt`→`endTime`, `errorMessage`→`error`, 新增 `documentName` |
| Review 补字段 | `ReviewController.java` | 注入 `DocumentMapper` + `UserMapper`，批量查 `documentName`/`reviewer` |

### Phase 2：Review 端点重构
- **文件**: `ReviewController.java`（完全重写）
- 移除 `PUT /{id}/fields/{fieldId}` + `PUT /{id}/batch`
- 新增 6 个端点：`approve`, `reject`, `correct`, `batch-approve`, `batch-reject`, `GET /{id}/fields`
- 修复 null operatorId：注入 `JwtTokenProvider` + `HttpServletRequest`，从 JWT 提取当前用户

### Phase 3：Document 子资源
- **文件**: `DocumentController.java`
- 新增：`GET /{id}/layout`, `GET /{id}/ocr`, `GET /{id}/table`, `POST /{id}/preprocess`（桩）

### Phase 4：Dashboard 真实数据
- **文件**: `DashboardController.java`（完全重写）
- 注入 6 Mapper，全部 5 端点从桩代码改为真实 DB 查询

### Phase 5：新增 3 个 AI 域 Controller
- `PromptController.java`（8 端点）— 提示词版本 + 评测
- `FinetuneController.java`（4 端点）— 微调任务 CRUD
- `ModelController.java`（9 端点）— 模型资产 + 绑定

### Phase 6：@TableLogic 一致性
- 8/8 有 `deletedAt` 字段的实体均已有 `@TableLogic`，无需修改。

### Phase 7-8：Python AI 服务
- 统一 SQLAlchemy Base（4 文件）
- 移除硬编码密钥（`config.py`）
- 创建 `.env` + `Dockerfile`
- 清理 `requirements.txt`（移除 `diff-match-patch`/`python-magic`）
- 统一 `docker-compose.yml` DB 凭证
- 创建 4 个默认提示词模板（`prompts/*.txt`）

### Phase 9：端到端集成测试（2026-07-04）
- **48 端点测试，47 通过（97.9%）**
- 1 个 404 为预期行为（preprocess 无文档可处理）
- 详情：[[summary-t4.md]]

---

## 九、开发模式速查

### 新增模块标准步骤
1. 定义 API（`src/api/<module>.ts`）— 接口类型 + 请求函数
2. 添加 Mock 数据（`src/mock/data.ts` + `src/mock/index.ts`）
3. 创建视图（`src/views/<module>/`）— 使用 `useTable<T>` composable
4. 注册路由（`src/router/index.ts`）— 设置 `meta.permission`
5. 添加导航（`src/components/layout/Sidebar.vue`）— 配置菜单项 + 权限过滤

### useTable composable
```ts
const { data, loading, pagination, query, fetchData, handleSearch, handleReset, handlePageChange, handleSizeChange } = useTable<T>({
  fetchApi: getXxx,
  defaultQuery: { status: '' }
})
```

### 常用图标注意事项
- Element Plus 图标必须从 `@element-plus/icons-vue` 导入实际存在的名称
- `Receipt` 不存在，需用 `Tickets`/`Document` 等替代

---

## 十、待完成

| 项 | 优先级 | 说明 |
|----|--------|------|
| MinIO 启动 | **高** | 文档上传依赖 |
| PaddleOCR 安装 | 高 | `pip install -r requirements.txt` |
| 前端 Mock 关闭测试 | 中 | `VITE_USE_MOCK=false` 全栈联调 |
| 硅基流动 API Key 轮换 | 中 | 密钥已暴露于 git 历史 |
| Docker Compose 全栈编排 | 中 | 含 Python AI 服务容器 |
| LangGraph 多智能体 | 🔜 | Route → Extract → Validate → Reflect |
| RocketMQ 异步通信 | 🔜 | 替代同步 HTTP |

---

## 十一、文档索引

- [[summary-t1.md]] — 第一次会话总结
- [[summary-t2.md]] — Phase 1 收尾 + Phase 2 启动 (2026-07-03)
- [[summary-t3.md]] — Phase 2 完成 + API 契约对齐 (2026-07-03)
- [[summary-t4.md]] — Phase 9 端到端集成测试报告 (2026-07-04)
- [[sum-backend-p01.md]] — Python AI 服务架构文档
- [[sum-backend-j01.md]] — Java 后端架构文档
- [[全栈开发计划.md]] — 总架构
- [[数据库设计文档.md]] — 28 表 DDL

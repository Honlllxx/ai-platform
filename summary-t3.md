# Phase 2 完成 + API 契约对齐 — 会话总结

> 日期: 2026-07-03 | 上级文档: [[全栈开发计划.md]] [[summary-t2.md]] [[sum-backend-p01.md]]

## 一、背景

本次会话基于三份全栈探索报告，系统性地修复了前端期望 API 与 Java 后端实际暴露 API 之间的不匹配问题，补齐了 3 个完全缺失的 Controller，并完成了 Python AI 服务的关键架构修复。

**核心发现：** 前端 Mock 层覆盖了 14 个模块的全部端点，但 Java 后端缺少 3 个 Controller、Review 端点 URL 不一致、Pipeline 字段名不匹配、Dashboard 全桩代码。关闭 Mock 层会导致大量 404。

---

## 二、Phase 1：API 契约对齐（6 项修复）

### 2.1 Document 搜索参数
- **文件**: `DocumentController.java` + `DocumentService.java`
- **修复**: 参数 `fileName` → `keyword`，匹配前端请求

### 2.2 User 状态 HTTP 方法
- **文件**: `UserController.java`
- **修复**: `@PutMapping("/{id}/status")` → `@PatchMapping("/{id}/status")`

### 2.3 权限前缀统一（移除 ROLE_）
- **文件**: `AuthService.java` + 全部 12 个 Controller（57 处注解）
- **问题**: 后端 `hasAuthority('ROLE_review:list')` vs 前端裸码 `review:list`
- **修复**:
  - `AuthService.getAuthorities()`: `"ROLE_" + p.getCode()` → `p.getCode()`
  - 移除 `login/refreshToken` 中的 `.replace("ROLE_", "")`（不再需要）
  - 全局替换所有 `@PreAuthorize` 注解中的 `ROLE_` 前缀

### 2.4 Pipeline 响应字段名
- **文件**: `PipelineController.java`
- **修复**:
  - `startedAt` → `startTime`，`finishedAt` → `endTime`，`errorMessage` → `error`
  - 新增 `documentName` — 注入 `DocumentMapper` 批量查文档名

### 2.5 Review 列表补字段
- **文件**: `ReviewController.java`
- **修复**: 注入 `DocumentMapper` + `UserMapper`，批量查 `documentName` 和 `reviewer`

---

## 三、Phase 2：Review 端点重构

### 3.1 替换为 action-specific POST 端点
- **文件**: `ReviewController.java`（完全重写）
- **新增端点**:
  | 方法 | 路径 | 说明 |
  |------|------|------|
  | POST | `/api/review/{id}/fields/{fieldId}/approve` | 确认字段 |
  | POST | `/api/review/{id}/fields/{fieldId}/reject` | 驳回字段（可选 `comment`） |
  | POST | `/api/review/{id}/fields/{fieldId}/correct` | 修正字段（`correctedValue`） |
  | POST | `/api/review/{id}/batch-approve` | 批量确认 |
  | POST | `/api/review/{id}/batch-reject` | 批量驳回 |
  | GET | `/api/review/{id}/fields` | 独立获取字段列表 |

### 3.2 修复 operatorId
- **修复**: 注入 `JwtTokenProvider` + `HttpServletRequest`，从 JWT token 提取当前用户 ID
- 提取为 `private` 方法 `getCurrentUserId()` / `handleFieldAction()`

---

## 四、Phase 3：Document 子资源端点

- **文件**: `DocumentController.java`
- **新增端点**:
  | 方法 | 路径 | 说明 |
  |------|------|------|
  | GET | `/api/documents/{id}/layout` | 版面分析结果（查 `document_layouts`） |
  | GET | `/api/documents/{id}/ocr` | OCR 结果（查 `document_ocr`） |
  | GET | `/api/documents/{id}/table` | 表格提取结果（查 `document_tables`） |
  | POST | `/api/documents/{id}/preprocess` | 提交预处理（当前为桩，待对接 Python） |

- 新增注入: `DocumentLayoutMapper`, `DocumentOcrMapper`, `DocumentTableMapper`, `DocumentMapper`

---

## 五、Phase 4：Dashboard 真实数据

- **文件**: `DashboardController.java`（完全重写）
- **注入**: 6 个 Mapper（User/OperationLog/AgentTrace/Review/Pipeline/Document）
- **修复**:
  | 端点 | 原状态 | 新实现 |
  |------|--------|--------|
  | `/stats` | 全 0 | `COUNT` 用户/操作/Agent + `AVG` 处理时间 + 自动通过率 |
  | `/daily-processing` | 空数组 | 近 7 天按日聚合文档+审核数 |
  | `/doc-type-distribution` | 空数组 | 按 `file_type` 聚合 + 中文标签 |
  | `/operation-trend` | 空数组 | 近 7 天按日聚合操作日志 |
  | `/operation-type-distribution` | 空数组 | 按 `operation_type` 聚合 |

---

## 六、Phase 5：新增 3 个 AI 域 Controller

实体和 Mapper 已存在，本次新建 Controller：

### 6.1 PromptController（新建）
- **文件**: `PromptController.java`
- **端点** (8 个):
  - Prompt 版本: `GET/POST/PUT` 列表+CRUD，`POST /rollback`（桩），`GET /diff`（桩）
  - 评测: `GET /evaluation` 列表，`POST /evaluation/run`（桩）

### 6.2 FinetuneController（新建）
- **文件**: `FinetuneController.java`
- **端点** (4 个): `GET/POST/DELETE /tasks`，`GET /tasks/{id}`

### 6.3 ModelController（新建）
- **文件**: `ModelController.java`
- **端点** (9 个):
  - 模型资产: `GET/POST/PUT/DELETE` CRUD
  - 模型绑定: `GET /bindings/list`, `POST /bindings`, `DELETE /bindings/{id}`, `POST /bindings/{id}/activate`

---

## 七、Phase 6：工程完善

### 7.1 @TableLogic 检查
- 全部 8 个有 `deletedAt` 字段的实体（User/Role/Document/MetadataField/Template/Review/ModelAsset/ModelBinding）均已有 `@TableLogic`，无需修改。

---

## 八、Phase 7：Python AI 服务关键修复

### 8.1 统一 SQLAlchemy Base
- **文件**: `database.py` + `models/business.py` + `models/document.py` + `models/pipeline.py`
- **问题**: 每个 model 文件各自创建 `declarative_base()`，导致跨模型关联和查询无法工作
- **修复**: 在 `database.py` 创建单一 `Base = declarative_base()`，3 个 model 文件统一导入

### 8.2 移除硬编码密钥
- **文件**: `app/config.py`
- **修复**: `db_password` 和 `siliconflow_api_key` 默认值改为空字符串，仅从环境变量读取
- **注意**: API key 已暴露于代码历史中，建议到硅基流动后台轮换

### 8.3 创建 .env 文件
- **新建**: `backend-python/.env` — 包含所有实际连接配置

### 8.4 清理依赖
- **文件**: `requirements.txt`
- **移除**: `diff-match-patch`, `python-magic`（未被任何代码引用）

### 8.5 创建 Dockerfile
- **新建**: `backend-python/Dockerfile`
- 基于 `python:3.11-slim`，包含 OpenCV 系统依赖（libgl1, libglib2 等）

### 8.6 统一 docker-compose 凭证
- **文件**: `docker-compose.yml`
- **修复**: PostgreSQL 默认值 `platform/platform123/platform_db` → `postgres/jinjin/biz_form_recog_db`

---

## 九、Phase 8：Python AI 补充完善

### 9.1 默认提示词模板
- **新建**:
  - `prompts/field_extraction_default.txt` — 字段提取提示词
  - `prompts/validation_default.txt` — 数据校验提示词
  - `prompts/ocr_correction_default.txt` — OCR 纠错提示词
  - `prompts/table_extraction_default.txt` — 表格提取提示词

---

## 十、Java 后端 Controller 全貌（15 个）

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
| **PromptController** | `/api/prompt/*` | **8** | 🆕 |
| **FinetuneController** | `/api/finetune/*` | **4** | 🆕 |
| **ModelController** | `/api/models/*` | **9** | 🆕 |

**总端点: ~100 个**

---

## 十一、权限码速查（更新）

```
# 基础模块
user:list / user:create / user:update / user:delete
role:list / role:create / role:update / role:delete
permission:list / permission:create / permission:update / permission:delete

# 业务模块
document:upload / document:list
pipeline:list
metadata:list / metadata:update / metadata:export
template:list
review:list

# AI 模块（新增）
prompt:version / prompt:evaluation
finetune:list
model:list

# 日志
log:operation / log:agent
```

---

## 十二、文件变更统计

### 修改文件（15 个 Java + 5 个 Python）
| 类型 | 文件 | 变更 |
|------|------|------|
| Controller | `DocumentController.java` | 参数名 + 6 个新端点 |
| Controller | `UserController.java` | PUT → PATCH |
| Controller | `PipelineController.java` | 字段名对齐 + documentName |
| Controller | `ReviewController.java` | 完全重写（6 端点重构） |
| Controller | `DashboardController.java` | 完全重写（真实查询） |
| Controller | 全部 12 Controller | 57 处 ROLE_ 移除 |
| Service | `AuthService.java` | ROLE_ 生成逻辑移除 |
| Service | `DocumentService.java` | 参数名 fileName → keyword |
| Config | `database.py` | 添加共享 Base |
| Config | `config.py` | 移除硬编码密钥 |
| Models | `business/document/pipeline.py` | 统一 Base 导入 |
| Config | `requirements.txt` | 移除 2 个未使用依赖 |
| Config | `docker-compose.yml` | DB 凭证对齐 |

### 新建文件（10 个）
| 文件 | 说明 |
|------|------|
| `PromptController.java` | 提示词版本 + 评测 (8 端点) |
| `FinetuneController.java` | 微调任务 CRUD (4 端点) |
| `ModelController.java` | 模型资产 + 绑定 (9 端点) |
| `backend-python/.env` | Python 服务环境变量 |
| `backend-python/Dockerfile` | Python 容器化 |
| `prompts/field_extraction_default.txt` | 默认字段提取模板 |
| `prompts/validation_default.txt` | 默认校验模板 |
| `prompts/ocr_correction_default.txt` | 默认 OCR 纠错模板 |
| `prompts/table_extraction_default.txt` | 默认表格提取模板 |
| `summary-t3.md` | 本文档 |

---

## 十三、验证结果

| 检查项 | 状态 |
|--------|------|
| Java `mvn compile` | ✅ 全量通过（15 Controller, ~100 端点） |
| Python 语法检查 | ✅ 全部通过 |
| Python FastAPI 导入 | ✅ app 成功加载 |
| SQLAlchemy Base 统一 | ✅ 11 模型共享同一 Base |
| @TableLogic 一致性 | ✅ 8/8 实体正确配置 |

---

## 十四、待完成

| 项 | 优先级 | 说明 |
|----|--------|------|
| Phase 9 端到端集成测试 | 高 | 启动全栈，关闭 Mock，逐页验证 |
| PaddleOCR + OpenCV 安装 | 中 | `pip install -r requirements.txt` |
| MinIO 启动 | 中 | Docker 或直接安装，文档上传依赖 |
| 前端对接测试 | 中 | `VITE_USE_MOCK=false` 全面验证 |
| 硅基流动 API Key 轮换 | 中 | 密钥已暴露于代码历史 |
| 其他用户密码设置 | 低 | zhangsan / zhaoliu / lisi 等 |
| Phase 3: LangGraph 多智能体 | 🔜 | Route/Extract/Validate/Reflect |
| Phase 3: RocketMQ 集成 | 🔜 | 异步通信替代同步 HTTP |
| Docker Compose 全栈编排 | 🔜 | 含 Python AI 服务容器 |

---

## 十五、启动命令

```bash
# 后端 (Java Spring Boot → :8080)
cd backend-java && mvn spring-boot:run

# AI 服务 (Python FastAPI → :8000)
cd backend-python && uvicorn app.main:app --reload --port 8000

# 前端 (Vue 3 → :3000)
cd frontend && npm run dev

# 数据库
psql -U postgres -h localhost -d biz_form_recog_db
```

# Phase 1 收尾 + Phase 2 启动 — 会话总结

> 日期: 2026-07-03 | 上级文档: [[全栈开发计划.md]] [[Phase1-Java后端实施计划.md]]

## 一、Phase 1 问题修复（Java 后端）

### 1.1 TIMESTAMPTZ 类型转换
- **问题**: PostgreSQL `TIMESTAMPTZ` → JDBC 返回 `OffsetDateTime` → MyBatis-Plus 映射 `LocalDateTime` 失败
- **修复**: 创建 `handler/LocalDateTimeTypeHandler.java`，全局注册到 `MyBatisPlusConfig`
- **依赖**: 通过 `@EventListener(ApplicationReadyEvent.class)` 延迟获取 `SqlSessionFactory`，避免循环依赖

### 1.2 Redis 密码
- **问题**: Java 配置 `redis123`，但本地 Redis 无密码 → `ERR Client sent AUTH, but no password is set`
- **修复**: `application-dev.yml` 中去掉 `password: redis123`

### 1.3 BCrypt 密码哈希
- **问题**: `init-db.sql` 中密码哈希是占位符（`$2a$10$N.zmdr9k7uOCQb376NoUnu...`），无法登录
- **修复**: 用 Python bcrypt 5.0.0 生成 `$2a$` 前缀的真实哈希 → 密码 `admin123`

### 1.4 缺失的 Controller
| 文件 | 状态 |
|------|------|
| `PipelineController.java` | ✅ 新建 |
| `ReviewController.java` | ✅ 新建 |

### 1.5 缺失的端点
| 端点 | 修复 |
|------|------|
| `GET /api/roles/all` | 新增（避免 "all" 被 `/{id}` 拦截） |
| `GET /api/permissions/tree` | 新增（避免 "tree" 被 `/{id}` 拦截） |
| `GET /api/metadata/groups` | 新增别名 |
| `GET/PUT /api/metadata/export/webhook` | 新增 Webhook 配置端点 |
| `GET /api/operation-logs/export` | 新增导出端点 |
| `GET /api/operation-logs/types` | 新增操作类型枚举 |
| `POST /api/users/{userId}/roles` | 新增用户角色分配 |
| `POST /api/roles/{roleId}/permissions` | 新增角色权限分配 |

### 1.6 Service 方法补充
- `UserService.updateUserRoles()` — 删除旧角色 + 插入新角色
- `RoleService.updateRolePermissions()` — 删除旧权限 + 插入新权限

## 二、数据库配置

```
Host: localhost:5432
Database: biz_form_recog_db
User: postgres
Password: jinjin
```

### 登录凭据

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |
| zhangsan | （待设置） | 普通用户 |
| zhaoliu | （待设置） | 审计员 |

## 三、Java 后端 Controller 全貌（12 个）

| Controller | 路径 | 端点数 |
|------------|------|--------|
| AuthController | `/api/auth/*` | 4 |
| DashboardController | `/api/dashboard/*` | 5 |
| UserController | `/api/users/*` | 8 |
| RoleController | `/api/roles/*` | 8 |
| PermissionController | `/api/permissions/*` | 7 |
| DocumentController | `/api/documents/*` | 4 |
| PipelineController | `/api/pipeline/*` | 5 |
| MetadataController | `/api/metadata/*` | 12 |
| TemplateController | `/api/templates/*` | 5 |
| ReviewController | `/api/review/*` | 5 |
| OperationLogController | `/api/operation-logs/*` | 4 |
| AgentTraceController | `/api/agent-traces/*` | 3 |

## 四、Phase 2：Python AI 服务

### 4.1 项目结构（backend-python/）
```
backend-python/
├── app/
│   ├── main.py                    # FastAPI 入口 (端口 8000)
│   ├── config.py                  # 配置 (DB/LLM/OCR)
│   ├── database.py                # SQLAlchemy async engine
│   ├── models/                    # 只读 ORM (document, pipeline, business)
│   ├── schemas/common.py          # ApiResponse<T> 对齐 Java
│   ├── routers/
│   │   ├── ai.py                  # /api/ai/* (preprocess/ocr/layout/table/pipeline)
│   │   ├── prompt.py              # /api/prompt/versions/*
│   │   ├── finetune.py            # /api/finetune/tasks/*
│   │   └── models_router.py       # /api/models/*
│   └── services/
│       ├── llm_service.py         # 硅基流动 GLM-5.2 (OpenAI 兼容)
│       ├── preprocess_service.py  # OpenCV 矫正 + 去印章
│       ├── ocr_service.py         # PaddleOCR 封装
│       ├── layout_service.py      # PPStructure 版面分析
│       ├── table_service.py       # 表格检测 + 结构重建
│       └── pipeline_service.py    # 5 阶段流水线编排
├── uploads/
├── requirements.txt
└── Dockerfile
```

### 4.2 LLM 配置
```
Provider: 硅基流动 (SiliconFlow)
Model: zai-org/GLM-5.2
API Key: sk-ummudopdvwvyqqjgitacmvzonjsyfhaemtjqdnskldejevcf
Base URL: https://api.siliconflow.cn/v1
```

### 4.3 服务状态
- ✅ `GET /api/health` → `{"status":"ok","service":"python-ai"}`
- ✅ Swagger UI: http://localhost:8000/docs
- ⚠️ PaddleOCR + OpenCV 待安装（`pip install paddleocr paddlepaddle opencv-python-headless`）

## 五、待完成

| 项 | 优先级 |
|----|--------|
| MinIO 启动（文档上传依赖） | 高 |
| 其他用户密码设置 | 中 |
| PaddleOCR 安装 + AI 端点测试 | 中 |
| Java 前端对接测试 | 中 |
| Docker Compose 全栈编排 | 低 |
| Phase 3: LangGraph 多智能体 | 低 |

## 六、启动命令

```bash
# 后端
cd backend-java && mvn spring-boot:run     # :8080

# AI 服务
cd backend-python && uvicorn app.main:app --reload --port 8000  # :8000

# 前端
cd frontend && npm run dev                  # :3000
```

# Phase 1：Java 后端基础 + Docker 基础设施 — 实施计划

## 目标

Java Spring Boot 跑起来，DB 初始化，JWT 认证，非 AI 模块 CRUD 可用。前端切到真实 API。

## 交付清单

- Spring Boot 3 项目脚手架（pom.xml、启动类、分层结构）
- Docker Compose：PostgreSQL + Redis + MinIO + RocketMQ (NameServer + Broker)
- MyBatis-Plus 实体 + Mapper（28 张表）+ 数据库迁移脚本
- 种子数据（admin 用户、3 角色、38 权限）
- Spring Security + JWT 过滤器（access + refresh token）
- `@PreAuthorize` RBAC 权限控制
- AOP 操作日志切面（自动记录请求参数/响应/IP/耗时）
- CRUD Controller：Auth、Users、Roles、Permissions、Documents（仅上传/列表）、Metadata、Templates、Operation Logs、Agent Traces（桩）
- 统一响应格式 `{ code, data, message }` + 分页 `{ records, total }`
- 前端 Mock 层关闭 `/api/*` 请求 → 走真实后端
- 验证：登录、CRUD 用户/角色/权限、上传文档到 MinIO

**暂不实现**：RocketMQ 消息通信、Pipeline 执行、OCR、审核

---

## 准备工作

- [ ] **环境确认**：JDK 17+、Maven 3.8+、Docker Desktop、Node 18+
- [ ] **项目根目录**：`D:\table_project\project\ai-platform\`

---

## Step 1: Docker Compose 基础设施

**目标**：一键启动 PostgreSQL、Redis、MinIO、RocketMQ（NameServer + Broker）

**产出文件**：
- `docker-compose.yml` — 项目根目录
- `.env` — 环境变量（数据库密码、MinIO 密钥等）

**服务清单**：

| 服务 | 镜像 | 端口 |
|------|------|------|
| postgres | `postgres:16-alpine` | 5432 |
| redis | `redis:7-alpine` | 6379 |
| minio | `minio/minio:latest` | 9000 (API), 9001 (Console) |
| rocketmq-namesrv | `apache/rocketmq:5.3.1` | 9876 |
| rocketmq-broker | `apache/rocketmq:5.3.1` | 10909, 10911 |

**验证**：
```bash
docker compose up -d
docker compose ps                    # 5 个容器均 healthy
psql -h localhost -U platform -d platform_db  # 可连接
```

---

## Step 2: Spring Boot 3 项目脚手架

**目标**：可启动的 Spring Boot 应用

**产出目录**：`backend-java/`

**主要产出文件**：
```
backend-java/
├── pom.xml                          # Spring Boot 3.2 + Java 17
│   # 依赖：spring-boot-starter-web, spring-boot-starter-security,
│   # mybatis-plus-spring-boot3-starter, postgresql, minio (8.5+),
│   # spring-boot-starter-data-redis, rocketmq-spring-boot-starter (2.3+),
│   # jjwt-api (0.12+), lombok, validation, springdoc-openapi
├── src/main/java/com/platform/
│   └── PlatformApplication.java     # @SpringBootApplication
├── src/main/resources/
│   ├── application.yml              # 基础配置 (server.port=8080)
│   └── application-dev.yml          # 开发环境 (DB/Redis/MinIO/RocketMQ 连接信息)
└── Dockerfile
```

**关键 pom.xml 依赖版本**：
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.7</version>
</parent>
<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <jjwt.version>0.12.6</jjwt.version>
    <minio.version>8.5.10</minio.version>
</properties>
```

**注意**：RocketMQ 依赖只需引入，Phase 1 不配置 Consumer/Producer，避免启动报错。

**验证**：`mvn spring-boot:run` → 端口 8080 启动成功，无报错

---

## Step 3: 数据库初始化

**目标**：28 张表已建好，种子数据已写入，MyBatis-Plus 可映射

### 3a. 执行 DDL + 种子数据
- 直接使用已生成的 `init-db.sql`
- 方式：`docker exec -i postgres psql -U platform -d platform_db < init-db.sql`
- 或配置 `spring.sql.init.mode=always` 自动执行（开发环境）

### 3b. 生成 Entity 类（28 张表对应 28 个 Entity）

按模块分包：
```
entity/
├── system/          # User.java, Role.java, Permission.java, UserRole.java, RolePermission.java
├── document/        # Document.java, DocumentLayout.java, DocumentOcr.java, DocumentTable.java
├── pipeline/        # Pipeline.java, ExtractionResult.java, ValidationResult.java, ReflectionResult.java
├── business/        # MetadataField.java, Template.java, Review.java, ReviewField.java, ReviewHistory.java
├── ai/              # PromptVersion.java, Evaluation.java, FinetuneTask.java, ModelAsset.java, ModelBinding.java
├── log/             # OperationLog.java, AgentTrace.java, AgentTraceNode.java
└── common/          # ExportHistory.java, WebhookConfig.java
```

每个 Entity 必须包含：
- `@TableName` 注解（表名映射）
- `@TableId(type = IdType.AUTO)` 主键策略
- `@TableField(fill = FieldFill.INSERT)` / `FieldFill.INSERT_UPDATE` 自动填充（created_at/updated_at）
- `@TableLogic` 软删除（deleted_at 字段）

### 3c. 生成 Mapper 接口（MyBatis-Plus BaseMapper）
```
mapper/
├── UserMapper.java, RoleMapper.java, PermissionMapper.java, ...
└── (共 28 个 Mapper)
```

### 3d. MyBatis-Plus 配置
- `MyBatisPlusConfig.java` — 分页插件 `PaginationInnerInterceptor`
- `MetaObjectHandler` — created_at / updated_at 自动填充
- `application-dev.yml` — 打印 SQL 日志 & 自动建表策略设为 none

**验证**：编写一个简单测试 → `userMapper.selectById(1)` 返回 admin 用户

---

## Step 4: 认证系统（JWT + Spring Security）

**产出文件**：
```
config/
├── SecurityConfig.java            # SecurityFilterChain + 白名单
├── CorsConfig.java                 # 允许 localhost:3000 跨域
└── PasswordEncoderConfig.java      # BCryptPasswordEncoder bean

security/
├── JwtTokenProvider.java          # Token 生成 / 验证 / 解析
├── JwtAuthenticationFilter.java   # OncePerRequestFilter
├── UserDetailsServiceImpl.java    # implements UserDetailsService → 查 users 表
└── PermissionEvaluator.java       # @PreAuthorize("hasAuthority('user:list')")

dto/
├── LoginRequest.java              # { username, password }
├── LoginResponse.java             # { accessToken, refreshToken, expiresIn, userInfo }
├── RefreshTokenRequest.java       # { refreshToken }
└── ChangePasswordRequest.java     # { oldPassword, newPassword }
```

**SecurityConfig 白名单**：
```java
// 无需认证的路径
"/api/auth/login",
"/api/auth/refresh-token",
"/actuator/health",
"/v3/api-docs/**",
"/swagger-ui/**"
```

**JWT 设计**：
- accessToken：有效期 2 小时，payload = `{ sub: userId, username, authorities: [...] }`
- refreshToken：有效期 7 天，存 Redis `refresh_token:{userId}` → token 值
- Token 格式：`Authorization: Bearer <token>`

**AuthController 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 → 返回 tokens + 用户信息 |
| POST | `/api/auth/refresh-token` | 刷新 accessToken |
| POST | `/api/auth/logout` | 登出 → 删除 Redis 中 refreshToken |
| GET | `/api/auth/current-user` | 获取当前用户信息 (含角色/权限列表) |

**RBAC 权限模型**：
1. `UserDetailsServiceImpl.loadUserByUsername()` → 查 users + JOIN user_roles → roles + JOIN role_permissions → permissions
2. 将 `permissions.code` 放入 `GrantedAuthority` 集合（如 `ROLE_user:list`）
3. Controller 用 `@PreAuthorize("hasAuthority('user:list')")` 保护端点
4. admin 角色特殊处理：`roles.contains("admin")` → 拥有所有权限（在 PermissionEvaluator 中判断）

**验证**：
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# → { code: 200, data: { accessToken, refreshToken, userInfo } }
```

---

## Step 5: 统一响应 & 基础配置

**产出文件**：
```
dto/
└── ApiResponse.java               # 泛型响应 { code, data, message, timestamp }

config/
└── GlobalExceptionHandler.java    # @RestControllerAdvice

exception/
├── BusinessException.java         # 业务异常 (code + message)
└── ErrorCode.java                 # 错误码枚举
```

**ApiResponse 格式**（与前端约定完全对齐）：
```json
// 成功
{ "code": 200, "data": T, "message": "success" }
// 分页
{ "code": 200, "data": { "records": [...], "total": 100, "page": 1, "pageSize": 10 }, "message": "success" }
// 失败
{ "code": 400, "data": null, "message": "用户名已存在" }
```

**全局异常处理覆盖**：
- `MethodArgumentNotValidException` → 400 参数校验失败
- `BindException` → 400 参数绑定失败
- `AccessDeniedException` → 403 无权限
- `AuthenticationException` → 401 未认证
- `BusinessException` → 自定义业务异常
- `Exception` → 500 兜底

**分页工具**：
```java
// MyBatis-Plus Page → 统一分页响应
public static <T> ApiResponse<PageResult<T>> page(Page<T> page) {
    return ApiResponse.success(new PageResult<>(
        page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()
    ));
}
```

**验证**：调用不存在的端点 → 返回统一格式的 404，而非 Tomcat 默认页面

---

## Step 6: RBAC CRUD 控制器

### 6a. UserController (`/api/users/*`)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/users` | `user:list` | 分页列表 (支持 username/status 搜索) |
| GET | `/api/users/{id}` | `user:list` | 用户详情 (含角色信息) |
| POST | `/api/users` | `user:create` | 创建用户 (密码 BCrypt) |
| PUT | `/api/users/{id}` | `user:update` | 编辑用户 (含角色分配 → user_roles 更新) |
| DELETE | `/api/users/{id}` | `user:delete` | 软删除用户 |
| PUT | `/api/users/{id}/status` | `user:update` | 启用/禁用 |
| PUT | `/api/users/{id}/reset-password` | `user:update` | 重置密码 |

### 6b. RoleController (`/api/roles/*`)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/roles` | `role:list` | 全量列表 (不分页) |
| GET | `/api/roles/{id}` | `role:list` | 角色详情 (含 permissionIds) |
| POST | `/api/roles` | `role:create` | 创建角色 + 分配权限 |
| PUT | `/api/roles/{id}` | `role:update` | 编辑角色 + 更新权限关联 |
| DELETE | `/api/roles/{id}` | `role:delete` | 软删除 (检查是否有用户关联) |
| PUT | `/api/roles/{id}/status` | `role:update` | 启用/禁用 |

### 6c. PermissionController (`/api/permissions/*`)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/permissions` | `permission:list` | 树形结构 (递归组装 children) |
| GET | `/api/permissions/flat` | `permission:list` | 平铺列表 |
| POST | `/api/permissions` | `permission:create` | 创建权限 |
| PUT | `/api/permissions/{id}` | `permission:update` | 编辑权限 |
| DELETE | `/api/permissions/{id}` | `permission:delete` | 删除权限 (检查子节点) |

**Service 层注意事项**：
- UserService.createUser()：`@Transactional` — 插入 users + user_roles
- UserService.updateUser()：`@Transactional` — 更新 users + 删除旧 user_roles + 插入新 user_roles
- RoleService：同理处理 role_permissions 关联
- PermissionService：删除前检查 parent_id 引用

**验证**：用前端页面登录 → 用户 CRUD → 角色 CRUD → 权限 CRUD 全部走通

---

## Step 7: 文档管理（上传 + MinIO）

**产出文件**：
```
config/
└── MinioConfig.java               # MinioClient bean

controller/
└── DocumentController.java        # /api/documents/*

service/
└── DocumentService.java           # 上传逻辑 + MinIO 交互

dto/
├── DocumentUploadResponse.java    # { id, fileName, fileUrl, fileSize, status }
└── DocumentQueryRequest.java      # { fileName, fileType, status, page, pageSize }
```

**端点**：

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/documents` | `document:list` | 分页列表 (支持 fileName/fileType/status 搜索) |
| GET | `/api/documents/{id}` | `document:list` | 文档详情 |
| POST | `/api/documents/upload` | `document:upload` | 上传文档 (MultipartFile) |
| DELETE | `/api/documents/{id}` | `document:list` | 软删除 (同时删 MinIO 文件) |

**上传流程**：
```
1. 接收 MultipartFile
2. 校验：文件类型 (pdf/jpg/png/word)、大小 (≤20MB)
3. 生成 objectKey = "documents/{yyyyMMdd}/{uuid}.{ext}"
4. MinioClient.putObject(bucket, objectKey, inputStream, contentType)
5. 写入 documents 表 (file_url = MinIO URL, status = 'pending')
6. 返回 DocumentUploadResponse
```

**MinIO 配置**：
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: documents
```

**注意**：Phase 1 只做上传+入库，不发 RocketMQ 消息，不触发 AI 处理。

**验证**：用前端文档上传页 → 上传 PDF → MinIO Console 查看文件存在 → 文档列表可见

---

## Step 8: 元数据 & 模板 CRUD

### 8a. MetadataController (`/api/metadata/*`)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/metadata/fields` | `metadata:list` | 分页列表 (支持 group_code/status 搜索) |
| GET | `/api/metadata/fields/{id}` | `metadata:list` | 字段详情 |
| POST | `/api/metadata/fields` | `metadata:list` | 创建字段 |
| PUT | `/api/metadata/fields/{id}` | `metadata:update` | 编辑字段 |
| DELETE | `/api/metadata/fields/{id}` | `metadata:update` | 软删除 |
| GET | `/api/metadata/fields/groups` | `metadata:list` | 分组列表 (用于前端下拉) |
| GET | `/api/metadata/fields/by-group/{groupCode}` | `metadata:export` | 按分组查字段 |

### 8b. TemplateController (`/api/templates/*`)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/templates` | `template:list` | 分页列表 |
| GET | `/api/templates/{id}` | `template:list` | 模板详情 (含 regions) |
| POST | `/api/templates` | `template:list` | 创建模板 |
| PUT | `/api/templates/{id}` | `template:list` | 编辑模板 |
| DELETE | `/api/templates/{id}` | `template:list` | 软删除 |

**验证**：用前端页面 → 字段库 CRUD → 模板 CRUD 全部走通

---

## Step 9: 操作日志（AOP 切面）

**产出文件**：
```
aspect/
└── OperationLogAspect.java        # @Aspect, @Around("@annotation(...)")

annotation/
└── OperationLog.java              # @OperationLog(value = "用户管理", type = "create")

service/
└── OperationLogService.java       # 异步写入 operation_logs 表

controller/
└── OperationLogController.java    # /api/operation-logs/* (只读查询)
```

**AOP 切面逻辑**：
```java
@Around("@annotation(opLog)")
public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) {
    // 1. 从 SecurityContextHolder 获取当前用户
    // 2. 从 HttpServletRequest 获取 IP
    // 3. 序列化 requestParams (截断 2000 字符)
    // 4. long start = System.currentTimeMillis()
    // 5. Object result = joinPoint.proceed()
    // 6. long duration = System.currentTimeMillis() - start
    // 7. 异步写入 operation_logs (@Async)
    // 8. return result
    // catch: 记录失败状态
}
```

**OperationLogController 端点**：

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/operation-logs` | `log:operation` | 分页列表 (支持 type/module/status 搜索) |
| GET | `/api/operation-logs/{id}` | `log:operation` | 日志详情 |
| GET | `/api/operation-logs/export` | `log:operation` | 导出 (JSON) |

**@OperationLog 注解用法**：
```java
@PostMapping
@OperationLog(value = "用户管理", type = "create")
@PreAuthorize("hasAuthority('user:create')")
public ApiResponse<User> createUser(@RequestBody @Valid CreateUserRequest req) { ... }
```

**验证**：登录 → 创建用户 → 查看操作日志列表出现该条记录

---

## Step 10: 仪表盘 & Agent Traces（桩）

### 10a. DashboardController (`/api/dashboard/*`)

Phase 1 使用桩数据（真实数据需要 Phase 2+ 才能产出）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/stats` | 总览统计 (文档数/流水线数/字段数/今日处理量) |
| GET | `/api/dashboard/trends` | 处理趋势 (最近30天) |
| GET | `/api/dashboard/doc-type-distribution` | 文档类型分布 |
| GET | `/api/dashboard/status-distribution` | 处理状态分布 |
| GET | `/api/dashboard/recent-activities` | 最近活动 |

### 10b. AgentTraceController (`/api/agent-traces/*`)

Phase 1 返回空数组：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/agent-traces` | 分页列表 (空) |
| GET | `/api/agent-traces/{id}` | 详情 (含 nodes) |

**验证**：仪表盘页面不报错，至少显示 0 值；Agent 链路页面显示空表格

---

## Step 11: 前端对接真实 API

**目标**：前端从 Mock 数据切换到真实 Java 后端

### 11a. Vite 代理配置 (`frontend/vite.config.ts`)
```ts
// 已存在代理，确认指向 Java 8080:
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  }
}
```

### 11b. 关闭 Mock 层 (`frontend/src/main.ts`)
```ts
// 注释掉或条件判断：
// if (import.meta.env.DEV) setupMock()
// 改为通过环境变量控制：
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
  setupMock()
}
```

### 11c. `.env.development` 增加
```
VITE_USE_MOCK=false
```

### 11d. Token 传递验证
确认 `frontend/src/utils/request.ts` 中 Axios 拦截器正确携带 token：
```ts
config.headers.Authorization = `Bearer ${getToken()}`
```

**验证**：`npm run dev` → 打开前端 → 登录 admin/admin123 → 所有页面数据来自 Java 后端

---

## Step 12: 集成验证清单

```
□ Docker Compose 5 容器全部启动
□ Spring Boot 启动在 8080，无错误日志
□ POST /api/auth/login 返回 token
□ GET /api/auth/current-user 返回用户+角色+权限
□ CRUD /api/users/* 全部正常
□ CRUD /api/roles/* 全部正常（含权限分配）
□ GET /api/permissions 返回树形数据
□ POST /api/documents/upload 上传 PDF 到 MinIO 成功
□ GET /api/documents 分页列表正常
□ CRUD /api/metadata/fields/* 正常
□ CRUD /api/templates/* 正常
□ GET /api/operation-logs 返回数据（含 AOP 写入的记录）
□ GET /api/dashboard/stats 返回桩数据（不报错）
□ GET /api/agent-traces 返回空列表（不报错）
□ 前端登录 → 各页面数据来自 Java 后端（不再是 Mock）
□ 无权限用户访问受限页面 → 跳转 403
```

---

## 执行顺序

```
Docker Compose → Spring Boot 脚手架 → 数据库初始化
  → 认证系统 (Auth) → 统一响应格式
  → User CRUD → Role CRUD → Permission CRUD
  → Document Upload + MinIO
  → Metadata CRUD → Template CRUD
  → 操作日志 AOP
  → Dashboard (桩) + AgentTrace (桩)
  → 前端对接
```

预计工作量：约 3-5 天（全栈独立开发，每天 4-6 小时）

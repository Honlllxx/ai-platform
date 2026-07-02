## 1. 高层摘要 (TL;DR)

**影响范围：** 🟢 **高** - 全新搭建 Spring Boot 3.2.7 + Java 17 后端项目，涵盖完整的认证授权、用户管理、文档存储、Agent 追踪等核心功能

**核心变更：**

- ✨ **搭建完整的 Spring Boot 3.x 项目架构**，采用 Spring Security + JWT 实现无状态认证
- 🔐 **实现 RBAC 权限体系**，支持用户-角色-权限三级管理，Admin 自动拥有所有权限
- 📄 **集成 MinIO 对象存储**，支持 PDF/图片/Word 文档上传，按日期分桶存储
- 📊 **设计 Agent 追踪系统**，记录 AI Agent 执行链路的完整过程和节点详情
- 🪵 **实现 AOP 操作日志**，自动记录关键操作的请求参数、响应结果和执行耗时

------

## 2. 可视化架构总览

### 2.1 系统架构流程图

```
graph TD
    subgraph "🌐 前端请求层"
        Client["前端应用"]
    end

    subgraph "🔒 安全认证层"
        SecurityConfig["SecurityConfig"]
        JwtFilter["JwtAuthenticationFilter"]
        JwtProvider["JwtTokenProvider"]
    end

    subgraph "🎯 控制器层"
        AuthCtrl["AuthController"]
        UserCtrl["UserController"]
        DocCtrl["DocumentController"]
        AgentCtrl["AgentTraceController"]
        DashboardCtrl["DashboardController"]
    end

    subgraph "💼 业务服务层"
        AuthService["AuthService"]
        UserService["UserService"]
        DocService["DocumentService"]
        OpLogAspect["OperationLogAspect"]
    end

    subgraph "🗄️ 数据持久层"
        Postgres["PostgreSQL<br/>MyBatis-Plus"]
        Redis["Redis<br/>RefreshToken"]
        Minio["MinIO<br/>文件存储"]
    end

    Client -->|"POST /api/auth/login<br/>Authorization: Bearer {token}"| JwtFilter
    JwtFilter -->|"验证 Token"| JwtProvider
    JwtProvider -->|"提取 userId, authorities"| SecurityConfig
    SecurityConfig -->|"允许/拒绝"| AuthCtrl
    SecurityConfig -->|"允许/拒绝"| UserCtrl
    SecurityConfig -->|"允许/拒绝"| DocCtrl
    
    AuthCtrl -->|"登录/刷新/登出"| AuthService
    AuthService -->|"查询用户/角色"| Postgres
    AuthService -->|"存储 refreshToken<br/>key: refresh_token:{userId}"| Redis
    
    DocCtrl -->|"上传文件"| DocService
    DocService -->|"上传文档<br/>路径: documents/{date}/{uuid}.{ext}"| Minio
    DocService -->|"记录元数据"| Postgres
    
    OpLogAspect -.->|"拦截 @OperationLog 方法"| AuthCtrl
    OpLogAspect -.->|"拦截 @OperationLog 方法"| UserCtrl
    OpLogAspect -.->|"记录操作日志到数据库"| Postgres
    
    style SecurityConfig fill:#e3f2fd,color:#0d47a1
    style JwtFilter fill:#fff3e0,color:#e65100
    style AuthService fill:#c8e6c9,color:#1a5e20
    style DocService fill:#c8e6c9,color:#1a5e20
    style Postgres fill:#f3e5f5,color:#7b1fa2
    style Redis fill:#f3e5f5,color:#7b1fa2
    style Minio fill:#f3e5f5,color:#7b1fa2
```

### 2.2 认证授权流程图

```
sequenceDiagram
    participant User as 👤 用户
    participant AuthCtrl as AuthController
    participant AuthService as AuthService
    participant DB as PostgreSQL
    participant Redis as Redis
    participant JwtProvider as JwtTokenProvider
    participant SecFilter as JwtAuthenticationFilter

    User->>AuthCtrl: POST /api/auth/login(username, password)
    AuthCtrl->>AuthService: login(request)
    AuthService->>DB: 查询用户信息
    DB-->>AuthService: User + Role + Permission
    AuthService->>DB: 校验密码 BCrypt
    AuthService->>JwtProvider: 生成 accessToken(2h)
    AuthService->>JwtProvider: 生成 refreshToken(7d)
    JwtProvider-->>AuthService: JWT Token
    AuthService->>Redis: SET refresh_token:{userId}
    AuthService-->>AuthCtrl: LoginResponse
    AuthCtrl-->>User: {token, refreshToken, userInfo}

    Note over User,SecFilter: 后续请求携带 Token
    
    User->>SecFilter: API Request (Authorization: Bearer {token})
    SecFilter->>JwtProvider: validateToken(token)
    JwtProvider-->>SecFilter: userId + authorities
    SecFilter->>SecFilter: 设置 SecurityContext
    SecFilter-->>User: 继续处理请求

    User->>AuthCtrl: POST /api/auth/refresh(refreshToken)
    AuthCtrl->>AuthService: refreshToken(refreshToken)
    AuthService->>Redis: GET refresh_token:{userId}
    AuthService->>JwtProvider: 生成新 Token
    AuthService->>Redis: 更新 refreshToken
    AuthService-->>AuthCtrl: 新的 LoginResponse
    AuthCtrl-->>User: 新的 {token, refreshToken}

    User->>AuthCtrl: POST /api/auth/logout
    AuthCtrl->>AuthService: logout(userId)
    AuthService->>Redis: DEL refresh_token:{userId}
    AuthService-->>AuthCtrl: 成功
    AuthCtrl-->>User: 登出成功
```

### 2.3 文档上传流程图

```
flowchart TD
    subgraph "📁 DocumentController.java"
        Upload["upload(file, userId)"]
        ExtractToken["extractToken(request)"]
    end

    subgraph "💼 DocumentService.java"
        ValidateType["校验文件类型<br/>pdf|jpg|jpeg|png|doc|docx"]
        ValidateSize["校验文件大小<br/>最大 20MB"]
        GeneratePath["生成路径<br/>documents/{date}/{uuid}.{ext}"]
        MinioPut["minioClient.putObject()"]
        InsertDB["documentMapper.insert()"]
        MapType["mapFileType()<br/>pdf→pdf, image→jpg/png, word→doc/docx"]
    end

    subgraph "🗄️ 存储层"
        MinioBucket["MinIO Bucket: documents"]
        PostgresDocs["documents 表"]
    end

    Upload --> ExtractToken
    ExtractToken -->|"从 Token 提取 userId"| ValidateType
    ValidateType -->|"通过"| ValidateSize
    ValidateSize -->|"通过"| GeneratePath
    GeneratePath -->|"文件名: {date}/{uuid}.{ext}"| MinioPut
    MinioPut -->|"存储成功"| MapType
    MapType --> InsertDB
    InsertDB -->|"status = 'pending'"| PostgresDocs
    MinioPut --> MinioBucket
    
    style Upload fill:#bbdefb,color:#0d47a1
    style ValidateType fill:#fff3e0,color:#e65100
    style ValidateSize fill:#fff3e0,color:#e65100
    style MinioPut fill:#c8e6c9,color:#1a5e20
    style InsertDB fill:#c8e6c9,color:#1a5e20
```

------

## 3. 详细变更分析

### 3.1 🔐 认证授权模块 (Auth & Security)

**组件名称：** `Authentication & Authorization System`

**变更内容：**

#### ✅ 安全配置 (SecurityConfig.java)

- 配置 **无状态 Session 管理** (`SessionCreationPolicy.STATELESS`)
- **白名单路径**：登录、刷新Token、健康检查、Swagger 文档
- **CORS 配置**：允许跨域请求
- **CSRF 禁用**：适用于 API 服务
- **密码编码器**：使用 `BCryptPasswordEncoder`

#### ✅ JWT Token 管理 (JwtTokenProvider.java)

- **双 Token 机制**：

  - `accessToken`：有效期 **2 小时**（可配置）
  - `refreshToken`：有效期 **7 天**（可配置）

- **Token 载荷**：

  ```
  {
    "sub": "userId",
    "username": "username",
    "authorities": ["ROLE_log:agent", "ROLE_document:list"],
    "iat": "签发时间",
    "exp": "过期时间"
  }
  ```

- **签名算法**：HMAC-SHA256，使用 Base64 编码的密钥

#### ✅ 认证过滤器 (JwtAuthenticationFilter.java)

- 继承 `OncePerRequestFilter`，确保每个请求只执行一次
- 从 `Authorization: Bearer {token}` 提取 Token
- 验证 Token 有效性后，构建 `UsernamePasswordAuthenticationToken` 设置到 SecurityContext

#### ✅ 认证服务 (AuthService.java)

- **登录逻辑**：
  1. 查询用户，校验密码
  2. 检查用户状态（status = 0 表示禁用）
  3. 获取用户角色和权限
  4. **Admin 特权**：角色为 `admin` 时自动拥有所有权限
  5. 生成双 Token
  6. 将 `refreshToken` 存入 Redis（key: `refresh_token:{userId}`，TTL: 7天）
- **刷新 Token 逻辑**：
  1. 验证 refreshToken 有效性
  2. 检查 Redis 中是否存在匹配的 refreshToken
  3. 重新查询用户最新角色权限
  4. 生成新的 accessToken 和 refreshToken
  5. 更新 Redis 中的 refreshToken
- **登出逻辑**：删除 Redis 中的 refreshToken

**关键数据结构：**

| 表名               | 字段                       | 说明                           |
| :----------------- | :------------------------- | :----------------------------- |
| `users`            | username, password, status | 用户基本信息，密码 BCrypt 加密 |
| `roles`            | code                       | 角色编码，如 `admin`           |
| `permissions`      | code                       | 权限编码，如 `document:list`   |
| `user_roles`       | user_id, role_id           | 用户-角色关联                  |
| `role_permissions` | role_id, permission_id     | 角色-权限关联                  |

**依赖配置：**

```
# JWT 配置
jwt:
  secret: cGxhdGZvcm0tand0LXNlY3JldC1rZXktbXVzdC1iZS1hdC1sZWFzdC0yNTYtYml0cw==
  access-token-expiration: 7200000    # 2 小时
  refresh-token-expiration: 604800000 # 7 天
```

------

### 3.2 👥 用户管理模块 (User Management)

**组件名称：** `User Management System`

**变更内容：**

#### ✅ 用户实体 (User.java)

- 字段：id, username, password, nickname, email, phone, avatar, status
- **审计字段**：
  - `createdAt`：插入时自动填充
  - `updatedAt`：插入/更新时自动填充
  - `deletedAt`：逻辑删除字段（@TableLogic）

#### ✅ 用户服务 (UserService.java)

- **分页查询**：支持按用户名模糊搜索、按状态筛选
- **创建用户**：
  - 检查用户名唯一性
  - 密码 BCrypt 加密
  - 默认状态 `status = 1`（启用）
  - 可选分配角色
- **更新用户**：支持更新昵称、邮箱、手机号、角色
- **删除用户**：逻辑删除（更新 deletedAt）
- **重置密码**：生成新的 BCrypt 密码

#### ✅ 角色权限查询逻辑

- **Admin 角色**：自动拥有 `permissions` 表中所有权限
- **普通用户**：通过 `user_roles` → `role_permissions` → `permissions` 链路查询

**API 端点：**

| 方法   | 路径                             | 权限               | 说明             |
| :----- | :------------------------------- | :----------------- | :--------------- |
| GET    | `/api/users`                     | `ROLE_user:list`   | 分页查询用户列表 |
| GET    | `/api/users/{id}`                | `ROLE_user:list`   | 查询用户详情     |
| POST   | `/api/users`                     | `ROLE_user:create` | 创建用户         |
| PUT    | `/api/users/{id}`                | `ROLE_user:update` | 更新用户         |
| DELETE | `/api/users/{id}`                | `ROLE_user:delete` | 删除用户         |
| POST   | `/api/users/{id}/reset-password` | `ROLE_user:update` | 重置密码         |
| PUT    | `/api/users/{id}/status`         | `ROLE_user:update` | 更新用户状态     |

------

### 3.3 📄 文档管理模块 (Document Management)

**组件名称：** `Document & File Storage System`

**变更内容：**

#### ✅ 文档实体 (Document.java)

- 字段：id, fileName, fileType, fileSize, fileUrl, pageCount, uploadUserId, status, errorMessage
- **状态枚举**（推测）：`pending`, `processing`, `completed`, `failed`

#### ✅ 文档服务 (DocumentService.java)

- **支持的文件类型**：pdf, jpg, jpeg, png, doc, docx
- **文件类型映射**：
  - `pdf` → `pdf`
  - `jpg/jpeg/png` → `image`
  - `doc/docx` → `word`
- **存储路径策略**：`documents/{yyyyMMdd}/{uuid}.{ext}`
- **文件大小限制**：**最大 20MB**
- **上传流程**：
  1. 校验文件类型和大小
  2. 上传到 MinIO 对象存储
  3. 写入数据库记录（status 初始为 `pending`）

#### ✅ MinIO 配置 (MinioConfig.java)

```
# MinIO 配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin123
  bucket: documents
```

**API 端点：**

| 方法   | 路径                    | 权限                   | 说明                            |
| :----- | :---------------------- | :--------------------- | :------------------------------ |
| GET    | `/api/documents`        | `ROLE_document:list`   | 分页查询文档列表                |
| GET    | `/api/documents/{id}`   | `ROLE_document:list`   | 查询文档详情                    |
| POST   | `/api/documents/upload` | `ROLE_document:upload` | 上传文档                        |
| DELETE | `/api/documents/{id}`   | `ROLE_document:list`   | 删除文档（同步删除 MinIO 文件） |

------

### 3.4 🤖 Agent 追踪模块 (Agent Tracing)

**组件名称：** `Agent Trace & Pipeline Monitoring`

**变更内容：**

#### ✅ AgentTrace 实体

- 字段：id, traceId, sessionId, pipelineId, userId, username, query, status, totalDuration, totalTokens, startedAt, finishedAt
- **用途**：记录 AI Agent 每次执行的会话信息

#### ✅ AgentTraceNode 实体

- 字段：id, traceId, nodeId, parentNodeId, name, type, status, input, output, tokenCount, modelName, errorMessage, startedAt, finishedAt, duration
- **用途**：记录 Agent 执行链路中的每个节点详情

#### ✅ AgentTraceController

- **列表查询**：支持按 status 筛选、按 traceId/username 关键词搜索
- **详情查询**：返回 trace 信息及其关联的所有 nodes

**API 端点：**

| 方法 | 路径                     | 权限             | 说明                    |
| :--- | :----------------------- | :--------------- | :---------------------- |
| GET  | `/api/agent-traces`      | `ROLE_log:agent` | 分页查询 Agent 追踪列表 |
| GET  | `/api/agent-traces/{id}` | `ROLE_log:agent` | 查询追踪详情（含节点）  |

------

### 3.5 🪵 操作日志模块 (Operation Logging)

**组件名称：** `AOP-based Operation Logging`

**变更内容：**

#### ✅ 切面实现 (OperationLogAspect.java)

- **注解拦截**：`@Around("@annotation(com.platform.annotation.OperationLog)")`
- **记录内容**：
  - 模块 (`module`)
  - 操作类型 (`operationType`)
  - 用户名 (`username`)
  - 客户端 IP (`ip`)
  - 请求参数 (`requestParams`) - 截断至 2000 字符
  - 响应结果 (`responseResult`) - 截断至 1000 字符
  - 执行耗时 (`duration`)
  - 执行状态 (`status`) - `success` / `fail`

#### ✅ IP 提取逻辑

```
// 优先级：X-Forwarded-For > X-Real-IP > RemoteAddr
// 支持逗号分隔的代理 IP（取第一个）
```

**异常处理：**

- 方法执行异常时，记录 `status = 'fail'` 并重新抛出异常

------

### 3.6 📊 仪表板模块 (Dashboard)

**组件名称：** `Dashboard Analytics`

**变更内容：**

#### ✅ 统计指标

当前返回空数据，已预留以下字段：

- `userCount`：用户总数
- `todayOperations`：今日操作数
- `agentCalls`：Agent 调用次数
- `activeAgents`：活跃 Agent 数
- `autoPassRate`：自动通过率
- `manualCorrectionRate`：人工修正率
- `costSavings`：成本节约
- `avgProcessingTime`：平均处理时间

**API 端点：**

| 方法 | 路径                                         | 说明           |
| :--- | :------------------------------------------- | :------------- |
| GET  | `/api/dashboard/stats`                       | 获取统计数据   |
| GET  | `/api/dashboard/daily-processing`            | 每日处理量趋势 |
| GET  | `/api/dashboard/doc-type-distribution`       | 文档类型分布   |
| GET  | `/api/dashboard/operation-trend`             | 操作趋势       |
| GET  | `/api/dashboard/operation-type-distribution` | 操作类型分布   |

------

### 3.7 🛠️ 配置与依赖 (Configuration & Dependencies)

**组件名称：** `Project Configuration`

#### ✅ 核心依赖 (pom.xml)

| 包名                | 版本   | 用途                           |
| :------------------ | :----- | :----------------------------- |
| Spring Boot Starter | 3.2.7  | Web、Security、AOP、Validation |
| MyBatis-Plus        | 3.5.7  | ORM 框架，支持 PostgreSQL      |
| PostgreSQL Driver   | -      | 数据库驱动                     |
| Spring Data Redis   | -      | 缓存支持                       |
| JWT (jjwt)          | 0.12.6 | Token 生成和验证               |
| MinIO Client        | 8.5.10 | 对象存储                       |
| RocketMQ Starter    | 2.3.1  | 消息队列（Phase 1 预留）       |
| SpringDoc           | 2.3.0  | API 文档生成                   |
| Lombok              | -      | 代码简化                       |

#### ✅ 数据库配置 (application-dev.yml)

```
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/platform_db
    username: platform
    password: platform123
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  data:
    redis:
      host: localhost
      port: 6379
      password: redis123
```

#### ✅ MyBatis-Plus 配置

- **ID 策略**：`IdType.AUTO`（自增）
- **逻辑删除**：字段 `deletedAt`，删除值 `NOW()`，未删除值 `NULL`
- **驼峰转换**：自动映射下划线到驼峰

#### ✅ 其他配置

- **文件上传限制**：最大 20MB
- **Jackson 日期格式**：`yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`
- **Swagger UI**：`/swagger-ui.html`
- **OpenAPI 文档**：`/v3/api-docs`

------

### 3.8 ⚠️ 异常处理 (Exception Handling)

**组件名称：** `Global Exception Handler`

**变更内容：**

#### ✅ GlobalExceptionHandler (GlobalExceptionHandler.java)

| 异常类型                          | HTTP 状态码 | 响应                       |
| :-------------------------------- | :---------- | :------------------------- |
| `BusinessException`               | 200 OK      | 返回业务错误码和消息       |
| `AuthenticationException`         | 200 OK      | 401 错误码，登录失败       |
| `AccessDeniedException`           | 200 OK      | 403 错误码，无权限         |
| `MethodArgumentNotValidException` | 200 OK      | 400 错误码，参数校验失败   |
| `MaxUploadSizeExceededException`  | 200 OK      | 400 错误码，文件超限       |
| `Exception`                       | 200 OK      | 500 错误码，服务器内部错误 |

**响应格式：**

```
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

------

## 4. 影响与风险评估

### ⚠️ 破坏性变更

| 变更类型     | 影响范围 | 说明                   |
| :----------- | :------- | :--------------------- |
| **新建项目** | 无       | 全新项目，无破坏性变更 |

### 🧪 测试建议

#### 🔐 认证测试

- **登录流程**：验证正确的用户名密码能成功登录，返回双 Token
- **密码错误**：验证错误密码返回 401 错误
- **用户禁用**：验证 status=0 的用户无法登录
- **Token 过期**：验证 2 小时后 accessToken 自动过期
- **Token 刷新**：验证使用 refreshToken 能获取新 Token
- **登出失效**：验证登出后 refreshToken 无法再用于刷新
- **Token 拦截**：验证未携带 Token 的 API 请求被拒绝

#### 👥 用户管理测试

- **创建用户**：验证用户名唯一性校验，密码被加密存储
- **角色分配**：验证分配角色后权限立即生效
- **Admin 权限**：验证 admin 角色拥有所有权限
- **用户删除**：验证逻辑删除不影响数据完整性
- **重置密码**：验证重置后原密码失效

#### 📄 文档上传测试

- **文件类型**：验证只允许 pdf/jpg/jpeg/png/doc/docx
- **文件大小**：验证超过 20MB 的文件被拒绝
- **MinIO 存储**：验证文件正确上传到指定路径
- **数据库记录**：验证文档元数据正确入库
- **删除同步**：验证删除文档时 MinIO 文件也被删除

#### 🤖 Agent 追踪测试

- **分页查询**：验证支持按 status 和 keyword 筛选
- **关联查询**：验证详情接口能正确返回关联的 nodes

#### 🪵 操作日志测试

- **日志记录**：验证带 `@OperationLog` 的方法自动记录日志
- **异常处理**：验证方法异常时日志状态为 'fail'
- **IP 提取**：验证正确提取客户端 IP（包括代理场景）

#### 🛡️ 安全测试

- **SQL 注入**：验证使用 MyBatis-Plus 防御 SQL 注入
- **XSS**：验证输入参数被正确转义
- **CORS**：验证跨域请求正常处理
- **密码强度**：当前未强制验证密码强度，建议添加

### 📋 其他注意事项

1. **数据库初始化**：需要创建 `users`、`roles`、`permissions`、`user_roles`、`role_permissions`、`documents`、`agent_traces`、`agent_trace_nodes`、`operation_logs` 等表
2. **默认管理员**：建议初始化一个 admin 用户（username: admin，角色: admin）
3. **MinIO Bucket**：需要预先创建名为 `documents` 的 Bucket
4. **Redis 连接**：确保 Redis 服务正常运行
5. **环境变量**：生产环境应使用环境变量配置敏感信息（数据库密码、Redis 密码、MinIO 密钥、JWT 密钥）
6. **日志级别**：当前开发环境 DEBUG，生产环境建议调整为 INFO
7. **文件清理**：未实现文档定时清理逻辑，建议后续添加
8. **并发控制**：文件上传和删除未加锁，高并发场景可能需要优化

------

**📌 总结：** 这是一个设计完整的 Spring Boot 3.x 后端项目，实现了现代化的认证授权体系（JWT + Redis + RBAC）、文档存储（MinIO）、操作日志（AOP）和监控追踪（Agent Trace）等核心功能。代码结构清晰，遵循领域驱动设计，适合作为商业单据自动化录入平台的后端基础。
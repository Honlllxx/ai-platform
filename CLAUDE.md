# AI Platform Frontend

## 技术栈

- **Vue 3** (Composition API + `<script setup>`)
- **TypeScript**
- **Element Plus** 2.7 + `@element-plus/icons-vue`
- **Pinia** 状态管理
- **Vue Router** 4（createWebHistory 模式）
- **ECharts 5** + `vue-echarts` 图表
- **Axios** HTTP 请求
- **Sass** 样式
- **Vite 5** 构建工具

## 启动命令

```bash
cd frontend
npm run dev     # 端口 3000
npm run build   # 生产构建 (vue-tsc 类型检查 + vite build)
```

## 项目结构

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
│   │   └── data.ts            # 所有 Mock 数据（用户/角色/权限/日志/文档/流水线/元数据/模板/审核/提示词/微调/模型）
│   ├── router/
│   │   └── index.ts           # 路由配置 + 导航守卫（token 检查 + 权限验证）
│   ├── stores/
│   │   ├── app.ts             # 应用状态（侧边栏折叠、主题、语言）
│   │   ├── permission.ts      # 权限状态
│   │   └── user.ts            # 用户状态（token、用户信息、角色、权限）
│   ├── styles/
│   │   ├── index.scss         # 全局样式 + 工具类
│   │   └── variables.scss     # SCSS 变量（颜色、布局尺寸）
│   ├── utils/
│   │   ├── auth.ts            # Token 管理（localStorage）+ requestId 生成
│   │   └── request.ts         # Axios 实例（拦截器：token 注入、401 处理）
│   ├── views/
│   │   ├── dashboard/         # 仪表盘（ECharts 图表：处理趋势、文档类型分布）
│   │   ├── document/          # 文档管理（上传/列表/详情/版面分析/OCR/表格提取）
│   │   ├── finetune/          # 微调任务（列表/新建/取消）
│   │   ├── login/             # 登录页面
│   │   ├── log/
│   │   │   ├── agent-trace/   # Agent 链路追踪（列表/甘特图/时间线）
│   │   │   └── operation/     # 操作日志（列表/筛选/详情/导出）
│   │   ├── metadata/          # 元数据管理（字段库/数据导出/Webhook 配置）
│   │   ├── models/            # 模型资产（资产列表/模型绑定）
│   │   ├── pipeline/          # 处理流水线（列表/详情/状态图/提取/验证/反思）
│   │   ├── prompt/            # 提示词工程（版本管理/版本对比/自动化评测）
│   │   ├── review/            # 审核工作台（列表/详情/字段对比/批量操作）
│   │   ├── settings/          # 系统设置
│   │   ├── system/
│   │   │   ├── error/         # 403 / 404 错误页面
│   │   │   ├── permission/    # 权限管理（树形表格）
│   │   │   ├── role/          # 角色管理
│   │   │   └── user/          # 用户管理
│   │   └── template/          # 模板配置（列表/编辑器）
│   ├── App.vue
│   ├── env.d.ts               # TypeScript 类型声明
│   └── main.ts                # 应用入口
├── .env.development            # VITE_API_BASE_URL=/api, VITE_APP_ENV=development
├── .env.production
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.node.json
└── vite.config.ts              # @ 别名、端口 3000、API 代理到 localhost:8080、代码分包
```

## Mock 层说明

- 在 `main.ts` 中 `import.meta.env.DEV` 时自动调用 `setupMock()`
- `setupMock()` 替换 Axios 的 `defaults.adapter`，拦截所有请求返回假数据
- 匹配规则：`if (url === '/xxx' && method === 'get')` 逐个匹配，覆盖全部 14 个模块
- 未匹配的路由会 fall through 到真实 API（支持部分 mock）
- 网络延迟模拟：100-300ms 随机延迟
- Mock 数据直接修改内存数组（push/splice/Object.assign），重启后恢复
- **启动时控制台会打印**: `[Mock] Mock API layer enabled — 默认账号: admin / 任意6位以上密码`

## 默认账号

| 用户名 | 角色 | 权限 |
|--------|------|------|
| `admin` | 超级管理员 (admin) | 所有权限 |
| `zhangsan` | 普通用户 (user) | 基本操作权限 |
| `zhaoliu` | 审计员 (auditor) | 查看日志和链路 |

- 密码：**任意 6 位以上字符**（Mock 层只检查长度 >= 6）
- `wangwu`（审计员）、`lisi`（普通用户）也可用，但 wangwu 状态为禁用

## 路由与权限

- 路由守卫 `router.beforeEach`：
  1. 无 token → 重定向到 `/login?redirect=xxx`
  2. 有 token 但无用户信息 → 调用 `fetchUserInfo()` 获取
  3. 检查 `meta.permission` → 无权限则跳转 `/403`
- admin 角色拥有所有权限（`roles.includes('admin')` 时跳过权限检查）
- 三层权限控制：
  1. 路由守卫 — 无权限跳 403
  2. `v-permission` 指令 — 无权限时从 DOM 移除元素
  3. Sidebar 菜单过滤 — `hasXxxAccess` computed 控制整组显隐

## 已完成的模块

### 原始模块（Phase 1）
1. **认证系统** - 登录/登出、token 管理、路由守卫、refresh token
2. **用户管理** - CRUD、启用/禁用、分配角色
3. **角色管理** - CRUD、分配权限
4. **权限管理** - 树形结构、CRUD
5. **仪表盘** - ECharts 图表（处理趋势、文档类型分布、运营统计）
6. **操作日志** - 列表、筛选、详情、导出
7. **Agent 链路追踪** - 列表、甘特图、时间线/树形视图
8. **系统设置** - 基础设置页面
9. **403/404 错误页面**

### 新增模块（Phase 2-4）
10. **文档管理** - 上传/列表/详情/预处理/版面分析/OCR 结果/表格提取
11. **处理流水线** - 列表/详情/状态图/提取结果/验证报告/反思数据
12. **元数据管理** - 字段库 CRUD/数据导出/Webhook 配置
13. **模板配置** - 列表/编辑器（区域标注）
14. **审核工作台** - 列表/详情/字段对比审核/批量操作/审核历史、键盘快捷键（Enter=确认/Esc=跳过）
15. **提示词工程** - 版本管理/版本 Diff/回滚/自动化评测
16. **模型管理** - 微调任务/模型资产/模型绑定

### 权限码速查
```
document:upload     document:list
pipeline:list
metadata:list       metadata:export
template:list
review:list
prompt:version      prompt:evaluation
finetune:list       model:list
user:list           role:list           permission:list
log:operation       log:agent
settings:view
```

## 开发模式速查

### 新增模块标准步骤
1. 定义 API（`src/api/<module>.ts`）— 接口类型 + 请求函数
2. 添加 Mock 数据（`src/mock/data.ts` + `src/mock/index.ts`）— 字面量字段用 `as const`
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

## API 响应格式

后端/ Mock 统一返回格式：
```ts
{ code: 200 | 0, data: T, message: string }
```
Axios 响应拦截器检查 `code !== 0 && code !== 200` 时走错误处理。

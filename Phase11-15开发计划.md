# 项目剩余开发任务清单

> 日期: 2026-07-07 | 基于代码库三端深入探索分析

---

## 总体状态

| 层 | 已完成 | 待完成 |
|----|--------|--------|
| Java 后端 | 15 Controller / ~100 端点 / CRUD 完整 | 7 个端点桩 / HTTP 客户端缺失 / 无异步处理 |
| Python AI | 5 个 AI 端点真实实现 / 6 个服务 | 流水线无 DB 持久化 / 校验&反思缺失 / 提示词未集成 |
| 前端 | 14 模块 UI 完成 / Mock 可切换 | 多处硬编码数据绕过 API / API 字段名不匹配 / 错误处理弱 |

---

## 优先级排序

### 🔴 阻断级（P0）— 核心业务闭环中断

| # | 问题 | 层 | 影响 |
|---|------|----|------|
| P0-1 | **Preprocess 与 Python AI 无连接** | Java | 文档预处理是桩，用户点击无实际效果 |
| P0-2 | **Pipeline 只读、无触发** | Java | 无法启动/创建处理流水线 |
| P0-3 | **Python Pipeline 不写 DB** | Python | AI 处理结果不持久化，8 个 ORM 模型闲置 |
| P0-4 | **前端多处硬编码数据**（15+ 视图）| 前端 | `VITE_USE_MOCK=false` 下面仍用假数据，API 短路 |

### 🟡 高优（P1）— 关键功能缺失

| # | 问题 | 层 | 影响 |
|---|------|----|------|
| P1-1 | **API URL 不匹配**（4 处）| 前端 | model/finetune/review 等端点格式不一致 |
| P1-2 | **API 函数缺失**（reset-password/role-status/dashboard 端点不在 API 模块）| 前端 | 用户不能在前端调用这些操作 |
| P1-3 | **Upload 进度条伪造** | 前端 | 用户看到的是客户端模拟进度，非真实上传进度 |
| P1-4 | **Prompt 系统未集成** | Python | 4 个模板文件 + prompt_versions 表均不参与推理 |
| P1-5 | **Validation/Reflection 缺失** | Python | validation_results + reflection_results 表闲置 |
| P1-6 | **OperationLog 导出是桩** | Java | 返回原始数据而非生成文件 |
| P1-7 | **Prompt rollback/diff/eval 是桩** | Java | 3 个端点返回硬编码结果 |
| P1-8 | **Settings 页无后端 API** | 前端+Java | 系统设置页面全部失效 |

### 🟢 中优（P2）— 工程完善

| # | 问题 | 层 | 影响 |
|---|------|----|------|
| P2-1 | **Review 无 create 端点** | Java | 审核任务只能读不能新建 |
| P2-2 | **7 个 Controller 缺少 Service 层** | Java | 控制器直接调 Mapper，无业务逻辑封装 |
| P2-3 | **useTable 缺少 error 状态** | 前端 | 所有表格无"加载失败"UI |
| P2-4 | **空 catch 块吞错误** | 前端 | 组件级错误静默失败 |
| P2-5 | **PPStructureV3 双实例加载** | Python | layout 和 table 服务各加载一次，浪费内存 |
| P2-6 | **Pydantic 模型缺失** | Python | 所有端点接受 `body: dict`，无类型验证 |

### 🔵 远期（P3）— 大型特性

| # | 问题 | 层 | 说明 |
|---|------|----|------|
| P3-1 | **LangGraph 多智能体** | Python | Pipeline 当前是线性顺序调用，需改为 Route→Extract→Validate→Reflect 有状态图 |
| P3-2 | **RocketMQ 异步通信** | Java+Python | 依赖和配置已就绪，零实现代码 |
| P3-3 | **硅基流动 API Key 轮换** | Python | 密钥已在 git 历史暴露，需轮换 |

---

## 建议执行顺序

### Phase 11：Java ↔ Python AI 对接（P0-1, P0-2, P0-3）

**目标：文档上传→预处理→AI流水线→结果持久化 全链路打通**

1. **Java 端新增 HTTP Client**（`RestTemplate` 或 `WebClient`）
   - `PreprocessService` 调用 Python `/api/ai/preprocess`
   - `PipelineService` 调用 Python `/api/ai/pipeline/run`
2. **Python Pipeline 写入数据库** — `pipeline_service.py` 在每步完成后 `db.commit()`
3. **Java PipelineController 新增触发端点** — `POST /api/pipeline/run`

**涉及文件：** Java 新建 `PreprocessService`, `PipelineService`, `AiClientConfig`；Python 修改 `pipeline_service.py`

### Phase 12：Python AI 完善（P1-4, P1-5, P2-5）

**目标：校验、反思、提示词集成**

1. **提示词系统集成** — pipeline 从 DB 读 prompt_version，或读取 `prompts/*.txt`
2. **Validation 步骤** — LLM 校验提取结果，写入 `validation_results`
3. **Reflection 步骤** — 对低置信度字段做二次纠正
4. **PPStructureV3 单例复用** — 合并 layout/table 共享同一实例

**涉及文件：** Python `pipeline_service.py`, `prompt.py`，新建 `validation_service.py`, `reflection_service.py`

### Phase 13：前端 API 对接（P0-4, P1-1, P1-2, P1-3）

**目标：关闭 Mock 后所有页面正常工作**

1. **移除视图层硬编码数据** — 15 个文件中的 `generateMockData()` 和直接赋值的假数据
2. **修复 API URL 不匹配** — model.ts, finetune.ts, review.ts, pipeline 字段名
3. **补全缺失的 API 函数** — `resetPassword`, `updateRoleStatus`, dashboard 独立模块
4. **Upload 真实进度** — Axios `onUploadProgress` 回调替换伪造进度条
5. **useTable 错误状态** — 新增 `error` ref，视图层渲染错误 UI

**涉及文件：** 前文列出的 18+ 个视图文件、7 个 API 模块文件、useTable composable

### Phase 14：Java 工程完善（P0-4, P1-6, P1-7, P1-8, P2-1）

**目标：补齐桩端点、Service 层、Settings API**

1. **创建 SettingsController** — 后端支持系统设置 CRUD
2. **OperationLog 导出** — 生成 CSV/Excel 文件并返回下载 URL
3. **Prompt 端点去桩** — rollback 实现版本回滚逻辑，diff 实现文本对比，evaluation 触发真实评测
4. **Review create 端点** — 支持创建审核任务
5. **Controller Service 层补齐** — PipelineController 等 7 个 Controller 添加 Service

**涉及文件：** Java 新建 `SettingsController`, `SettingsService`, `PipelineService` 等；修改 `PromptController`, `OperationLogController`, `ReviewController`

### Phase 15：LangGraph + RocketMQ（P3-1, P3-2）

**目标：多智能体编排 + 异步消息**

1. **LangGraph StateGraph** — 定义 Route / Extract / Validate / Reflect 4 节点 + 条件边
2. **RocketMQ Producer**（Java）— 文档上传后发消息触发 AI 处理
3. **RocketMQ Consumer**（Python）— 消费消息执行流水线，结果回写 DB

---

## 依赖关系

```
Phase 11 (Java↔Python 对接) ← 最关键，无前置依赖
  ├── Phase 12 (Python AI 完善) ← 依赖 Phase 11 的 DB 持久化
  ├── Phase 13 (前端 API 对接) ← 可与 11/12 并行
  └── Phase 14 (Java 工程完善) ← 可与 11/12 并行
        └── Phase 15 (LangGraph + RocketMQ) ← 依赖前述全部
```

---

## 各 Phase 预估端点数

| Phase | 新增端点数 | 修改端点数 |
|-------|-----------|-----------|
| Phase 11 | 3-5 (Java: pipeline 触发 + Python: 响应) | 2 (preprocess 改造 + pipeline create) |
| Phase 12 | 0 | 1 (pipeline 内部步骤扩展) |
| Phase 13 | 0 | 15+ 视图文件 |
| Phase 14 | 10-15 (Settings CRUD 5 + Review create 1 + export 改进等) | 5 (去桩) |
| Phase 15 | 2 (RocketMQ producer/consumer) | 1 (pipeline 触发方式改为消息) |

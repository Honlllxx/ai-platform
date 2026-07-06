# Python AI 服务架构文档

> 文档版本: v1.0 | 日期: 2026-07-03 | 上级: [[全栈开发计划.md]]

## 一、概述

Python AI 服务是整个平台的**视觉感知层 + 认知推理层**，负责：
- 文档图像预处理（矫正、去印章）
- OCR 文字识别
- 版面布局分析
- 表格检测与结构重建
- LLM 字段抽取（路由 → 提取 → 校验 → 反思）

Java 业务后端通过 HTTP 调用 Python 服务的 AI 处理端点，各司其职。

## 二、技术栈

| 组件 | 选型 | 说明 |
|------|------|------|
| Web 框架 | FastAPI 0.115 + uvicorn 0.34 | 异步支持、自动 OpenAPI 文档 |
| OCR | PaddleOCR 2.10 | CPU 模式，内置版面分析 |
| 图像处理 | OpenCV 4.11 (headless) | 矫正、HSV 印章检测 |
| LLM | 硅基流动 `zai-org/GLM-5.2` | OpenAI 兼容 API |
| HTTP Client | httpx 0.28 | 异步 LLM 调用 |
| 数据库 | SQLAlchemy 2.0 + asyncpg | 只读 PostgreSQL |
| 配置 | pydantic-settings 2.7 | 环境变量 + .env 文件 |
| 文本对比 | diff-match-patch | 提示词版本 Diff |

## 三、项目结构

```
backend-python/
├── app/
│   ├── __init__.py
│   ├── main.py                     # FastAPI 应用入口 (CORS, 路由注册)
│   ├── config.py                   # pydantic-settings 统一配置
│   ├── database.py                 # SQLAlchemy async engine + session
│   │
│   ├── models/                     # 只读 ORM 模型（映射现有 PostgreSQL 表）
│   │   ├── __init__.py
│   │   ├── document.py             # documents, document_layouts, document_ocr, document_tables
│   │   ├── pipeline.py             # pipelines, extraction/validation/reflection_results
│   │   └── business.py             # metadata_fields, templates, prompt_versions
│   │
│   ├── schemas/                    # Pydantic 请求/响应模型
│   │   ├── __init__.py
│   │   └── common.py               # ApiResponse<T>, PageResult（对齐 Java 格式）
│   │
│   ├── routers/                    # API 路由层
│   │   ├── __init__.py
│   │   ├── ai.py                   # /api/ai/*          — AI 处理端点
│   │   ├── prompt.py               # /api/prompt/*      — 提示词版本管理
│   │   ├── finetune.py             # /api/finetune/*    — 微调任务管理
│   │   └── models_router.py        # /api/models/*      — 模型资产管理
│   │
│   └── services/                   # 业务逻辑层
│       ├── __init__.py
│       ├── llm_service.py          # 硅基流动 API 封装（httpx 异步客户端）
│       ├── preprocess_service.py   # OpenCV: 霍夫变换矫正 + HSV 去印章
│       ├── ocr_service.py          # PaddleOCR 封装（惰性加载）
│       ├── layout_service.py       # PPStructure 版面分析
│       ├── table_service.py        # 表格检测 + 单元格解析
│       └── pipeline_service.py     # 5 阶段流水线编排器
│
├── uploads/                        # 临时文件上传目录
├── prompts/                        # 提示词模板文件
├── models/                         # PaddleOCR 模型文件目录
├── requirements.txt
└── Dockerfile
```

## 四、配置说明 (`app/config.py`)

```python
# PostgreSQL (只读)
DB_HOST=localhost, DB_PORT=5432, DB_USER=postgres, DB_NAME=biz_form_recog_db

# LLM
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
SILICONFLOW_MODEL=zai-org/GLM-5.2

# OCR
OCR_LANG=ch, OCR_USE_GPU=False

# 超时
PIPELINE_LLM_TIMEOUT=120s, PIPELINE_OCR_TIMEOUT=60s
```

配置通过 `.env` 文件或环境变量注入，`pydantic-settings` 自动加载。

## 五、API 端点

### 5.1 基础

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| GET | `/docs` | Swagger UI |
| GET | `/redoc` | ReDoc API 文档 |

### 5.2 AI 处理 (`/api/ai/*`)

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| POST | `/api/ai/preprocess` | 图像预处理（矫正+去印章） | `file` (multipart) |
| POST | `/api/ai/ocr` | OCR 文字识别 | `file` (multipart) |
| POST | `/api/ai/layout` | 版面布局分析 | `file` (multipart) |
| POST | `/api/ai/table` | 表格检测与提取 | `file` (multipart) |
| POST | `/api/ai/pipeline/run` | **全流程**（预处理→版面→OCR→表格→LLM抽取） | `file` + `metadata_fields` (JSON) |

### 5.3 提示词管理 (`/api/prompt/*`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/prompt/versions` | 分页列表 |
| GET | `/api/prompt/versions/{id}` | 版本详情 |
| POST | `/api/prompt/versions` | 创建版本 |
| PUT | `/api/prompt/versions/{id}` | 编辑版本 |
| DELETE | `/api/prompt/versions/{id}` | 删除版本 |

### 5.4 微调任务 (`/api/finetune/*`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/finetune/tasks` | 任务列表 |
| GET | `/api/finetune/tasks/{id}` | 任务详情 |
| POST | `/api/finetune/tasks` | 创建任务 (Stub) |
| DELETE | `/api/finetune/tasks/{id}` | 取消任务 |

### 5.5 模型资产 (`/api/models/*`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/models` | 资产列表 |
| GET | `/api/models/{id}` | 资产详情 |
| POST | `/api/models` | 注册资产 |
| PUT | `/api/models/{id}` | 更新资产 |
| DELETE | `/api/models/{id}` | 软删除 |
| GET | `/api/models/bindings/list` | 绑定列表 |
| POST | `/api/models/bindings` | 创建绑定 |

### 5.6 响应格式（对齐 Java 后端）

```json
// 成功
{ "code": 200, "data": { ... }, "message": "success" }

// 分页
{ "code": 200, "data": { "records": [...], "total": 100, "page": 1, "page_size": 10 }, "message": "success" }

// 失败
{ "code": 500, "data": null, "message": "错误描述" }
```

## 六、AI 处理流水线

```
请求文件
    │
    ▼
[1] Preprocess     OpenCV: 霍夫变换矫正倾斜 + HSV 色彩空间检测/去除红色印章
    │
    ▼
[2] Layout          PaddleOCR PPStructure: 识别文本块、表格区域、图片区域
    │
    ▼
[3] OCR             PaddleOCR: 逐行识别文字，返回文本 + 坐标 + 置信度
    │
    ▼
[4] Table            PaddleOCR Table: 检测表格 → 解析单元格 → HTML/JSON 结构
    │
    ▼
[5] Extract          LLM: 输入 OCR 文本 + 元数据字段定义 → 输出 JSON 字段提取结果
    │
    ▼
返回结果: { task_id, status, duration, layout, ocr, tables, extraction, timeline }
```

### 阶段详情

| # | 阶段 | 耗时占比 | 输出 |
|---|------|----------|------|
| 1 | Preprocess | 5% | 矫正后图像 + 元数据 |
| 2 | Layout | 15% | 区域列表 `[{id, type, x, y, w, h}]` |
| 3 | OCR | 30% | 文本条目 `[{text, confidence, bbox}]` |
| 4 | Table | 10% | 表格 `[{headers, rows, html}]` |
| 5 | Extract | 40% | 字段 `[{field_code, value, confidence}]` |

## 七、服务层设计

### 7.1 LLM Service (`llm_service.py`)
- 单例模式，全服务共享一个 httpx 客户端
- `chat(messages, temperature, max_tokens)` → 原始响应
- `extract_json(system_prompt, user_content)` → 提取 JSON 结构化数据
- 自动处理 API 错误和超时

### 7.2 Preprocess Service (`preprocess_service.py`)
- `deskew()`: 霍夫变换检测直线角度 → 旋转校正
- `remove_stamp()`: HSV 红色范围检测 → 形态学膨胀 → inpainting 填充
- `process()`: 串联矫正 + 去印章

### 7.3 OCR Service (`ocr_service.py`)
- 惰性加载 PaddleOCR 模型（避免首次启动耗时）
- `recognize(image)` → `[{id, text, confidence, bbox: {x, y, w, h}}]`

### 7.4 Layout Service (`layout_service.py`)
- 使用 PaddleOCR PPStructure（`recovery=True` 恢复文档结构）
- `analyze(image)` → `[{id, type, label, x, y, width, height, content}]`

### 7.5 Table Service (`table_service.py`)
- 使用 PaddleOCR PPStructure Table 模式
- `extract_tables(image)` → `[{bbox, headers, rows, html, cell_count}]`

### 7.6 Pipeline Service (`pipeline_service.py`)
- 编排 5 阶段异步执行（使用 `asyncio.to_thread` 处理 CPU 密集型运算）
- 进度回调 `progress_callback(step, progress_pct, message)`
- 异常捕获，返回部分成功结果

## 八、数据库模型（只读）

Python 服务**只读**访问 PostgreSQL 获取：
- `metadata_fields` — 字段定义（供 LLM 提取时参考）
- `templates` — 模板配置（区域坐标 + 字段映射）
- `prompt_versions` — 提示词版本
- `documents` — 文档信息
- `pipelines` / `extraction_results` / `validation_results` / `reflection_results` — 流水线数据

**不直接写业务表**，结果通过 HTTP 响应返回给 Java，由 Java 写入数据库。

## 九、启动

```bash
# 首次安装
cd backend-python
pip install -r requirements.txt

# 启动（开发模式，热重载）
uvicorn app.main:app --reload --port 8000 --host 0.0.0.0

# 验证
curl http://localhost:8000/api/health
# → {"code":200,"data":{"status":"ok","service":"python-ai"},"message":"success"}

# Swagger UI
open http://localhost:8000/docs
```

## 十、Docker 部署

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

通过 `docker-compose.yml` 与 Java 后端、Nginx 一起编排。

## 十一、当前状态与后续规划

| 模块 | 状态 | 说明 |
|------|------|------|
| FastAPI 框架 | ✅ 完成 | 21 个 Python 文件，全部编译通过 |
| 数据库连接 | ✅ 完成 | 只读 PostgreSQL，对齐 28 张表 |
| LLM 客户端 | ✅ 完成 | 硅基流动 GLM-5.2 |
| AI 端点 | ✅ 完成 | 预处理/OCR/版面/表格/流水线 |
| Prompt 管理 | ✅ 完成 | CRUD 端点（数据库读写） |
| Finetune 管理 | ⚠️ Stub | CRUD 可用，LLaMA-Factory 调用待 Phase 4 |
| Model 管理 | ⚠️ Stub | CRUD 可用 |
| PaddleOCR 安装 | ⚠️ 待安装 | `pip install paddleocr paddlepaddle` |
| OpenCV 安装 | ⚠️ 待安装 | `pip install opencv-python-headless` |
| LangGraph Agent | 🔜 Phase 3 | Route/Extract/Validate/Reflect |
| RocketMQ 集成 | 🔜 Phase 3 | 异步通信替代当前 HTTP 同步 |

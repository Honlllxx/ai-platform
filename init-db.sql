-- ============================================================
-- 面向复杂版面的商业单据自动化录入平台
-- 数据库初始化脚本 (PostgreSQL 16)
-- ============================================================

\encoding UTF8



-- 先清空所有表（按依赖顺序）
DROP TABLE IF EXISTS webhook_config CASCADE;
DROP TABLE IF EXISTS export_history CASCADE;
DROP TABLE IF EXISTS agent_trace_nodes CASCADE;
DROP TABLE IF EXISTS agent_traces CASCADE;
DROP TABLE IF EXISTS operation_logs CASCADE;
DROP TABLE IF EXISTS model_bindings CASCADE;
DROP TABLE IF EXISTS model_assets CASCADE;
DROP TABLE IF EXISTS finetune_tasks CASCADE;
DROP TABLE IF EXISTS evaluations CASCADE;
DROP TABLE IF EXISTS prompt_versions CASCADE;
DROP TABLE IF EXISTS review_history CASCADE;
DROP TABLE IF EXISTS review_fields CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS templates CASCADE;
DROP TABLE IF EXISTS metadata_fields CASCADE;
DROP TABLE IF EXISTS reflection_results CASCADE;
DROP TABLE IF EXISTS validation_results CASCADE;
DROP TABLE IF EXISTS extraction_results CASCADE;
DROP TABLE IF EXISTS pipelines CASCADE;
DROP TABLE IF EXISTS document_tables CASCADE;
DROP TABLE IF EXISTS document_ocr CASCADE;
DROP TABLE IF EXISTS document_layouts CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS system_settings CASCADE;

-- ============================================================
-- 1. RBAC 权限体系
-- ============================================================

-- 1.1 用户表
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    nickname        VARCHAR(100),
    email           VARCHAR(100),
    phone           VARCHAR(30),
    avatar          VARCHAR(500),
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- 1.2 角色表
CREATE TABLE roles (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    code            VARCHAR(50)     NOT NULL UNIQUE,
    description     VARCHAR(500),
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_roles_code ON roles(code);

-- 1.3 权限表
CREATE TABLE permissions (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    code            VARCHAR(100)    NOT NULL UNIQUE,
    type            VARCHAR(20)     NOT NULL DEFAULT 'menu',
    parent_id       BIGINT          REFERENCES permissions(id),
    path            VARCHAR(200),
    icon            VARCHAR(50),
    sort            INT             NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_permissions_parent ON permissions(parent_id);
CREATE INDEX idx_permissions_type ON permissions(type);

-- 1.4 用户-角色关联
CREATE TABLE user_roles (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT          NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, role_id)
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- 1.5 角色-权限关联
CREATE TABLE role_permissions (
    id              BIGSERIAL       PRIMARY KEY,
    role_id         BIGINT          NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   BIGINT          NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(role_id, permission_id)
);
CREATE INDEX idx_role_perm_role ON role_permissions(role_id);
CREATE INDEX idx_role_perm_perm ON role_permissions(permission_id);

-- ============================================================
-- 2. 文档处理流水线
-- ============================================================

-- 2.1 文档表
CREATE TABLE documents (
    id              BIGSERIAL       PRIMARY KEY,
    file_name       VARCHAR(500)    NOT NULL,
    file_type       VARCHAR(20)     NOT NULL,
    file_size       BIGINT          NOT NULL,
    file_url        VARCHAR(1000)   NOT NULL,
    page_count      INT,
    upload_user_id  BIGINT          REFERENCES users(id),
    status          VARCHAR(30)     NOT NULL DEFAULT 'pending',
    error_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_file_type ON documents(file_type);
CREATE INDEX idx_documents_upload_user ON documents(upload_user_id);
CREATE INDEX idx_documents_created_at ON documents(created_at);

-- 2.2 版面分析结果
CREATE TABLE document_layouts (
    id              BIGSERIAL       PRIMARY KEY,
    document_id     BIGINT          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,  -- allow re-processing
    image_url       VARCHAR(1000),
    regions         JSONB           NOT NULL DEFAULT '[]',
    confidence      DOUBLE PRECISION,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doc_layouts_doc ON document_layouts(document_id);

-- 2.3 OCR 识别结果
CREATE TABLE document_ocr (
    id              BIGSERIAL       PRIMARY KEY,
    document_id     BIGINT          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,  -- allow re-processing
    total_chars     INT,
    avg_confidence  DOUBLE PRECISION,
    language        VARCHAR(20)     DEFAULT 'ch',
    entries         JSONB           NOT NULL DEFAULT '[]',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doc_ocr_doc ON document_ocr(document_id);

-- 2.4 表格提取结果
CREATE TABLE document_tables (
    id              BIGSERIAL       PRIMARY KEY,
    document_id     BIGINT          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,  -- allow re-processing
    confidence      DOUBLE PRECISION,
    original_table  JSONB           NOT NULL DEFAULT '{}',
    extracted_table JSONB           NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doc_tables_doc ON document_tables(document_id);

-- 2.5 处理流水线
CREATE TABLE pipelines (
    id              BIGSERIAL       PRIMARY KEY,
    document_id     BIGINT          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    task_id         VARCHAR(36)     NOT NULL UNIQUE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'pending',
    current_step    VARCHAR(50),
    total_steps     INT             NOT NULL DEFAULT 6,
    completed_steps INT             NOT NULL DEFAULT 0,
    duration        DOUBLE PRECISION,
    state_graph     JSONB,
    error_message   TEXT,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pipelines_doc ON pipelines(document_id);
CREATE INDEX idx_pipelines_status ON pipelines(status);
CREATE INDEX idx_pipelines_task_id ON pipelines(task_id);

-- 2.6 字段提取结果
CREATE TABLE extraction_results (
    id              BIGSERIAL       PRIMARY KEY,
    pipeline_id     BIGINT          NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    field_name      VARCHAR(100)    NOT NULL,
    field_code      VARCHAR(100)    NOT NULL,
    extracted_value TEXT,
    confidence      DOUBLE PRECISION NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'uncertain',
    page_ref        INT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_extraction_pipeline ON extraction_results(pipeline_id);
CREATE INDEX idx_extraction_status ON extraction_results(status);

-- 2.7 校验报告
CREATE TABLE validation_results (
    id              BIGSERIAL       PRIMARY KEY,
    pipeline_id     BIGINT          NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    rule            VARCHAR(200)    NOT NULL,
    field_name      VARCHAR(100),
    severity        VARCHAR(20)     NOT NULL DEFAULT 'info',
    message         TEXT            NOT NULL,
    suggestion      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_validation_pipeline ON validation_results(pipeline_id);

-- 2.8 反思纠正记录
CREATE TABLE reflection_results (
    id              BIGSERIAL       PRIMARY KEY,
    pipeline_id     BIGINT          NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    field_name      VARCHAR(100)    NOT NULL,
    before_value    TEXT,
    after_value     TEXT,
    reason          TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reflection_pipeline ON reflection_results(pipeline_id);

-- ============================================================
-- 3. 业务配置
-- ============================================================

-- 3.1 元数据字段库
CREATE TABLE metadata_fields (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    code            VARCHAR(100)    NOT NULL UNIQUE,
    type            VARCHAR(20)     NOT NULL DEFAULT 'string',
    length          INT,
    enum_values     VARCHAR(500),
    validation_rule VARCHAR(500),
    group_code      VARCHAR(50)     NOT NULL,
    group_name      VARCHAR(100)    NOT NULL,
    required        BOOLEAN         NOT NULL DEFAULT FALSE,
    description     VARCHAR(500),
    sort            INT             NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_metadata_group ON metadata_fields(group_code);
CREATE INDEX idx_metadata_code ON metadata_fields(code);
CREATE INDEX idx_metadata_status ON metadata_fields(status);

-- 3.2 模板配置
CREATE TABLE templates (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    document_type   VARCHAR(50)     NOT NULL,
    description     VARCHAR(500),
    regions         JSONB           NOT NULL DEFAULT '[]',
    status          SMALLINT        NOT NULL DEFAULT 1,
    version         INT             NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_templates_type ON templates(document_type);
CREATE INDEX idx_templates_status ON templates(status);

-- 3.3 审核工作台
CREATE TABLE reviews (
    id              BIGSERIAL       PRIMARY KEY,
    document_id     BIGINT          NOT NULL REFERENCES documents(id),
    pipeline_id     BIGINT          REFERENCES pipelines(id),
    document_type   VARCHAR(50),
    status          VARCHAR(20)     NOT NULL DEFAULT 'pending',
    total_fields    INT             NOT NULL DEFAULT 0,
    reviewed_fields INT             NOT NULL DEFAULT 0,
    approved_fields INT             NOT NULL DEFAULT 0,
    rejected_fields INT             NOT NULL DEFAULT 0,
    reviewer_id     BIGINT          REFERENCES users(id),
    started_at      TIMESTAMPTZ,
    reviewed_at     TIMESTAMPTZ,
    review_comment  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_reviews_doc ON reviews(document_id);
CREATE INDEX idx_reviews_status ON reviews(status);
CREATE INDEX idx_reviews_reviewer ON reviews(reviewer_id);

-- 3.4 审核字段
CREATE TABLE review_fields (
    id              BIGSERIAL       PRIMARY KEY,
    review_id       BIGINT          NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    extraction_id   BIGINT          REFERENCES extraction_results(id),
    field_name      VARCHAR(100)    NOT NULL,
    field_code      VARCHAR(100)    NOT NULL,
    extracted_value TEXT,
    confidence      DOUBLE PRECISION NOT NULL DEFAULT 0,
    image_region    JSONB,
    status          VARCHAR(20)     NOT NULL DEFAULT 'pending',
    corrected_value TEXT,
    comment         TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_review_fields_review ON review_fields(review_id);
CREATE INDEX idx_review_fields_status ON review_fields(status);

-- 3.5 审核操作历史
CREATE TABLE review_history (
    id              BIGSERIAL       PRIMARY KEY,
    review_id       BIGINT          NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    field_id        BIGINT          REFERENCES review_fields(id),
    action          VARCHAR(20)     NOT NULL,
    operator_id     BIGINT          REFERENCES users(id),
    detail          JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_review_history_review ON review_history(review_id);

-- ============================================================
-- 4. AI 工程
-- ============================================================

-- 4.1 提示词版本
CREATE TABLE prompt_versions (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    version         VARCHAR(20)     NOT NULL,
    document_type   VARCHAR(50)     NOT NULL,
    stage           VARCHAR(50)     NOT NULL,
    content         TEXT            NOT NULL,
    variables       JSONB,
    author          VARCHAR(100),
    status          VARCHAR(20)     NOT NULL DEFAULT 'draft',
    commit_message  VARCHAR(500),
    git_commit_hash VARCHAR(64),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prompt_doc_type ON prompt_versions(document_type);
CREATE INDEX idx_prompt_stage ON prompt_versions(stage);
CREATE INDEX idx_prompt_status ON prompt_versions(status);

-- 4.2 自动化评测
CREATE TABLE evaluations (
    id                  BIGSERIAL       PRIMARY KEY,
    task_name           VARCHAR(200)    NOT NULL,
    prompt_version_id   BIGINT          NOT NULL REFERENCES prompt_versions(id),
    prompt_version_name VARCHAR(200),
    dataset_name        VARCHAR(200),
    dataset_size        INT             NOT NULL DEFAULT 0,
    accuracy            DOUBLE PRECISION,
    precision           DOUBLE PRECISION,
    recall              DOUBLE PRECISION,
    f1_score            DOUBLE PRECISION,
    status              VARCHAR(20)     NOT NULL DEFAULT 'pending',
    duration            DOUBLE PRECISION,
    error_message       TEXT,
    detail              JSONB,
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_eval_prompt ON evaluations(prompt_version_id);
CREATE INDEX idx_eval_status ON evaluations(status);

-- 4.3 微调任务
CREATE TABLE finetune_tasks (
    id              BIGSERIAL       PRIMARY KEY,
    task_name       VARCHAR(200)    NOT NULL,
    base_model      VARCHAR(100)    NOT NULL,
    dataset_name    VARCHAR(200)    NOT NULL,
    dataset_size    INT             NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'pending',
    progress        INT             NOT NULL DEFAULT 0,
    epochs          INT             NOT NULL DEFAULT 3,
    current_epoch   INT             NOT NULL DEFAULT 0,
    learning_rate   DOUBLE PRECISION NOT NULL DEFAULT 2e-5,
    loss            DOUBLE PRECISION,
    metrics         JSONB,
    output_model    VARCHAR(200),
    error_message   TEXT,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    duration        DOUBLE PRECISION,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_finetune_tasks_status ON finetune_tasks(status);

-- 4.4 模型资产
CREATE TABLE model_assets (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    version         VARCHAR(50)     NOT NULL DEFAULT 'v1.0',
    type            VARCHAR(20)     NOT NULL,
    description     VARCHAR(1000),
    size            VARCHAR(20),
    file_path       VARCHAR(1000),
    framework       VARCHAR(50),
    status          VARCHAR(20)     NOT NULL DEFAULT 'inactive',
    metrics         JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_model_assets_type ON model_assets(type);
CREATE INDEX idx_model_assets_status ON model_assets(status);

-- 4.5 模型绑定
CREATE TABLE model_bindings (
    id              BIGSERIAL       PRIMARY KEY,
    binding_name    VARCHAR(200)    NOT NULL,
    base_model_id   BIGINT          NOT NULL REFERENCES model_assets(id),
    lora_adapter_id BIGINT          REFERENCES model_assets(id),
    dataset_id      BIGINT          REFERENCES model_assets(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'inactive',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_bindings_base ON model_bindings(base_model_id);
CREATE INDEX idx_bindings_status ON model_bindings(status);

-- ============================================================
-- 5. 日志审计
-- ============================================================

-- 5.1 操作日志
CREATE TABLE operation_logs (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          REFERENCES users(id),
    username        VARCHAR(50)     NOT NULL,
    operation_type  VARCHAR(30)     NOT NULL,
    module          VARCHAR(100)    NOT NULL,
    description     TEXT,
    request_params  TEXT,
    response_result TEXT,
    ip              VARCHAR(50),
    duration        INT,
    status          VARCHAR(10)     NOT NULL DEFAULT 'success',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_op_logs_user ON operation_logs(user_id);
CREATE INDEX idx_op_logs_type ON operation_logs(operation_type);
CREATE INDEX idx_op_logs_module ON operation_logs(module);
CREATE INDEX idx_op_logs_created_at ON operation_logs(created_at);
CREATE INDEX idx_op_logs_status ON operation_logs(status);

-- 5.2 Agent 链路追踪
CREATE TABLE agent_traces (
    id              BIGSERIAL       PRIMARY KEY,
    trace_id        VARCHAR(100)    NOT NULL UNIQUE,
    session_id      VARCHAR(100)    NOT NULL,
    pipeline_id     BIGINT          REFERENCES pipelines(id),
    user_id         BIGINT          REFERENCES users(id),
    username        VARCHAR(50),
    query           TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'running',
    total_duration  DOUBLE PRECISION,
    total_tokens    INT             NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ     NOT NULL,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_agent_traces_trace ON agent_traces(trace_id);
CREATE INDEX idx_agent_traces_session ON agent_traces(session_id);
CREATE INDEX idx_agent_traces_pipeline ON agent_traces(pipeline_id);
CREATE INDEX idx_agent_traces_status ON agent_traces(status);
CREATE INDEX idx_agent_traces_user ON agent_traces(user_id);

-- 5.3 Agent 链路节点
CREATE TABLE agent_trace_nodes (
    id              BIGSERIAL       PRIMARY KEY,
    trace_id        BIGINT          NOT NULL REFERENCES agent_traces(id) ON DELETE CASCADE,
    node_id         VARCHAR(50)     NOT NULL,
    parent_node_id  VARCHAR(50),
    name            VARCHAR(200)    NOT NULL,
    type            VARCHAR(30)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'running',
    input           TEXT,
    output          TEXT,
    token_count     INT             NOT NULL DEFAULT 0,
    model_name      VARCHAR(100),
    error_message   TEXT,
    started_at      TIMESTAMPTZ     NOT NULL,
    finished_at     TIMESTAMPTZ,
    duration        DOUBLE PRECISION,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trace_nodes_trace ON agent_trace_nodes(trace_id);
CREATE INDEX idx_trace_nodes_parent ON agent_trace_nodes(parent_node_id);
CREATE INDEX idx_trace_nodes_type ON agent_trace_nodes(type);

-- ============================================================
-- 6. 辅助表
-- ============================================================

-- 6.1 导出历史
CREATE TABLE export_history (
    id              BIGSERIAL       PRIMARY KEY,
    format          VARCHAR(20)     NOT NULL,
    file_url        VARCHAR(1000),
    document_ids    JSONB,
    record_count    INT             NOT NULL DEFAULT 0,
    operator_id     BIGINT          REFERENCES users(id),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_export_hist_format ON export_history(format);

-- 6.2 Webhook 配置
CREATE TABLE webhook_config (
    id              BIGSERIAL       PRIMARY KEY,
    url             VARCHAR(1000)   NOT NULL,
    secret          VARCHAR(255),
    enabled         BOOLEAN         NOT NULL DEFAULT FALSE,
    events          JSONB           NOT NULL DEFAULT '[]',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE system_settings (
    id              BIGSERIAL       PRIMARY KEY,
    setting_key     VARCHAR(255)    NOT NULL,
    setting_value   TEXT,
    category        VARCHAR(100)    NOT NULL DEFAULT 'general',
    description     VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_system_settings_cat_key ON system_settings(category, setting_key);

-- Default system settings
INSERT INTO system_settings (setting_key, setting_value, category, description) VALUES
    ('siteName', 'AI Platform', 'basic', '系统名称'),
    ('logo', '', 'basic', '系统Logo URL'),
    ('description', '智能管理平台', 'basic', '系统描述'),
    ('pageSize', '20', 'basic', '每页条数'),
    ('tokenExpire', '120', 'security', 'Token过期时间(分钟)'),
    ('maxLoginAttempts', '5', 'security', '最大登录尝试次数'),
    ('minPasswordLength', '6', 'security', '密码最小长度'),
    ('sessionTimeout', '30', 'security', '会话超时(分钟)'),
    ('retentionDays', '90', 'log', '日志保留天数'),
    ('enableOperationLog', 'true', 'log', '操作日志记录'),
    ('enableAgentTrace', 'true', 'log', 'Agent链路记录'),
    ('logRequestParams', 'true', 'log', '记录请求参数'),
    ('logResponseResult', 'false', 'log', '记录响应结果');

-- ============================================================
-- 7. 种子数据
-- ============================================================

-- 7.1 默认用户
-- 密码由 BCryptPasswordEncoder 生成，部署时替换
INSERT INTO users (id, username, password, nickname, email, status) VALUES
(1, 'admin',    '$2b$10$sgqrEuV/qBKQ5N.Bgo6eCe.a/bDDHRRRIbAW2FdiCOe.DzxIGvGUy', '超级管理员', 'admin@platform.com', 1),
(2, 'zhangsan', '$2b$10$sgqrEuV/qBKQ5N.Bgo6eCe.a/bDDHRRRIbAW2FdiCOe.DzxIGvGUy', '张三',       'zhangsan@test.com',  1),
(3, 'zhaoliu',  '$2b$10$sgqrEuV/qBKQ5N.Bgo6eCe.a/bDDHRRRIbAW2FdiCOe.DzxIGvGUy', '赵六',       'zhaoliu@test.com',   1),
(4, 'lisi',     '$2b$10$sgqrEuV/qBKQ5N.Bgo6eCe.a/bDDHRRRIbAW2FdiCOe.DzxIGvGUy', '李四',       'lisi@test.com',      1),
(5, 'wangwu',   '$2b$10$sgqrEuV/qBKQ5N.Bgo6eCe.a/bDDHRRRIbAW2FdiCOe.DzxIGvGUy', '王五',       'wangwu@test.com',    0);

-- 重置序列避免冲突
SELECT setval('users_id_seq', 5);

-- 7.2 默认角色
INSERT INTO roles (id, name, code, description, status) VALUES
(1, '超级管理员', 'admin',   '拥有所有权限',   1),
(2, '普通用户',   'user',    '基本操作权限',   1),
(3, '审计员',     'auditor', '查看日志和链路',  1);

SELECT setval('roles_id_seq', 3);

-- 7.3 权限种子数据（38 条）
-- 一级菜单和基础权限 (parent_id 先写 NULL，后面 UPDATE)
INSERT INTO permissions (id, name, code, type, parent_id, path, icon, sort) VALUES
-- 仪表盘
(1,  '仪表盘',       'dashboard',              'menu',   NULL, '/dashboard',               'Odometer',       0),
-- 文档管理
(2,  '文档管理',     'document',               'menu',   NULL, NULL,                        'Folder',         10),
(3,  '文档上传',     'document:upload',        'button', 2,    '/documents/upload',         NULL,             11),
(4,  '文档列表',     'document:list',          'menu',   2,    '/documents/list',           NULL,             12),
-- 处理流水线
(5,  '处理流水线',   'pipeline',               'menu',   NULL, NULL,                        'Connection',     20),
(6,  '流水线列表',   'pipeline:list',          'menu',   5,    '/pipeline/list',            NULL,             21),
-- 业务配置
(7,  '业务配置',     'business',               'menu',   NULL, NULL,                        'Setting',        30),
(8,  '模板列表',     'template:list',          'menu',   7,    '/templates/list',           NULL,             31),
-- 审核工作台
(9,  '审核工作台',   'review',                 'menu',   NULL, NULL,                        'Checked',        40),
(10, '审核列表',     'review:list',            'menu',   9,    '/review/list',              NULL,             41),
(110, '创建审核',     'review:create',         'button', 9,    NULL,                        NULL,             42),
-- 元数据管理
(11, '元数据管理',   'metadata',               'menu',   NULL, NULL,                        'Grid',           50),
(12, '字段列表',     'metadata:list',          'menu',   11,   '/metadata/fields',          NULL,             51),
(13, '数据导出',     'metadata:export',        'menu',   11,   '/metadata/export',          NULL,             52),
-- 提示词工程
(14, '提示词工程',   'prompt',                 'menu',   NULL, NULL,                        'Document',       60),
(15, '版本管理',     'prompt:version',         'menu',   14,   '/prompt/versions',          NULL,             61),
(16, '自动化评测',   'prompt:evaluation',      'menu',   14,   '/prompt/evaluation',        NULL,             62),
-- 模型管理
(17, '模型管理',     'model',                  'menu',   NULL, NULL,                        'Cpu',            70),
(18, '微调任务',     'finetune:list',          'menu',   17,   '/finetune/tasks',           NULL,             71),
(19, '模型资产',     'model:list',             'menu',   17,   '/models/assets',            NULL,             72),
-- 系统管理
(20, '系统管理',     'system',                 'menu',   NULL, NULL,                        'Tools',          80),
(21, '用户管理',     'user:list',              'menu',   20,   '/system/users',             NULL,             81),
(22, '角色管理',     'role:list',              'menu',   20,   '/system/roles',             NULL,             82),
(23, '权限管理',     'permission:list',        'menu',   20,   '/system/permissions',       NULL,             83),
-- 日志审计
(24, '日志审计',     'log',                    'menu',   NULL, NULL,                        'Tickets',        90),
(25, '操作日志',     'log:operation',          'menu',   24,   '/logs/operations',          NULL,             91),
(26, 'Agent链路',    'log:agent',              'menu',   24,   '/logs/agent-traces',        NULL,             92),
-- 系统设置
(27, '系统设置',     'settings',               'menu',   NULL, NULL,                        'Tools',          100),
(28, '设置查看',     'settings:view',          'button', 27,   '/settings',                 NULL,             101);

-- 增补子权限（CRUD 操作级别）
INSERT INTO permissions (id, name, code, type, parent_id, sort) VALUES
-- 用户操作
(29, '创建用户', 'user:create',         'button', 21, 811),
(30, '编辑用户', 'user:update',         'button', 21, 812),
(31, '删除用户', 'user:delete',         'button', 21, 813),
-- 角色操作
(32, '创建角色', 'role:create',         'button', 22, 821),
(33, '编辑角色', 'role:update',         'button', 22, 822),
(34, '删除角色', 'role:delete',         'button', 22, 823),
-- 权限操作
(35, '创建权限', 'permission:create',   'button', 23, 831),
(36, '编辑权限', 'permission:update',   'button', 23, 832),
(37, '删除权限', 'permission:delete',   'button', 23, 833),
-- 元数据操作
(38, '编辑元数据', 'metadata:update',   'button', 11, 531),
-- 审核操作
(39, '编辑审核', 'review:update',       'button', 9,  421),
(40, '删除审核', 'review:delete',       'button', 9,  422);

SELECT setval('permissions_id_seq', 41);

-- 7.4 角色-权限关联
-- admin(1) 拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- user(2) 拥有基本操作权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE code IN (
    'dashboard', 'document', 'document:upload', 'document:list',
    'pipeline', 'pipeline:list',
    'business', 'template:list',
    'review', 'review:list'
);

-- auditor(3) 拥有日志查看权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE code IN (
    'dashboard', 'log', 'log:operation', 'log:agent'
);

-- 7.5 用户-角色关联
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),  -- admin → 超级管理员
(2, 2),  -- zhangsan → 普通用户
(3, 3),  -- zhaoliu → 审计员
(4, 2),  -- lisi → 普通用户
(5, 3);  -- wangwu → 审计员（但用户状态为禁用）

-- ============================================================
-- 8. 元数据字段种子数据（示例）
-- ============================================================

-- 合同字段
INSERT INTO metadata_fields (name, code, type, enum_values, validation_rule, group_code, group_name, required, sort) VALUES
('合同编号',     'contract_no',       'string',  NULL,                                    'required|max:50',   'contract', '合同', TRUE,  1),
('合同名称',     'contract_name',     'string',  NULL,                                    'required|max:200',  'contract', '合同', TRUE,  2),
('甲方名称',     'party_a',           'string',  NULL,                                    'required|max:200',  'contract', '合同', TRUE,  3),
('乙方名称',     'party_b',           'string',  NULL,                                    'required|max:200',  'contract', '合同', TRUE,  4),
('合同金额',     'contract_amount',   'number',  NULL,                                    'required',          'contract', '合同', TRUE,  5),
('签订日期',     'sign_date',         'date',    NULL,                                    'required',          'contract', '合同', TRUE,  6),
('生效日期',     'effective_date',    'date',    NULL,                                    'required',          'contract', '合同', TRUE,  7),
('到期日期',     'expiry_date',       'date',    NULL,                                    'nullable',          'contract', '合同', FALSE, 8),
('结算方式',     'payment_method',    'enum',    '电汇,信用证,承兑汇票,现金',              'required',          'contract', '合同', TRUE,  9),
('合同条款',     'contract_terms',    'string',  NULL,                                    'nullable',          'contract', '合同', FALSE, 10);

-- 发票字段
INSERT INTO metadata_fields (name, code, type, enum_values, validation_rule, group_code, group_name, required, sort) VALUES
('发票号码',     'invoice_no',        'string',  NULL,                                    'required|max:20',   'invoice',  '发票', TRUE,  1),
('开票日期',     'invoice_date',      'date',    NULL,                                    'required',          'invoice',  '发票', TRUE,  2),
('购买方名称',   'buyer_name',        'string',  NULL,                                    'required|max:200',  'invoice',  '发票', TRUE,  3),
('购买方税号',   'buyer_tax_no',      'string',  NULL,                                    'max:30',            'invoice',  '发票', FALSE, 4),
('销售方名称',   'seller_name',       'string',  NULL,                                    'required|max:200',  'invoice',  '发票', TRUE,  5),
('销售方税号',   'seller_tax_no',     'string',  NULL,                                    'max:30',            'invoice',  '发票', FALSE, 6),
('合计金额',     'total_amount',      'number',  NULL,                                    'required',          'invoice',  '发票', TRUE,  7),
('税率',         'tax_rate',          'number',  '0,3,6,9,13',                            'required',          'invoice',  '发票', TRUE,  9),
('税额',         'tax_amount',        'number',  NULL,                                    'required',          'invoice',  '发票', TRUE,  10);

-- 提单字段
INSERT INTO metadata_fields (name, code, type, enum_values, validation_rule, group_code, group_name, required, sort) VALUES
('提单号',       'bl_no',             'string',  NULL,                                    'required|max:50',   'bill_of_lading', '提单', TRUE,  1),
('船名',         'vessel_name',       'string',  NULL,                                    'required|max:100',  'bill_of_lading', '提单', TRUE,  2),
('航次',         'voyage_no',         'string',  NULL,                                    'required|max:30',   'bill_of_lading', '提单', TRUE,  3),
('装货港',       'port_of_loading',   'string',  NULL,                                    'required|max:100',  'bill_of_lading', '提单', TRUE,  4),
('卸货港',       'port_of_discharge', 'string',  NULL,                                    'required|max:100',  'bill_of_lading', '提单', TRUE,  5),
('发货人',       'shipper',           'string',  NULL,                                    'required|max:200',  'bill_of_lading', '提单', TRUE,  6),
('收货人',       'consignee',         'string',  NULL,                                    'required|max:200',  'bill_of_lading', '提单', TRUE,  7),
('货物描述',     'cargo_description', 'string',  NULL,                                    'nullable',          'bill_of_lading', '提单', FALSE, 8),
('件数',         'package_count',     'number',  NULL,                                    'required',          'bill_of_lading', '提单', TRUE,  9),
('毛重',         'gross_weight',      'number',  NULL,                                    'required',          'bill_of_lading', '提单', TRUE,  10);

-- ============================================================
-- 9. 默认 Webhook 配置
-- ============================================================

INSERT INTO webhook_config (id, url, secret, enabled, events) VALUES
(1, '', '', FALSE, '["extraction.completed", "review.approved", "document.uploaded"]');

SELECT setval('webhook_config_id_seq', 1);

-- ============================================================
-- 初始化完成
-- ============================================================

-- 验证数据完整性
DO $$
BEGIN
    RAISE NOTICE '====================================';
    RAISE NOTICE '数据库初始化完成!';
    RAISE NOTICE '  用户数: %', (SELECT COUNT(*) FROM users);
    RAISE NOTICE '  角色数: %', (SELECT COUNT(*) FROM roles);
    RAISE NOTICE '  权限数: %', (SELECT COUNT(*) FROM permissions);
    RAISE NOTICE '  元数据字段数: %', (SELECT COUNT(*) FROM metadata_fields);
    RAISE NOTICE '  表总数: 28';
    RAISE NOTICE '====================================';
    RAISE NOTICE '默认登录账号:';
    RAISE NOTICE '  admin   / admin123  (超级管理员)';
    RAISE NOTICE '  zhangsan / admin123 (普通用户)';
    RAISE NOTICE '  zhaoliu  / admin123 (审计员)';
    RAISE NOTICE '====================================';
END $$;

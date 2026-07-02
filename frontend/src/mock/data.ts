// ==================== Users ====================
export const users = [
  {
    id: 1, username: 'admin', nickname: '系统管理员', email: 'admin@ai-platform.com', phone: '13800000001',
    avatar: '', status: 1, roleIds: [1], roleNames: ['超级管理员'], createTime: '2026-01-10 09:00:00'
  },
  {
    id: 2, username: 'zhangsan', nickname: '张三', email: 'zhangsan@ai-platform.com', phone: '13800000002',
    avatar: '', status: 1, roleIds: [2], roleNames: ['普通用户'], createTime: '2026-02-15 14:30:00'
  },
  {
    id: 3, username: 'lisi', nickname: '李四', email: 'lisi@ai-platform.com', phone: '13800000003',
    avatar: '', status: 1, roleIds: [2], roleNames: ['普通用户'], createTime: '2026-03-20 10:00:00'
  },
  {
    id: 4, username: 'wangwu', nickname: '王五', email: 'wangwu@ai-platform.com', phone: '13800000004',
    avatar: '', status: 0, roleIds: [3], roleNames: ['审计员'], createTime: '2026-04-12 16:00:00'
  },
  {
    id: 5, username: 'zhaoliu', nickname: '赵六', email: 'zhaoliu@ai-platform.com', phone: '13800000005',
    avatar: '', status: 1, roleIds: [3], roleNames: ['审计员'], createTime: '2026-05-08 11:20:00'
  }
]
let userIdSeq = 5

export function nextUserId() { return ++userIdSeq }

// ==================== Roles ====================
export const roles = [
  { id: 1, name: '超级管理员', code: 'admin', description: '系统超级管理员，拥有所有权限', status: 1, permissionIds: Array.from({ length: 38 }, (_, i) => i + 1), userCount: 1, createTime: '2026-01-10 09:00:00' },
  { id: 2, name: '普通用户', code: 'user', description: '普通用户，拥有基本操作权限', status: 1, permissionIds: [1, 2, 5, 8], userCount: 2, createTime: '2026-02-15 14:30:00' },
  { id: 3, name: '审计员', code: 'auditor', description: '审计员，可查看日志和链路', status: 1, permissionIds: [1, 2, 8, 9, 10], userCount: 2, createTime: '2026-04-01 08:00:00' }
]
let roleIdSeq = 3

export function nextRoleId() { return ++roleIdSeq }

// ==================== Permissions ====================
export const permissionTree = [
  {
    id: 1, name: '仪表盘', code: 'dashboard:view', type: 'menu', parentId: null, path: '/dashboard', icon: 'Odometer', sort: 1, status: 1,
    children: []
  },
  {
    id: 2, name: '系统管理', code: 'system', type: 'menu', parentId: null, path: '/system', icon: 'Setting', sort: 2, status: 1,
    children: [
      { id: 3, name: '用户管理', code: 'user:list', type: 'menu', parentId: 2, path: '/system/users', icon: 'User', sort: 1, status: 1, children: [] },
      { id: 4, name: '新增用户', code: 'user:create', type: 'button', parentId: 3, path: '', icon: '', sort: 1, status: 1, children: [] },
      { id: 5, name: '编辑用户', code: 'user:update', type: 'button', parentId: 3, path: '', icon: '', sort: 2, status: 1, children: [] },
      { id: 6, name: '删除用户', code: 'user:delete', type: 'button', parentId: 3, path: '', icon: '', sort: 3, status: 1, children: [] },
      { id: 7, name: '分配角色', code: 'user:assign-role', type: 'button', parentId: 3, path: '', icon: '', sort: 4, status: 1, children: [] },
      { id: 8, name: '启用/禁用', code: 'user:status', type: 'button', parentId: 3, path: '', icon: '', sort: 5, status: 1, children: [] },
      { id: 9, name: '角色管理', code: 'role:list', type: 'menu', parentId: 2, path: '/system/roles', icon: 'Avatar', sort: 2, status: 1, children: [] },
      { id: 10, name: '新增角色', code: 'role:create', type: 'button', parentId: 9, path: '', icon: '', sort: 1, status: 1, children: [] },
      { id: 11, name: '编辑角色', code: 'role:update', type: 'button', parentId: 9, path: '', icon: '', sort: 2, status: 1, children: [] },
      { id: 12, name: '删除角色', code: 'role:delete', type: 'button', parentId: 9, path: '', icon: '', sort: 3, status: 1, children: [] },
      { id: 13, name: '分配权限', code: 'role:assign-permission', type: 'button', parentId: 9, path: '', icon: '', sort: 4, status: 1, children: [] },
      { id: 14, name: '权限管理', code: 'permission:list', type: 'menu', parentId: 2, path: '/system/permissions', icon: 'Lock', sort: 3, status: 1, children: [] },
      { id: 15, name: '新增权限', code: 'permission:create', type: 'button', parentId: 14, path: '', icon: '', sort: 1, status: 1, children: [] },
      { id: 16, name: '编辑权限', code: 'permission:update', type: 'button', parentId: 14, path: '', icon: '', sort: 2, status: 1, children: [] },
      { id: 17, name: '删除权限', code: 'permission:delete', type: 'button', parentId: 14, path: '', icon: '', sort: 3, status: 1, children: [] }
    ]
  },
  {
    id: 18, name: '日志审计', code: 'log', type: 'menu', parentId: null, path: '/logs', icon: 'Document', sort: 3, status: 1,
    children: [
      { id: 19, name: '操作日志', code: 'log:operation', type: 'menu', parentId: 18, path: '/logs/operations', icon: 'List', sort: 1, status: 1, children: [] },
      { id: 20, name: 'Agent链路', code: 'log:agent', type: 'menu', parentId: 18, path: '/logs/agent-traces', icon: 'Connection', sort: 2, status: 1, children: [] }
    ]
  },
  {
    id: 21, name: '系统设置', code: 'settings:view', type: 'menu', parentId: null, path: '/settings', icon: 'Tools', sort: 4, status: 1,
    children: []
  },
  {
    id: 22, name: '文档管理', code: 'document', type: 'menu', parentId: null, path: '/documents', icon: 'Folder', sort: 5, status: 1,
    children: [
      { id: 23, name: '文档上传', code: 'document:upload', type: 'menu', parentId: 22, path: '/documents/upload', icon: 'Upload', sort: 1, status: 1, children: [] },
      { id: 24, name: '文档列表', code: 'document:list', type: 'menu', parentId: 22, path: '/documents/list', icon: 'List', sort: 2, status: 1, children: [] }
    ]
  },
  {
    id: 25, name: '处理流水线', code: 'pipeline', type: 'menu', parentId: null, path: '/pipeline', icon: 'Connection', sort: 6, status: 1,
    children: [
      { id: 26, name: '流水线列表', code: 'pipeline:list', type: 'menu', parentId: 25, path: '/pipeline/list', icon: 'SetUp', sort: 1, status: 1, children: [] }
    ]
  },
  {
    id: 27, name: '业务配置', code: 'business', type: 'menu', parentId: null, path: '/templates', icon: 'EditPen', sort: 7, status: 1,
    children: [
      { id: 28, name: '模板配置', code: 'template:list', type: 'menu', parentId: 27, path: '/templates/list', icon: 'Crop', sort: 1, status: 1, children: [] },
      { id: 29, name: '审核工作台', code: 'review:list', type: 'menu', parentId: 27, path: '/review/list', icon: 'Checked', sort: 2, status: 1, children: [] }
    ]
  },
  {
    id: 30, name: '元数据管理', code: 'metadata', type: 'menu', parentId: null, path: '/metadata', icon: 'Coin', sort: 8, status: 1,
    children: [
      { id: 31, name: '字段库', code: 'metadata:list', type: 'menu', parentId: 30, path: '/metadata/fields', icon: 'Grid', sort: 1, status: 1, children: [] },
      { id: 32, name: '数据导出', code: 'metadata:export', type: 'menu', parentId: 30, path: '/metadata/export', icon: 'Download', sort: 2, status: 1, children: [] }
    ]
  },
  {
    id: 33, name: '提示词工程', code: 'prompt', type: 'menu', parentId: null, path: '/prompt', icon: 'Edit', sort: 9, status: 1,
    children: [
      { id: 34, name: '版本管理', code: 'prompt:version', type: 'menu', parentId: 33, path: '/prompt/versions', icon: 'CollectionTag', sort: 1, status: 1, children: [] },
      { id: 35, name: '自动化评测', code: 'prompt:evaluation', type: 'menu', parentId: 33, path: '/prompt/evaluation', icon: 'TrendCharts', sort: 2, status: 1, children: [] }
    ]
  },
  {
    id: 36, name: '模型管理', code: 'model', type: 'menu', parentId: null, path: '/models', icon: 'Cpu', sort: 10, status: 1,
    children: [
      { id: 37, name: '微调任务', code: 'finetune:list', type: 'menu', parentId: 36, path: '/finetune/tasks', icon: 'Monitor', sort: 1, status: 1, children: [] },
      { id: 38, name: '模型资产', code: 'model:list', type: 'menu', parentId: 36, path: '/models/assets', icon: 'Box', sort: 2, status: 1, children: [] }
    ]
  }
]
let permIdSeq = 38

export function nextPermId() { return ++permIdSeq }

// Flatten permissions for API
export function flattenPermissions(tree: any[]): any[] {
  const result: any[] = []
  function walk(nodes: any[]) {
    for (const node of nodes) {
      result.push({ ...node, children: undefined })
      if (node.children) walk(node.children)
    }
  }
  walk(tree)
  return result
}

// ==================== Operation Logs ====================
const operationTypes = ['login', 'query', 'create', 'update', 'delete', 'export', 'grant', 'other']
const modules = ['用户管理', '角色管理', '权限管理', '系统设置', 'Agent', '认证']
const descriptions: Record<string, string[]> = {
  login: ['用户登录系统', '用户退出登录'],
  query: ['查询用户列表', '查询角色列表', '查询操作日志', '查询Agent链路', '查询权限树'],
  create: ['新增用户 admin2', '新增角色 运营', '新增权限 menu:report'],
  update: ['修改用户信息', '更新角色权限', '修改系统设置'],
  delete: ['删除用户 test_user', '删除过期角色'],
  export: ['导出操作日志', '导出用户数据'],
  grant: ['为用户分配角色', '为角色分配权限'],
  other: ['系统配置变更', '缓存刷新']
}

export const operationLogs = Array.from({ length: 50 }, (_, i) => {
  const type = operationTypes[Math.floor(Math.random() * operationTypes.length)]
  const descArr = descriptions[type]
  const desc = descArr[Math.floor(Math.random() * descArr.length)]
  const hour = String(Math.floor(Math.random() * 24)).padStart(2, '0')
  const min = String(Math.floor(Math.random() * 60)).padStart(2, '0')
  const sec = String(Math.floor(Math.random() * 60)).padStart(2, '0')
  const day = 29 - Math.floor(i / 8)
  const date = `2026-06-${String(day).padStart(2, '0')}`
  return {
    id: i + 1,
    userId: (i % 5) + 1,
    username: ['admin', 'zhangsan', 'lisi', 'wangwu', 'zhaoliu'][i % 5],
    operationType: type,
    module: modules[Math.floor(Math.random() * modules.length)],
    description: desc,
    requestParams: JSON.stringify({ page: 1, pageSize: 10, keyword: 'test' }),
    responseResult: JSON.stringify({ code: 200, message: 'success', data: { total: Math.floor(Math.random() * 100) } }),
    ip: `192.168.1.${Math.floor(Math.random() * 255)}`,
    duration: Math.floor(Math.random() * 500) + 10,
    status: Math.random() > 0.05 ? 'success' : 'fail',
    createTime: `${date} ${hour}:${min}:${sec}`
  }
}).sort((a, b) => b.createTime.localeCompare(a.createTime))

// ==================== Agent Traces ====================
function makeTraceNodes(parentId: string | null, count: number, depth: number): any[] {
  if (depth > 3 || count <= 0) return []
  const types = ['llm_call', 'tool_call', 'embedding', 'vector_search', 'retrieval', 'llm_response'] as const
  const nodes: any[] = []
  for (let i = 0; i < count; i++) {
    const type = types[Math.floor(Math.random() * types.length)]
    const baseMs = 1688050000000 + Math.random() * 5000
    const duration = (Math.random() * 2 + 0.1)
    const hasChildren = depth < 2 && Math.random() > 0.5
    const node: any = {
      id: `${parentId || 'root'}-${depth}-${i}`,
      parentId,
      name: type === 'llm_call' ? `LLM Call (gpt-4)` :
            type === 'tool_call' ? `Tool: ${['search', 'calculator', 'weather'][Math.floor(Math.random() * 3)]}` :
            type === 'embedding' ? 'Embedding (text-embedding-3)' :
            type === 'vector_search' ? 'Vector Search (milvus)' :
            type === 'retrieval' ? 'Retrieval (chunk-0)' : 'LLM Response',
      type,
      status: Math.random() > 0.1 ? 'success' : Math.random() > 0.5 ? 'fail' : 'running',
      startTime: new Date(baseMs).toISOString(),
      endTime: new Date(baseMs + duration * 1000).toISOString(),
      duration: parseFloat(duration.toFixed(2)),
      input: JSON.stringify({ prompt: 'What is AI?', maxTokens: 1024 }),
      output: JSON.stringify({ text: 'AI stands for Artificial Intelligence...', finish: 'stop' }),
      tokenCount: Math.floor(Math.random() * 2000) + 100,
      modelName: type.includes('llm') ? 'gpt-4-turbo' : type === 'embedding' ? 'text-embedding-3-small' : undefined,
      children: hasChildren ? makeTraceNodes(`${parentId || 'root'}-${depth}-${i}`, Math.floor(Math.random() * 3) + 1, depth + 1) : []
    }
    nodes.push(node)
  }
  return nodes
}

export const agentTraces = Array.from({ length: 15 }, (_, i) => {
  const nodes = makeTraceNodes('root', 4, 0)
  const totalDuration = nodes.reduce((sum: number, n: any) => sum + n.duration + (n.children || []).reduce((s: number, c: any) => s + c.duration, 0), 0)
  const totalTokens = nodes.reduce((sum: number, n: any) => sum + (n.tokenCount || 0) + (n.children || []).reduce((s: number, c: any) => s + (c.tokenCount || 0), 0), 0)
  const day = 29 - Math.floor(i / 5)
  const hour = String(Math.floor(Math.random() * 24)).padStart(2, '0')
  const min = String(Math.floor(Math.random() * 60)).padStart(2, '0')
  return {
    id: `trace-${String(i + 1).padStart(3, '0')}`,
    traceId: `trace-202606${String(day).padStart(2, '0')}-${String(i + 1).padStart(3, '0')}`,
    sessionId: `session-${Math.random().toString(36).substring(2, 10)}`,
    userId: String((i % 5) + 1),
    username: ['admin', 'zhangsan', 'lisi', 'wangwu', 'zhaoliu'][i % 5],
    query: ['如何实现用户认证？', 'Python性能优化方案', 'Docker部署最佳实践', '数据库索引原理', '微服务架构设计'][i % 5],
    status: i === 2 ? 'running' : i === 4 ? 'fail' : 'success',
    totalDuration: parseFloat(totalDuration.toFixed(2)),
    totalTokens,
    startTime: `2026-06-${String(day).padStart(2, '0')} ${hour}:${min}:00`,
    endTime: `2026-06-${String(day).padStart(2, '0')} ${hour}:${String(Number(min) + Math.floor(totalDuration)).padStart(2, '0')}:${String(Math.floor(Math.random() * 60)).padStart(2, '0')}`,
    nodes
  }
})

// ==================== Documents ====================
const docTypes = ['pdf', 'jpg', 'png', 'word'] as const
const docStatuses = ['pending', 'preprocessing', 'completed', 'failed'] as const
const docNames = [
  '2026年度销售合同_编号SC2026-001.pdf',
  '增值税发票_No.44032026.jpg',
  '海运提单_BOL_CNUS2026.png',
  '采购订单_PO_2026_0567.pdf',
  '技术服务协议_TSA_2026Q2.docx',
  '进口报关单_20260629.pdf',
  '质量检验报告_QIR_2026.png',
  '运费结算单_2026_06.jpg',
  '设备租赁合同_ELC_2026.pdf',
  '产品合格证_COC_202606.docx',
  '供应商评估表_SEF_2026Q2.pdf',
  '出货装箱单_PL_2026_032.jpg',
  '银行保函_BG_2026_004.pdf',
  '信用证_LC_2026_0089.docx',
  '货物运输保险单_2026.png'
]

export const documents = Array.from({ length: 25 }, (_, i) => {
  const day = 29 - Math.floor(i / 5)
  const hour = String(Math.floor(Math.random() * 24)).padStart(2, '0')
  const min = String(Math.floor(Math.random() * 60)).padStart(2, '0')
  const type = docTypes[Math.floor(Math.random() * docTypes.length)]
  const status = docStatuses[Math.floor(Math.random() * docStatuses.length)]
  const sizes: Record<string, number> = { pdf: 250000, jpg: 180000, png: 220000, word: 150000 }
  return {
    id: i + 1,
    fileName: docNames[i % docNames.length],
    fileType: type,
    fileSize: sizes[type] + Math.floor(Math.random() * 500000),
    uploadTime: `2026-06-${String(day).padStart(2, '0')} ${hour}:${min}:${String(Math.floor(Math.random() * 60)).padStart(2, '0')}`,
    status,
    pageCount: Math.floor(Math.random() * 10) + 1,
    uploadUser: ['admin', 'zhangsan', 'lisi', 'zhaoliu'][i % 4]
  }
})

let docIdSeq = 25
export function nextDocId() { return ++docIdSeq }

// ==================== Pipeline ====================
const pipelineSteps = ['版面分析', 'OCR识别', '表格提取', '字段提取', '数据校验', '反思修正']
export const pipelines = Array.from({ length: 12 }, (_, i) => {
  const status = (['pending', 'running', 'running', 'completed', 'completed', 'completed', 'failed'] as const)[i % 7]
  const totalSteps = pipelineSteps.length
  const completedSteps = status === 'completed' ? totalSteps : status === 'failed' ? Math.floor(Math.random() * 4) + 2 : status === 'running' ? Math.floor(Math.random() * 4) + 1 : 0
  return {
    id: i + 1,
    documentId: i + 1,
    documentName: documents[i]?.fileName || `document_${i + 1}.pdf`,
    status,
    currentStep: status === 'completed' ? 'completed' : pipelineSteps[completedSteps] || pipelineSteps[0],
    totalSteps,
    completedSteps,
    duration: status === 'pending' ? 0 : Math.floor(Math.random() * 300) + 10,
    startTime: new Date(Date.now() - Math.random() * 86400000).toISOString().replace('T', ' ').substring(0, 19),
    endTime: status === 'completed' || status === 'failed' ? new Date(Date.now() - Math.random() * 3600000).toISOString().replace('T', ' ').substring(0, 19) : undefined,
    error: status === 'failed' ? 'OCR识别超时：文档图像质量过低' : undefined
  }
})

// State graph data for each pipeline
function generateStateGraph(): any {
  const nodes = [
    { id: 'start', label: '文档接入', type: 'input', status: 'completed', duration: 0.5 },
    { id: 'layout', label: '版面分析', type: 'llm', status: 'completed', duration: 2.3 },
    { id: 'ocr', label: 'OCR 识别', type: 'tool', status: 'completed', duration: 4.1 },
    { id: 'table', label: '表格提取', type: 'tool', status: Math.random() > 0.3 ? 'completed' : 'running', duration: 1.8 },
    { id: 'extract', label: '字段提取', type: 'llm', status: Math.random() > 0.5 ? 'completed' : 'pending', duration: 3.2 },
    { id: 'validate', label: '数据校验', type: 'decision', status: 'pending', duration: 0 },
    { id: 'correct', label: '反思修正', type: 'llm', status: 'pending', duration: 0 },
    { id: 'output', label: '结果输出', type: 'output', status: 'pending', duration: 0 }
  ]
  const edges = [
    { from: 'start', to: 'layout' },
    { from: 'layout', to: 'ocr' },
    { from: 'ocr', to: 'table' },
    { from: 'table', to: 'extract' },
    { from: 'extract', to: 'validate' },
    { from: 'validate', to: 'correct', label: '有异常' },
    { from: 'validate', to: 'output', label: '校验通过' },
    { from: 'correct', to: 'extract', label: '重新提取' }
  ]
  return { nodes, edges }
}

export const pipelineStateGraphs: Record<number, any> = {}
pipelines.forEach(p => { pipelineStateGraphs[p.id] = generateStateGraph() })

// Extraction results mock
export const extractionResults: Record<number, any[]> = {}
const fieldNames = ['合同编号', '签订日期', '甲方名称', '乙方名称', '合同金额', '付款方式', '交货地点', '发票号码', '开票日期', '购方名称', '销方名称', '货物名称', '规格型号', '数量', '单价', '税率', '提单号', '船名', '航次', '装货港', '卸货港', '收货人', '发货人', '运费', '保险金额']
pipelines.forEach(p => {
  extractionResults[p.id] = fieldNames.slice(0, 8 + Math.floor(Math.random() * 12)).map((name, idx) => ({
    id: idx + 1,
    fieldName: name,
    extractedValue: `${name === '合同金额' || name === '单价' || name === '运费' || name === '保险金额' ? '¥' : ''}${name === '合同金额' ? (Math.random() * 1000000).toFixed(2) : name === '单价' ? (Math.random() * 10000).toFixed(2) : name === '数量' ? Math.floor(Math.random() * 1000) : name.includes('日期') ? `2026-0${Math.floor(Math.random() * 9) + 1}-${String(Math.floor(Math.random() * 28) + 1).padStart(2, '0')}` : `样本值_${idx + 1}`}`,
    confidence: parseFloat((0.45 + Math.random() * 0.55).toFixed(2)),
    status: (['verified', 'uncertain', 'rejected'] as const)[Math.floor(Math.random() * 3)],
    pageRef: Math.floor(Math.random() * 3) + 1
  }))
})

// Validation report mock
export const validationReports: Record<number, any[]> = {}
pipelines.forEach(p => {
  validationReports[p.id] = [
    { id: 1, rule: '日期格式校验', field: '签订日期', severity: 'error', message: '日期格式不一致: 2026/02/30 vs 2026-02-28', suggestion: '统一为 YYYY-MM-DD 格式' },
    { id: 2, rule: '金额合理性', field: '合同金额', severity: 'warning', message: '合同金额超出同类合同平均值 2.5 倍标准差', suggestion: '请人工核实合同金额是否正确' },
    { id: 3, rule: '必填字段检查', field: '付款方式', severity: 'info', message: '付款方式字段为空，已使用默认值"电汇"填充', suggestion: undefined },
    { id: 4, rule: '关联校验', field: '数量/单价/金额', severity: 'error', message: '数量 × 单价 != 合同金额 (100 × 50.20 != 5280.00)', suggestion: '请核实数量、单价或合同金额' },
    { id: 5, rule: '发票号格式', field: '发票号码', severity: 'warning', message: '发票号码长度异常 (应为10-12位，当前为8位)', suggestion: '检查发票号码是否完整' },
    { id: 6, rule: '税率校验', field: '税率', severity: 'info', message: '税率 16% 已过期，当前法定税率为 13%', suggestion: '确认是否为历史发票' }
  ].slice(0, 3 + Math.floor(Math.random() * 4))
})

// Reflection correction data
export const reflectionData: Record<number, any[]> = {}
pipelines.forEach(p => {
  reflectionData[p.id] = [
    { id: 1, fieldName: '合同编号', beforeValue: 'SC2026-O01', afterValue: 'SC2026-001', reason: 'OCR将数字0误识别为字母O，根据上下文修正', timestamp: new Date(Date.now() - 60000).toISOString() },
    { id: 2, fieldName: '合同金额', beforeValue: '¥5,280.00', afterValue: '¥5,020.00', reason: '表格对齐偏差导致提取值错误，重新定位单元格后修正', timestamp: new Date(Date.now() - 30000).toISOString() }
  ].slice(0, 1 + Math.floor(Math.random() * 2))
})

// Layout analysis data
export const layoutData: Record<number, any> = {}
documents.forEach(doc => {
  layoutData[doc.id] = {
    imageUrl: '',
    regions: [
      { id: 'r1', label: '文档标题', x: 10, y: 5, width: 60, height: 8, color: '#409eff', type: 'title', confidence: 0.98 },
      { id: 'r2', label: '合同编号信息', x: 5, y: 15, width: 90, height: 10, color: '#67c23a', type: 'text', confidence: 0.95 },
      { id: 'r3', label: '甲方信息块', x: 5, y: 28, width: 42, height: 20, color: '#67c23a', type: 'text', confidence: 0.92 },
      { id: 'r4', label: '乙方信息块', x: 52, y: 28, width: 43, height: 20, color: '#67c23a', type: 'text', confidence: 0.91 },
      { id: 'r5', label: '合同条款正文', x: 5, y: 50, width: 90, height: 30, color: '#67c23a', type: 'text', confidence: 0.88 },
      { id: 'r6', label: '合同金额表格', x: 10, y: 82, width: 80, height: 15, color: '#e6a23c', type: 'table', confidence: 0.96 }
    ]
  }
})

// OCR data
export const ocrData: Record<number, any> = {}
documents.forEach(doc => {
  const texts = [
    '销售合同', '合同编号：SC2026-001', '签订日期：2026年6月15日',
    '甲方：XX科技有限公司', '法定代表人：张三', '地址：广东省深圳市南山区科技园路88号',
    '乙方：YY贸易有限公司', '法定代表人：李四', '地址：上海市浦东新区陆家嘴环路100号',
    '第一条 合同标的', '1.1 甲方向乙方购买以下产品：', '产品名称：工业传感器 XR-500',
    '规格型号：XR-500-A2', '数量：100台', '单价：¥5,020.00', '总金额：¥502,000.00',
    '第二条 付款方式', '2.1 乙方应在合同签订后15个工作日内支付合同总金额的30%作为预付款',
    '2.2 货物交付验收合格后10个工作日内支付合同总金额的70%',
    '第三条 交货时间与地点', '3.1 交货时间：2026年8月1日前', '3.2 交货地点：深圳市南山区科技园路88号'
  ]
  ocrData[doc.id] = {
    totalChars: texts.reduce((s: number, t: string) => s + t.length, 0),
    averageConfidence: parseFloat((0.75 + Math.random() * 0.2).toFixed(2)),
    language: 'zh-CN',
    entries: texts.map((text, idx) => ({
      id: idx + 1,
      text,
      confidence: parseFloat((0.7 + Math.random() * 0.3).toFixed(2)),
      bbox: { x: 5, y: 5 + idx * 4.5, width: 85, height: 3.5 }
    }))
  }
})

// Table extraction data
export const tableData: Record<number, any> = {}
documents.forEach(doc => {
  tableData[doc.id] = {
    confidence: parseFloat((0.8 + Math.random() * 0.19).toFixed(2)),
    originalTable: {
      headers: ['产品名称', '规格型号', '数量', '单价(元)', '总价(元)'],
      rows: [
        ['工业传感器 XR-500', 'XR-500-A2', '100', '5,020.00', '502,000.00'],
        ['数据采集模块', 'DAQ-200', '50', '3,500.00', '175,000.00'],
        ['连接线缆', 'CBL-10M', '200', '85.00', '17,000.00']
      ]
    },
    extractedTable: {
      headers: ['产品名称', '规格型号', '数量', '单价(元)', '总价(元)'],
      rows: [
        ['工业传感器 XR-500', 'XR-500-A2', '100', '5,020.00', '502,000.00'],
        ['数据采集模块', 'DAQ-200', '50', '3,500.00', '175,000.00'],
        ['连接线缆', 'CBL-10M', '200', '85.00', '17,000.00']
      ]
    }
  }
})

// ==================== Metadata Fields ====================
const metaGroups = [
  { code: 'contract', name: '合同' },
  { code: 'invoice', name: '发票' },
  { code: 'bill_of_lading', name: '提单' }
]

export const metadataFields = [
  // Contract fields
  { id: 1, name: '合同编号', code: 'contract_no', type: 'string', length: 50, enumValues: '', validationRule: 'required|max:50', groupCode: 'contract', groupName: '合同', required: true, description: '合同唯一标识编号', sort: 1, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 2, name: '签订日期', code: 'sign_date', type: 'date', length: 0, enumValues: '', validationRule: 'required|date', groupCode: 'contract', groupName: '合同', required: true, description: '合同签订日期', sort: 2, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 3, name: '甲方名称', code: 'party_a_name', type: 'string', length: 100, enumValues: '', validationRule: 'required|max:100', groupCode: 'contract', groupName: '合同', required: true, description: '合同甲方公司全称', sort: 3, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 4, name: '乙方名称', code: 'party_b_name', type: 'string', length: 100, enumValues: '', validationRule: 'required|max:100', groupCode: 'contract', groupName: '合同', required: true, description: '合同乙方公司全称', sort: 4, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 5, name: '合同金额', code: 'contract_amount', type: 'number', length: 0, enumValues: '', validationRule: 'required|numeric|min:0', groupCode: 'contract', groupName: '合同', required: true, description: '合同总金额（元）', sort: 5, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 6, name: '付款方式', code: 'payment_method', type: 'enum', length: 0, enumValues: '电汇,信用证,承兑汇票,现金', validationRule: '', groupCode: 'contract', groupName: '合同', required: false, description: '合同约定的付款方式', sort: 6, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 7, name: '交货地点', code: 'delivery_location', type: 'string', length: 200, enumValues: '', validationRule: '', groupCode: 'contract', groupName: '合同', required: false, description: '货物交付地点', sort: 7, status: 1, createTime: '2026-03-01 10:00:00' },
  // Invoice fields
  { id: 8, name: '发票号码', code: 'invoice_no', type: 'string', length: 20, enumValues: '', validationRule: 'required|max:20', groupCode: 'invoice', groupName: '发票', required: true, description: '增值税发票号码', sort: 1, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 9, name: '开票日期', code: 'invoice_date', type: 'date', length: 0, enumValues: '', validationRule: 'required|date', groupCode: 'invoice', groupName: '发票', required: true, description: '发票开具日期', sort: 2, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 10, name: '购方名称', code: 'buyer_name', type: 'string', length: 100, enumValues: '', validationRule: 'required|max:100', groupCode: 'invoice', groupName: '发票', required: true, description: '购买方公司全称', sort: 3, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 11, name: '销方名称', code: 'seller_name', type: 'string', length: 100, enumValues: '', validationRule: 'required|max:100', groupCode: 'invoice', groupName: '发票', required: true, description: '销售方公司全称', sort: 4, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 12, name: '发票金额', code: 'invoice_amount', type: 'number', length: 0, enumValues: '', validationRule: 'required|numeric|min:0', groupCode: 'invoice', groupName: '发票', required: true, description: '发票总金额（含税）', sort: 5, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 13, name: '税率', code: 'tax_rate', type: 'enum', length: 0, enumValues: '13%,9%,6%,3%,0%', validationRule: 'required', groupCode: 'invoice', groupName: '发票', required: true, description: '适用税率', sort: 6, status: 1, createTime: '2026-03-01 10:00:00' },
  // Bill of Lading fields
  { id: 14, name: '提单号', code: 'bol_no', type: 'string', length: 30, enumValues: '', validationRule: 'required|max:30', groupCode: 'bill_of_lading', groupName: '提单', required: true, description: '海运提单编号', sort: 1, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 15, name: '船名', code: 'vessel_name', type: 'string', length: 50, enumValues: '', validationRule: 'required|max:50', groupCode: 'bill_of_lading', groupName: '提单', required: true, description: '承运船舶名称', sort: 2, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 16, name: '装货港', code: 'loading_port', type: 'string', length: 50, enumValues: '', validationRule: 'required|max:50', groupCode: 'bill_of_lading', groupName: '提单', required: true, description: '货物装运港口', sort: 3, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 17, name: '卸货港', code: 'discharge_port', type: 'string', length: 50, enumValues: '', validationRule: 'required|max:50', groupCode: 'bill_of_lading', groupName: '提单', required: true, description: '货物卸货港口', sort: 4, status: 1, createTime: '2026-03-01 10:00:00' },
  { id: 18, name: '收货人', code: 'consignee', type: 'string', length: 100, enumValues: '', validationRule: 'required|max:100', groupCode: 'bill_of_lading', groupName: '提单', required: true, description: '收货人公司全称', sort: 5, status: 1, createTime: '2026-03-01 10:00:00' }
]

let metaFieldIdSeq = 18
export function nextMetaFieldId() { return ++metaFieldIdSeq }

// ==================== Templates ====================
export const templates = [
  {
    id: 1, name: '销售合同标准模板', documentType: 'contract', description: '适用于销售合同的标准字段模板',
    regions: [
      { id: 'tr1', x: 10, y: 5, width: 60, height: 8, fieldCode: 'contract_no', fieldName: '合同编号', type: 'text' },
      { id: 'tr2', x: 70, y: 5, width: 25, height: 8, fieldCode: 'sign_date', fieldName: '签订日期', type: 'text' },
      { id: 'tr3', x: 5, y: 28, width: 42, height: 20, fieldCode: 'party_a_name', fieldName: '甲方名称', type: 'text' },
      { id: 'tr4', x: 52, y: 28, width: 43, height: 20, fieldCode: 'party_b_name', fieldName: '乙方名称', type: 'text' },
      { id: 'tr5', x: 10, y: 82, width: 80, height: 15, fieldCode: 'contract_amount', fieldName: '合同金额', type: 'table' }
    ],
    status: 1, version: 2, createTime: '2026-04-01 09:00:00', updateTime: '2026-06-15 14:00:00'
  },
  {
    id: 2, name: '增值税发票模板', documentType: 'invoice', description: '增值税专用发票识别模板',
    regions: [
      { id: 'tr6', x: 30, y: 3, width: 40, height: 6, fieldCode: 'invoice_no', fieldName: '发票号码', type: 'text' },
      { id: 'tr7', x: 75, y: 3, width: 20, height: 6, fieldCode: 'invoice_date', fieldName: '开票日期', type: 'text' },
      { id: 'tr8', x: 5, y: 15, width: 42, height: 18, fieldCode: 'buyer_name', fieldName: '购方名称', type: 'text' },
      { id: 'tr9', x: 52, y: 15, width: 43, height: 18, fieldCode: 'seller_name', fieldName: '销方名称', type: 'text' },
      { id: 'tr10', x: 30, y: 88, width: 40, height: 8, fieldCode: 'invoice_amount', fieldName: '发票金额', type: 'key_value' }
    ],
    status: 1, version: 1, createTime: '2026-04-15 10:00:00', updateTime: '2026-04-15 10:00:00'
  },
  {
    id: 3, name: '海运提单模板', documentType: 'bill_of_lading', description: '国际海运提单信息提取模板',
    regions: [
      { id: 'tr11', x: 25, y: 8, width: 50, height: 6, fieldCode: 'bol_no', fieldName: '提单号', type: 'text' },
      { id: 'tr12', x: 5, y: 22, width: 35, height: 10, fieldCode: 'vessel_name', fieldName: '船名', type: 'text' },
      { id: 'tr13', x: 5, y: 38, width: 35, height: 10, fieldCode: 'loading_port', fieldName: '装货港', type: 'text' },
      { id: 'tr14', x: 55, y: 38, width: 40, height: 10, fieldCode: 'discharge_port', fieldName: '卸货港', type: 'text' },
      { id: 'tr15', x: 10, y: 55, width: 80, height: 12, fieldCode: 'consignee', fieldName: '收货人', type: 'text' }
    ],
    status: 1, version: 1, createTime: '2026-05-01 08:00:00', updateTime: '2026-05-01 08:00:00'
  }
]
let templateIdSeq = 3
export function nextTemplateId() { return ++templateIdSeq }

// ==================== Review ====================
export const reviews = Array.from({ length: 10 }, (_, i) => {
  const docIdx = (i + 5) % documents.length
  const totalFields = 8 + Math.floor(Math.random() * 8)
  const reviewedFields = Math.floor(Math.random() * totalFields)
  const approvedFields = Math.floor(Math.random() * reviewedFields)
  const rejectedFields = reviewedFields - approvedFields
  const status = reviewedFields === 0 ? 'pending' : reviewedFields >= totalFields ? (rejectedFields === 0 ? 'approved' : 'rejected') : 'partial'
  return {
    id: i + 1,
    documentId: documents[docIdx].id,
    documentName: documents[docIdx].fileName,
    documentType: documents[docIdx].fileType === 'pdf' ? 'contract' : documents[docIdx].fileType === 'jpg' ? 'invoice' : 'bill_of_lading',
    status,
    totalFields,
    reviewedFields,
    approvedFields,
    rejectedFields,
    reviewer: i < 5 ? 'admin' : undefined,
    reviewTime: i < 5 ? `2026-06-${String(29 - i).padStart(2, '0')} 14:30:00` : undefined,
    createTime: `2026-06-${String(29 - i).padStart(2, '0')} 10:00:00`
  }
})

export const reviewFields: Record<number, any[]> = {}
reviews.forEach(r => {
  reviewFields[r.id] = fieldNames.slice(0, r.totalFields).map((name, idx) => ({
    id: idx + 1,
    fieldName: name,
    fieldCode: `field_${idx + 1}`,
    extractedValue: `${name}: 样本值_${idx + 1}`,
    confidence: parseFloat((0.45 + Math.random() * 0.55).toFixed(2)),
    status: idx < r.approvedFields ? 'approved' : idx < r.reviewedFields ? 'rejected' : 'pending',
    correctedValue: idx < r.reviewedFields && Math.random() > 0.5 ? `修正值_${idx + 1}` : undefined,
    comment: idx < r.reviewedFields && Math.random() > 0.7 ? '人工审核备注' : undefined,
    originalImageRegion: { x: 5, y: 10 + idx * 8, width: 90, height: 6 }
  }))
})

export const reviewHistory: Record<number, any[]> = {}
reviews.forEach(r => {
  reviewHistory[r.id] = r.reviewer ? [
    { id: 1, action: '开始审核', operator: r.reviewer, timestamp: r.reviewTime!.replace('14:30', '14:00'), detail: '开始人工审核' },
    { id: 2, action: '批量确认', operator: r.reviewer, timestamp: r.reviewTime!, detail: `一键通过了 ${r.approvedFields} 个字段` }
  ] : []
})

// ==================== Prompt Versions ====================
export const promptVersions = [
  { id: 1, name: '合同字段提取提示词', version: 'v2.1.0', documentType: 'contract', stage: 'field_extraction', content: '你是一个专业的合同信息提取助手。请从以下合同文档中提取关键字段：\n\n1. 合同编号 (contract_no)\n2. 签订日期 (sign_date)\n3. 甲方名称 (party_a_name)\n4. 乙方名称 (party_b_name)\n5. 合同金额 (contract_amount)\n\n提取规则：\n- 日期统一为 ISO 8601 格式\n- 金额去除货币符号，保留两位小数\n- 如字段不存在，返回 null', author: 'admin', status: 'active', commitMessage: '优化金额提取正则表达式', gitCommitHash: 'a1b2c3d', createTime: '2026-06-20 10:00:00', updateTime: '2026-06-28 16:00:00' },
  { id: 2, name: '合同字段提取提示词', version: 'v2.0.0', documentType: 'contract', stage: 'field_extraction', content: '你是一个合同信息提取助手。请从以下合同文档中提取：\n\n合同编号、签订日期、甲方名称、乙方名称、合同金额\n\n以JSON格式返回。', author: 'admin', status: 'archived', commitMessage: '初版提示词', gitCommitHash: 'e4f5g6h', createTime: '2026-06-15 09:00:00', updateTime: '2026-06-20 09:00:00' },
  { id: 3, name: '版面分析提示词', version: 'v1.0.0', documentType: 'contract', stage: 'layout_analysis', content: '分析以下文档图像的版面结构：\n\n1. 识别所有文本块区域\n2. 标记标题区域\n3. 检测表格区域\n4. 分析阅读顺序\n\n输出每块区域的边界框坐标、类型标签和置信度。', author: 'zhangsan', status: 'active', commitMessage: '初始版本', gitCommitHash: 'i7j8k9l', createTime: '2026-06-10 11:00:00', updateTime: '2026-06-10 11:00:00' },
  { id: 4, name: '发票字段提取提示词', version: 'v1.2.0', documentType: 'invoice', stage: 'field_extraction', content: '你是一个增值税发票信息提取助手。请提取以下字段：\n\n1. 发票号码\n2. 开票日期\n3. 购方名称\n4. 销方名称\n5. 发票金额（含税）\n6. 税率\n\n注意：\n- 区分购方和销方\n- 金额包含所有价税合计\n- 税率可能以百分比或小数表示，统一为百分比', author: 'admin', status: 'active', commitMessage: '增加税率提取逻辑', gitCommitHash: 'm0n1o2p', createTime: '2026-06-22 14:00:00', updateTime: '2026-06-25 09:00:00' },
  { id: 5, name: '数据校验提示词', version: 'v1.0.0', documentType: 'contract', stage: 'validation', content: '对提取的合同字段进行逻辑校验：\n\n1. 合同金额 > 0\n2. 签订日期不能晚于当前日期\n3. 必填字段不能为空\n4. 日期格式统一性检查\n5. 数值字段小数点位数检查\n\n列出所有异常项和警告。', author: 'zhangsan', status: 'draft', commitMessage: '初始版本', gitCommitHash: undefined, createTime: '2026-06-28 08:00:00', updateTime: '2026-06-28 08:00:00' }
]
let promptVersionIdSeq = 5
export function nextPromptVersionId() { return ++promptVersionIdSeq }

// ==================== Evaluation ====================
export const evaluations = [
  { id: 1, taskName: '合同字段提取评测_v2.1', promptVersionId: 1, promptVersionName: '合同字段提取提示词 v2.1.0', datasetSize: 120, accuracy: 0.923, precision: 0.915, recall: 0.938, f1Score: 0.926, status: 'completed', duration: 326, createTime: '2026-06-28 17:00:00' },
  { id: 2, taskName: '合同字段提取评测_v2.0', promptVersionId: 2, promptVersionName: '合同字段提取提示词 v2.0.0', datasetSize: 120, accuracy: 0.875, precision: 0.862, recall: 0.891, f1Score: 0.876, status: 'completed', duration: 298, createTime: '2026-06-20 10:30:00' },
  { id: 3, taskName: '版面分析评测_v1.0', promptVersionId: 3, promptVersionName: '版面分析提示词 v1.0.0', datasetSize: 80, accuracy: 0.891, precision: 0.885, recall: 0.902, f1Score: 0.893, status: 'completed', duration: 412, createTime: '2026-06-15 09:00:00' },
  { id: 4, taskName: '发票字段提取评测_v1.2', promptVersionId: 4, promptVersionName: '发票字段提取提示词 v1.2.0', datasetSize: 95, accuracy: 0.947, precision: 0.951, recall: 0.942, f1Score: 0.946, status: 'completed', duration: 285, createTime: '2026-06-25 10:00:00' }
]
let evalIdSeq = 4
export function nextEvalId() { return ++evalIdSeq }

// ==================== Finetune Tasks ====================
export const finetuneTasks = [
  { id: 1, taskName: '合同版面分析_LoRA_Qwen2-7B', baseModel: 'Qwen2-7B', datasetName: 'contract_layout_v2', datasetSize: 5000, status: 'completed', progress: 100, epochs: 3, currentEpoch: 3, learningRate: 1e-4, loss: 0.023, metrics: { accuracy: 0.945, f1: 0.938 }, outputModelName: 'contract-layout-lora-v1', startTime: '2026-06-26 08:00:00', endTime: '2026-06-26 14:30:00', duration: 23400, createTime: '2026-06-25 16:00:00' },
  { id: 2, taskName: '发票字段提取_LoRA_Qwen2-14B', baseModel: 'Qwen2-14B', datasetName: 'invoice_extraction_v1', datasetSize: 3200, status: 'running', progress: 67, epochs: 3, currentEpoch: 2, learningRate: 5e-5, loss: 0.089, metrics: undefined, outputModelName: undefined, startTime: '2026-06-29 06:00:00', endTime: undefined, duration: undefined, createTime: '2026-06-28 10:00:00' },
  { id: 3, taskName: '提单OCR_LoRA_Llama3-8B', baseModel: 'Llama3-8B', datasetName: 'bol_ocr_v1', datasetSize: 2100, status: 'pending', progress: 0, epochs: 5, currentEpoch: 0, learningRate: 2e-4, loss: undefined, metrics: undefined, outputModelName: undefined, startTime: undefined, endTime: undefined, duration: undefined, createTime: '2026-06-29 09:00:00' },
  { id: 4, taskName: '合同校验_LoRA_Qwen2-7B_v2', baseModel: 'Qwen2-7B', datasetName: 'contract_validation_v3', datasetSize: 6800, status: 'failed', progress: 45, epochs: 3, currentEpoch: 2, learningRate: 1e-4, loss: 0.452, metrics: undefined, outputModelName: undefined, startTime: '2026-06-27 12:00:00', endTime: '2026-06-28 03:00:00', duration: 54000, errorMessage: 'GPU内存不足 (OOM at batch_size=16)，建议降低batch_size或使用gradient_accumulation', createTime: '2026-06-27 10:00:00' }
]
let finetuneTaskIdSeq = 4
export function nextFinetuneTaskId() { return ++finetuneTaskIdSeq }

// ==================== Model Assets ====================
export const baseModels = [
  { id: 1, name: 'Qwen2-7B-Instruct', version: 'v1.0', type: 'base', description: '通义千问2 70亿参数指令微调版', size: '7B', status: 'active', framework: 'transformers', metrics: { accuracy: 0.892, f1: 0.885, inferenceTime: 45.2 }, createTime: '2026-05-01 08:00:00' },
  { id: 2, name: 'Qwen2-14B-Instruct', version: 'v1.0', type: 'base', description: '通义千问2 140亿参数指令微调版', size: '14B', status: 'active', framework: 'transformers', metrics: { accuracy: 0.915, f1: 0.908, inferenceTime: 78.5 }, createTime: '2026-05-01 08:00:00' },
  { id: 3, name: 'Llama3-8B-Instruct', version: 'v1.0', type: 'base', description: 'Meta Llama3 80亿参数指令微调版', size: '8B', status: 'active', framework: 'transformers', metrics: { accuracy: 0.871, f1: 0.863, inferenceTime: 52.1 }, createTime: '2026-05-10 10:00:00' }
]

export const loraAdapters = [
  { id: 4, name: 'contract-layout-lora-v1', version: 'v1.0', type: 'lora', description: '合同版面分析LoRA适配器 (r=16, alpha=32)', size: '7B', status: 'active', framework: 'peft', metrics: { accuracy: 0.945, f1: 0.938, inferenceTime: 48.3 }, createTime: '2026-06-26 14:30:00' },
  { id: 5, name: 'invoice-extract-lora-v1', version: 'v0.9-beta', type: 'lora', description: '发票字段提取LoRA适配器 (r=16, alpha=32)', size: '14B', status: 'training', framework: 'peft', metrics: undefined, createTime: '2026-06-29 06:00:00' },
  { id: 6, name: 'table-detect-lora-v2', version: 'v2.0', type: 'lora', description: '通用表格检测LoRA适配器 (r=8, alpha=16)', size: '7B', status: 'inactive', framework: 'peft', metrics: { accuracy: 0.902, f1: 0.895, inferenceTime: 35.7 }, createTime: '2026-06-15 09:00:00' }
]

export const datasets = [
  { id: 7, name: 'contract_layout_v2', version: 'v2.0', type: 'dataset', description: '合同版面分析数据集 (5000条标注)', size: '5K', status: 'active', metrics: undefined, createTime: '2026-06-20 16:00:00' },
  { id: 8, name: 'invoice_extraction_v1', version: 'v1.0', type: 'dataset', description: '发票字段提取数据集 (3200条标注)', size: '3.2K', status: 'active', metrics: undefined, createTime: '2026-06-22 11:00:00' },
  { id: 9, name: 'bol_ocr_v1', version: 'v1.0', type: 'dataset', description: '提单OCR识别数据集 (2100条标注)', size: '2.1K', status: 'active', metrics: undefined, createTime: '2026-06-25 14:00:00' },
  { id: 10, name: 'contract_validation_v3', version: 'v3.0', type: 'dataset', description: '合同校验数据集 (6800条标注)', size: '6.8K', status: 'active', metrics: undefined, createTime: '2026-06-27 08:00:00' }
]

export const modelBindings = [
  { id: 1, baseModelId: 1, baseModelName: 'Qwen2-7B-Instruct', loraAdapterId: 4, loraAdapterName: 'contract-layout-lora-v1', datasetId: 7, datasetName: 'contract_layout_v2', bindingName: '合同版面分析完整模型', status: 'active', createTime: '2026-06-27 09:00:00' },
  { id: 2, baseModelId: 2, baseModelName: 'Qwen2-14B-Instruct', loraAdapterId: 5, loraAdapterName: 'invoice-extract-lora-v1', datasetId: 8, datasetName: 'invoice_extraction_v1', bindingName: '发票字段提取模型(训练中)', status: 'inactive', createTime: '2026-06-29 07:00:00' }
]

export { metaGroups }

// ==================== Current User Session ====================
// This stores the current "logged in" user (simulated)
export let currentMockUser: any = null

export function setCurrentMockUser(user: any) {
  currentMockUser = user
}

export function getCurrentMockUser() {
  return currentMockUser
}

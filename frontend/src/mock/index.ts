import request from '@/utils/request'
import { users, roles, permissionTree, operationLogs, agentTraces, flattenPermissions, documents, pipelines, pipelineStateGraphs, extractionResults, validationReports, reflectionData, layoutData, ocrData, tableData, metadataFields, metaGroups, templates, reviews, reviewFields, reviewHistory, promptVersions, evaluations, finetuneTasks, baseModels, loraAdapters, datasets, modelBindings } from './data'
import { setCurrentMockUser, getCurrentMockUser } from './data'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

// Mock response wrapper
function ok(data: any) {
  return { code: 200, data, message: 'success' }
}

function fail(message: string, code = 400) {
  return { code, data: null, message }
}

function resolve(config: InternalAxiosRequestConfig, data: any): Promise<AxiosResponse> {
  return Promise.resolve({
    data,
    status: 200,
    statusText: 'OK',
    headers: {} as any,
    config
  } as AxiosResponse)
}

// Simulated network delay
const delay = (ms = 200) => new Promise(r => setTimeout(r, ms))

export function setupMock() {
  const originalAdapter: AxiosAdapter = request.defaults.adapter as AxiosAdapter

  request.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
    const url = config.url || ''
    const method = (config.method || 'get').toLowerCase()
    let body: any = {}
    try { body = JSON.parse(config.data || '{}') } catch { /* ignore */ }
    const query = config.params || {}

    await delay(100 + Math.random() * 200)

    // ==================== Auth ====================
    if (url === '/auth/login' && method === 'post') {
      const { username, password } = body
      const user = users.find(u => u.username === username)
      if (!user || password.length < 6) {
        return resolve(config, fail('用户名或密码错误', 401))
      }
      if (user.status === 0) {
        return resolve(config, fail('账号已被禁用', 403))
      }
      setCurrentMockUser(user)
      return resolve(config, {
        code: 200,
        token: 'mock-jwt-token-' + Date.now(),
        refreshToken: 'mock-refresh-token-' + Date.now(),
        userInfo: {
          id: user.id,
          username: user.username,
          nickname: user.nickname,
          avatar: user.avatar
        },
        message: 'success'
      })
    }

    if (url === '/auth/logout' && method === 'post') {
      setCurrentMockUser(null)
      return resolve(config, ok(null))
    }

    if (url === '/auth/refresh' && method === 'post') {
      return resolve(config, {
        code: 200,
        token: 'mock-jwt-token-' + Date.now(),
        refreshToken: 'mock-refresh-token-' + Date.now(),
        message: 'success'
      })
    }

    if (url === '/auth/current-user' && method === 'get') {
      const user = getCurrentMockUser()
      if (!user) return resolve(config, fail('未登录', 401))
      const roleData = roles.find(r => r.id === user.roleIds?.[0])
      const allPerms = flattenPermissions(permissionTree)
      const permCodes = roleData ? roleData.permissionIds.map((pid: number) => {
        const p = allPerms.find((x: any) => x.id === pid)
        return p?.code || ''
      }).filter(Boolean) : []
      return resolve(config, ok({
        userId: user.id,
        username: user.username,
        nickname: user.nickname,
        avatar: user.avatar,
        roles: [roleData?.code || 'user'],
        permissions: permCodes
      }))
    }

    // ==================== Dashboard ====================
    if (url === '/dashboard/stats' && method === 'get') {
      return resolve(config, ok({
        userCount: users.length,
        todayOperations: 87,
        agentCalls: 23,
        activeAgents: 3,
        autoPassRate: 87.5,
        manualCorrectionRate: 12.5,
        costSavings: 158000,
        avgProcessingTime: 3.2
      }))
    }

    if (url === '/dashboard/daily-processing' && method === 'get') {
      return resolve(config, ok([
        { date: '06/23', count: 18, autoPassed: 15, manualCorrected: 3 },
        { date: '06/24', count: 22, autoPassed: 19, manualCorrected: 3 },
        { date: '06/25', count: 15, autoPassed: 13, manualCorrected: 2 },
        { date: '06/26', count: 28, autoPassed: 25, manualCorrected: 3 },
        { date: '06/27', count: 20, autoPassed: 17, manualCorrected: 3 },
        { date: '06/28', count: 25, autoPassed: 22, manualCorrected: 3 },
        { date: '06/29', count: 19, autoPassed: 17, manualCorrected: 2 }
      ]))
    }

    if (url === '/dashboard/doc-type-distribution' && method === 'get') {
      return resolve(config, ok([
        { type: '合同', value: 45 },
        { type: '发票', value: 62 },
        { type: '提单', value: 28 },
        { type: '报关单', value: 15 },
        { type: '其他', value: 8 }
      ]))
    }

    if (url === '/dashboard/operation-trend' && method === 'get') {
      return resolve(config, ok([
        { date: '06/23', count: 42 },
        { date: '06/24', count: 55 },
        { date: '06/25', count: 38 },
        { date: '06/26', count: 63 },
        { date: '06/27', count: 48 },
        { date: '06/28', count: 57 },
        { date: '06/29', count: 53 }
      ]))
    }

    if (url === '/dashboard/operation-type-distribution' && method === 'get') {
      return resolve(config, ok([
        { type: '查询', value: 120 },
        { type: '新增', value: 45 },
        { type: '修改', value: 38 },
        { type: '删除', value: 15 },
        { type: '导出', value: 22 },
        { type: '登录', value: 98 }
      ]))
    }

    // ==================== Users ====================
    if (url === '/users' && method === 'get') {
      let list = [...users]
      if (query.username) list = list.filter(u => u.username.includes(query.username))
      if (query.email) list = list.filter(u => u.email.includes(query.email))
      if (query.status !== undefined && query.status !== '') list = list.filter(u => u.status === Number(query.status))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/users\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const user = users.find(u => u.id === id)
      return user ? resolve(config, ok(user)) : resolve(config, fail('用户不存在', 404))
    }

    if (url === '/users' && method === 'post') {
      const newUser = {
        id: users.length + 1,
        ...body,
        status: 1,
        roleIds: body.roleIds || [2],
        roleNames: body.roleIds?.map((rid: number) => roles.find(r => r.id === rid)?.name).filter(Boolean) || ['普通用户'],
        createTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
      }
      users.push(newUser)
      return resolve(config, ok(newUser))
    }

    if (url.match(/^\/users\/\d+$/) && method === 'put') {
      const id = Number(url.split('/').pop())
      const idx = users.findIndex(u => u.id === id)
      if (idx === -1) return resolve(config, fail('用户不存在', 404))
      Object.assign(users[idx], body, { id })
      return resolve(config, ok(users[idx]))
    }

    if (url.match(/^\/users\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      const idx = users.findIndex(u => u.id === id)
      if (idx > -1) users.splice(idx, 1)
      return resolve(config, ok(null))
    }

    if (url.match(/^\/users\/\d+\/status$/) && method === 'patch') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const user = users.find(u => u.id === id)
      if (user) user.status = body.status
      return resolve(config, ok(user))
    }

    if (url.match(/^\/users\/\d+\/roles$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const user = users.find(u => u.id === id)
      if (user) {
        user.roleIds = body.roleIds
        user.roleNames = body.roleIds.map((rid: number) => roles.find(r => r.id === rid)?.name).filter(Boolean)
      }
      return resolve(config, ok(user))
    }

    // ==================== Roles ====================
    if (url === '/roles' && method === 'get') {
      let list = [...roles]
      if (query.name) list = list.filter(r => r.name.includes(query.name))
      if (query.code) list = list.filter(r => r.code.includes(query.code))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url === '/roles/all' && method === 'get') {
      return resolve(config, ok(roles.map(r => ({ id: r.id, name: r.name, code: r.code }))))
    }

    if (url === '/roles' && method === 'post') {
      const newRole = { id: roles.length + 1, ...body, status: 1, permissionIds: [], userCount: 0, createTime: new Date().toISOString().replace('T', ' ').substring(0, 19) }
      roles.push(newRole)
      return resolve(config, ok(newRole))
    }

    if (url.match(/^\/roles\/\d+$/) && method === 'put') {
      const id = Number(url.split('/').pop())
      const idx = roles.findIndex(r => r.id === id)
      if (idx > -1) Object.assign(roles[idx], body, { id })
      return resolve(config, ok(roles[idx]))
    }

    if (url.match(/^\/roles\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      const idx = roles.findIndex(r => r.id === id)
      if (idx > -1) roles.splice(idx, 1)
      return resolve(config, ok(null))
    }

    if (url.match(/^\/roles\/\d+\/permissions$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const role = roles.find(r => r.id === id)
      if (role) role.permissionIds = body.permissionIds
      return resolve(config, ok(role))
    }

    // ==================== Permissions ====================
    if (url === '/permissions' && method === 'get') {
      return resolve(config, ok(flattenPermissions(permissionTree)))
    }

    if (url === '/permissions/tree' && method === 'get') {
      return resolve(config, ok(permissionTree))
    }

    if (url === '/permissions' && method === 'post') {
      const newPerm: any = { id: Math.floor(Math.random() * 1000) + 100, ...body, status: 1, children: [] }
      // Find parent and add child
      function addToNode(nodes: any[]): boolean {
        for (const n of nodes) {
          if (n.id === newPerm.parentId) {
            if (!n.children) n.children = []
            n.children.push(newPerm)
            return true
          }
          if (n.children && addToNode(n.children)) return true
        }
        return false
      }
      if (!newPerm.parentId || !addToNode(permissionTree)) {
        newPerm.parentId = null
        permissionTree.push(newPerm)
      }
      return resolve(config, ok(newPerm))
    }

    if (url.match(/^\/permissions\/\d+$/) && method === 'put') {
      const id = Number(url.split('/').pop())
      function updateNode(nodes: any[]): boolean {
        for (let i = 0; i < nodes.length; i++) {
          if (nodes[i].id === id) {
            Object.assign(nodes[i], body, { id })
            return true
          }
          if (nodes[i].children && updateNode(nodes[i].children)) return true
        }
        return false
      }
      updateNode(permissionTree)
      return resolve(config, ok(body))
    }

    if (url.match(/^\/permissions\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      function removeNode(nodes: any[]): boolean {
        for (let i = 0; i < nodes.length; i++) {
          if (nodes[i].id === id) { nodes.splice(i, 1); return true }
          if (nodes[i].children && removeNode(nodes[i].children)) return true
        }
        return false
      }
      removeNode(permissionTree)
      return resolve(config, ok(null))
    }

    // ==================== Operation Logs ====================
    if (url === '/operation-logs' && method === 'get') {
      let list = [...operationLogs]
      if (query.startTime) list = list.filter(l => l.createTime >= query.startTime)
      if (query.endTime) list = list.filter(l => l.createTime <= query.endTime)
      if (query.username) list = list.filter(l => l.username.includes(query.username))
      if (query.operationType && query.operationType.length > 0) {
        const types = Array.isArray(query.operationType) ? query.operationType : [query.operationType]
        list = list.filter(l => types.includes(l.operationType))
      }
      if (query.module) list = list.filter(l => l.module.includes(query.module))
      if (query.status) list = list.filter(l => l.status === query.status)
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/operation-logs\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const log = operationLogs.find(l => l.id === id)
      return log ? resolve(config, ok(log)) : resolve(config, fail('日志不存在', 404))
    }

    if (url === '/operation-logs/types' && method === 'get') {
      return resolve(config, ok([
        { value: 'login', label: '登录' },
        { value: 'query', label: '查询' },
        { value: 'create', label: '新增' },
        { value: 'update', label: '修改' },
        { value: 'delete', label: '删除' },
        { value: 'export', label: '导出' },
        { value: 'grant', label: '授权' },
        { value: 'other', label: '其他' }
      ]))
    }

    if (url === '/operation-logs/export' && method === 'get') {
      return resolve(config, ok(null))
    }

    // ==================== Agent Traces ====================
    if (url === '/agent-traces' && method === 'get') {
      let list = [...agentTraces]
      if (query.startTime) list = list.filter(t => t.startTime >= query.startTime)
      if (query.endTime) list = list.filter(t => t.startTime <= query.endTime)
      if (query.username) list = list.filter(t => t.username.includes(query.username))
      if (query.status) list = list.filter(t => t.status === query.status)
      if (query.traceId) list = list.filter(t => t.traceId.includes(query.traceId))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/agent-traces\/[^/]+\/tree$/) && method === 'get') {
      const traceId = url.split('/')[url.split('/').length - 2]
      const trace = agentTraces.find(t => t.traceId === traceId)
      return trace ? resolve(config, ok(trace.nodes)) : resolve(config, ok([]))
    }

    if (url.match(/^\/agent-traces\/[^/]+$/) && method === 'get') {
      const traceId = url.split('/').pop()
      const trace = agentTraces.find(t => t.traceId === traceId)
      return trace ? resolve(config, ok(trace)) : resolve(config, fail('Trace不存在', 404))
    }

    // ==================== Documents ====================
    if (url === '/documents' && method === 'get') {
      let list = [...documents]
      if (query.fileType) list = list.filter(d => d.fileType === query.fileType)
      if (query.status) list = list.filter(d => d.status === query.status)
      if (query.startDate) list = list.filter(d => d.uploadTime >= query.startDate)
      if (query.endDate) list = list.filter(d => d.uploadTime <= query.endDate + ' 23:59:59')
      if (query.keyword) list = list.filter(d => d.fileName.includes(query.keyword))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url === '/documents/upload' && method === 'post') {
      const newDoc = {
        id: documents.length + 1,
        fileName: 'uploaded_document_' + Date.now() + '.pdf',
        fileType: 'pdf' as const,
        fileSize: Math.floor(Math.random() * 500000) + 100000,
        uploadTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
        status: 'pending' as const,
        pageCount: Math.floor(Math.random() * 10) + 1,
        uploadUser: 'admin'
      }
      documents.unshift(newDoc)
      return resolve(config, ok(newDoc))
    }

    if (url.match(/^\/documents\/\d+\/preprocess$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const doc = documents.find(d => d.id === id)
      if (doc) doc.status = 'preprocessing'
      setTimeout(() => { if (doc) doc.status = 'completed' }, 3000)
      return resolve(config, ok({ message: '预处理任务已提交' }))
    }

    if (url.match(/^\/documents\/\d+\/layout$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(layoutData[id] || { imageUrl: '', regions: [] }))
    }

    if (url.match(/^\/documents\/\d+\/ocr$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(ocrData[id] || { totalChars: 0, averageConfidence: 0, language: 'zh-CN', entries: [] }))
    }

    if (url.match(/^\/documents\/\d+\/table$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(tableData[id] || { confidence: 0, originalTable: { headers: [], rows: [] }, extractedTable: { headers: [], rows: [] } }))
    }

    if (url.match(/^\/documents\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const doc = documents.find(d => d.id === id)
      return doc ? resolve(config, ok(doc)) : resolve(config, fail('文档不存在', 404))
    }

    if (url.match(/^\/documents\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      const idx = documents.findIndex(d => d.id === id)
      if (idx > -1) documents.splice(idx, 1)
      return resolve(config, ok(null))
    }

    // ==================== Pipeline ====================
    if (url === '/pipeline' && method === 'get') {
      let list = [...pipelines]
      if (query.status) list = list.filter(p => p.status === query.status)
      if (query.keyword) list = list.filter(p => p.documentName.includes(query.keyword))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/pipeline\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const pipeline = pipelines.find(p => p.id === id)
      if (!pipeline) return resolve(config, fail('流水线不存在', 404))
      return resolve(config, ok({
        ...pipeline,
        stateGraph: pipelineStateGraphs[id],
        extractionResults: extractionResults[id] || [],
        validationReport: validationReports[id] || [],
        reflectionData: reflectionData[id] || []
      }))
    }

    if (url.match(/^\/pipeline\/\d+\/extraction$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(extractionResults[id] || []))
    }

    if (url.match(/^\/pipeline\/\d+\/validation$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(validationReports[id] || []))
    }

    if (url.match(/^\/pipeline\/\d+\/reflection$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(reflectionData[id] || []))
    }

    // ==================== Metadata ====================
    if (url === '/metadata/fields' && method === 'get') {
      let list = [...metadataFields]
      if (query.groupCode) list = list.filter(f => f.groupCode === query.groupCode)
      if (query.keyword) list = list.filter(f => f.name.includes(query.keyword) || f.code.includes(query.keyword))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/metadata\/fields\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const field = metadataFields.find(f => f.id === id)
      return field ? resolve(config, ok(field)) : resolve(config, fail('字段不存在', 404))
    }

    if (url === '/metadata/fields' && method === 'post') {
      const newField = { id: metadataFields.length + 1, ...body, status: 1, createTime: new Date().toISOString().replace('T', ' ').substring(0, 19) }
      metadataFields.push(newField)
      return resolve(config, ok(newField))
    }

    if (url.match(/^\/metadata\/fields\/\d+$/) && method === 'put') {
      const id = Number(url.split('/').pop())
      const idx = metadataFields.findIndex(f => f.id === id)
      if (idx > -1) Object.assign(metadataFields[idx], body, { id })
      return resolve(config, ok(metadataFields[idx]))
    }

    if (url.match(/^\/metadata\/fields\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      const idx = metadataFields.findIndex(f => f.id === id)
      if (idx > -1) metadataFields.splice(idx, 1)
      return resolve(config, ok(null))
    }

    if (url === '/metadata/groups' && method === 'get') {
      const counts: Record<string, number> = {}
      metadataFields.forEach(f => { counts[f.groupCode] = (counts[f.groupCode] || 0) + 1 })
      return resolve(config, ok(metaGroups.map(g => ({ ...g, fieldCount: counts[g.code] || 0 }))))
    }

    if (url === '/metadata/export' && method === 'post') {
      return resolve(config, ok({ taskId: Date.now(), format: body.format, status: 'processing', estimatedTime: '30s' }))
    }

    if (url === '/metadata/export/history' && method === 'get') {
      return resolve(config, ok({
        records: [
          { id: 1, exportTime: '2026-06-29 14:30:00', format: 'JSON', recordCount: 156, status: 'completed', downloadUrl: '/exports/data_20260629.json' },
          { id: 2, exportTime: '2026-06-28 10:00:00', format: 'Excel', recordCount: 203, status: 'completed', downloadUrl: '/exports/data_20260628.xlsx' }
        ],
        total: 2
      }))
    }

    if (url === '/metadata/export/webhook' && method === 'get') {
      return resolve(config, ok({ url: 'https://api.example.com/webhook/ai-platform', secret: 'wh_sec_***', enabled: true, events: ['export.completed', 'field.updated'] }))
    }

    if (url === '/metadata/export/webhook' && method === 'put') {
      return resolve(config, ok({ ...body, updatedAt: new Date().toISOString() }))
    }

    // ==================== Templates ====================
    if (url === '/templates' && method === 'get') {
      let list = [...templates]
      if (query.documentType) list = list.filter(t => t.documentType === query.documentType)
      if (query.keyword) list = list.filter(t => t.name.includes(query.keyword))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      // Return without full regions for list view
      return resolve(config, ok({ records: list.slice(start, start + pageSize).map(t => ({ ...t, regionCount: t.regions.length, regions: undefined })), total: list.length }))
    }

    if (url.match(/^\/templates\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const template = templates.find(t => t.id === id)
      return template ? resolve(config, ok(template)) : resolve(config, fail('模板不存在', 404))
    }

    if (url === '/templates' && method === 'post') {
      const newTemplate = { id: templates.length + 1, ...body, status: 1, version: 1, regions: [], createTime: new Date().toISOString().replace('T', ' ').substring(0, 19), updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19) }
      templates.push(newTemplate)
      return resolve(config, ok(newTemplate))
    }

    if (url.match(/^\/templates\/\d+$/) && method === 'put') {
      const id = Number(url.split('/').pop())
      const idx = templates.findIndex(t => t.id === id)
      if (idx > -1) Object.assign(templates[idx], body, { id, updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19) })
      return resolve(config, ok(templates[idx]))
    }

    if (url.match(/^\/templates\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      const idx = templates.findIndex(t => t.id === id)
      if (idx > -1) templates.splice(idx, 1)
      return resolve(config, ok(null))
    }

    // ==================== Review ====================
    if (url === '/review' && method === 'get') {
      let list = [...reviews]
      if (query.status) list = list.filter(r => r.status === query.status)
      if (query.keyword) list = list.filter(r => r.documentName.includes(query.keyword))
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/review\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const review = reviews.find(r => r.id === id)
      return review ? resolve(config, ok(review)) : resolve(config, fail('审核记录不存在', 404))
    }

    if (url.match(/^\/review\/\d+\/fields$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(reviewFields[id] || []))
    }

    if (url.match(/^\/review\/\d+\/fields\/\d+\/approve$/) && method === 'post') {
      const parts = url.split('/')
      const reviewId = Number(parts[parts.length - 4])
      const fieldId = Number(parts[parts.length - 2])
      const fields = reviewFields[reviewId]
      if (fields) {
        const field = fields.find((f: any) => f.id === fieldId)
        if (field) { field.status = 'approved'; reviews[reviewId - 1].approvedFields++; reviews[reviewId - 1].reviewedFields++; }
      }
      return resolve(config, ok({ message: '字段已确认' }))
    }

    if (url.match(/^\/review\/\d+\/fields\/\d+\/reject$/) && method === 'post') {
      const parts = url.split('/')
      const reviewId = Number(parts[parts.length - 4])
      const fieldId = Number(parts[parts.length - 2])
      const fields = reviewFields[reviewId]
      if (fields) {
        const field = fields.find((f: any) => f.id === fieldId)
        if (field) { field.status = 'rejected'; field.comment = body.comment; reviews[reviewId - 1].rejectedFields++; reviews[reviewId - 1].reviewedFields++; }
      }
      return resolve(config, ok({ message: '字段已驳回' }))
    }

    if (url.match(/^\/review\/\d+\/fields\/\d+\/correct$/) && method === 'post') {
      const parts = url.split('/')
      const reviewId = Number(parts[parts.length - 4])
      const fieldId = Number(parts[parts.length - 2])
      const fields = reviewFields[reviewId]
      if (fields) {
        const field = fields.find((f: any) => f.id === fieldId)
        if (field) { field.status = 'corrected'; field.correctedValue = body.correctedValue; reviews[reviewId - 1].reviewedFields++; }
      }
      return resolve(config, ok({ message: '字段已修正' }))
    }

    if (url.match(/^\/review\/\d+\/batch-approve$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const fields = reviewFields[id]
      if (fields) {
        body.fieldIds.forEach((fid: number) => {
          const f = fields.find((x: any) => x.id === fid)
          if (f && f.status === 'pending') { f.status = 'approved'; reviews[id - 1].approvedFields++; reviews[id - 1].reviewedFields++; }
        })
      }
      return resolve(config, ok({ message: `已批量确认 ${body.fieldIds.length} 个字段` }))
    }

    if (url.match(/^\/review\/\d+\/batch-reject$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const fields = reviewFields[id]
      if (fields) {
        body.fieldIds.forEach((fid: number) => {
          const f = fields.find((x: any) => x.id === fid)
          if (f && f.status === 'pending') { f.status = 'rejected'; reviews[id - 1].rejectedFields++; reviews[id - 1].reviewedFields++; }
        })
      }
      return resolve(config, ok({ message: `已批量驳回 ${body.fieldIds.length} 个字段` }))
    }

    if (url.match(/^\/review\/\d+\/history$/) && method === 'get') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      return resolve(config, ok(reviewHistory[id] || []))
    }

    // ==================== Prompt Versions ====================
    if (url === '/prompt/versions' && method === 'get') {
      let list = [...promptVersions]
      if (query.documentType) list = list.filter(p => p.documentType === query.documentType)
      if (query.stage) list = list.filter(p => p.stage === query.stage)
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/prompt\/versions\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const pv = promptVersions.find(p => p.id === id)
      return pv ? resolve(config, ok(pv)) : resolve(config, fail('版本不存在', 404))
    }

    if (url === '/prompt/versions' && method === 'post') {
      const newPV = { id: promptVersions.length + 1, ...body, status: 'draft', createTime: new Date().toISOString().replace('T', ' ').substring(0, 19), updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19) }
      promptVersions.push(newPV)
      return resolve(config, ok(newPV))
    }

    if (url.match(/^\/prompt\/versions\/\d+$/) && method === 'put') {
      const id = Number(url.split('/').pop())
      const idx = promptVersions.findIndex(p => p.id === id)
      if (idx > -1) Object.assign(promptVersions[idx], body, { id, updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19) })
      return resolve(config, ok(promptVersions[idx]))
    }

    if (url.match(/^\/prompt\/versions\/\d+\/rollback$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const pv = promptVersions.find(p => p.id === id)
      if (!pv) return resolve(config, fail('版本不存在', 404))
      pv.status = 'active'
      // Also set others with same name to archived
      promptVersions.forEach(p => { if (p.name === pv.name && p.id !== pv.id) p.status = 'archived' })
      return resolve(config, ok({ message: '已回滚到版本 ' + pv.version }))
    }

    if (url === '/prompt/versions/diff' && method === 'get') {
      const vA = promptVersions.find(p => p.id === Number(query.versionA))
      const vB = promptVersions.find(p => p.id === Number(query.versionB))
      if (!vA || !vB) return resolve(config, fail('版本不存在', 404))
      const linesA = vA.content.split('\n')
      const linesB = vB.content.split('\n')
      const maxLines = Math.max(linesA.length, linesB.length)
      const diffLines = []
      for (let i = 0; i < maxLines; i++) {
        const lineA = linesA[i] || ''
        const lineB = linesB[i] || ''
        diffLines.push({
          type: lineA === lineB ? 'unchanged' : !lineA ? 'added' : !lineB ? 'removed' : 'modified',
          lineNumber: i + 1,
          oldContent: lineA,
          newContent: lineB
        })
      }
      return resolve(config, ok({ versionA: vA.version, versionB: vB.version, diffLines }))
    }

    // ==================== Prompt Evaluation ====================
    if (url === '/prompt/evaluation' && method === 'get') {
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: evaluations.slice(start, start + pageSize), total: evaluations.length }))
    }

    if (url === '/prompt/evaluation/run' && method === 'post') {
      const newEval = {
        id: evaluations.length + 1,
        taskName: '评测_' + new Date().toISOString().substring(0, 16),
        promptVersionId: body.promptVersionId,
        promptVersionName: promptVersions.find(p => p.id === body.promptVersionId)?.name || '',
        datasetSize: 100,
        accuracy: parseFloat((0.85 + Math.random() * 0.14).toFixed(3)),
        precision: parseFloat((0.84 + Math.random() * 0.15).toFixed(3)),
        recall: parseFloat((0.83 + Math.random() * 0.16).toFixed(3)),
        f1Score: parseFloat((0.84 + Math.random() * 0.15).toFixed(3)),
        status: 'completed',
        duration: Math.floor(Math.random() * 300) + 100,
        createTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
      }
      evaluations.unshift(newEval)
      return resolve(config, ok(newEval))
    }

    // ==================== Finetune Tasks ====================
    if (url === '/finetune/tasks' && method === 'get') {
      let list = [...finetuneTasks]
      if (query.status) list = list.filter(t => t.status === query.status)
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/finetune\/tasks\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const task = finetuneTasks.find(t => t.id === id)
      return task ? resolve(config, ok(task)) : resolve(config, fail('任务不存在', 404))
    }

    if (url === '/finetune/tasks' && method === 'post') {
      const newTask = {
        id: finetuneTasks.length + 1,
        taskName: body.taskName,
        baseModel: body.baseModel,
        datasetName: body.datasetName,
        datasetSize: Math.floor(Math.random() * 5000) + 1000,
        status: 'pending',
        progress: 0,
        epochs: body.epochs || 3,
        currentEpoch: 0,
        learningRate: body.learningRate || 1e-4,
        createTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
      }
      finetuneTasks.push(newTask as any)
      return resolve(config, ok(newTask))
    }

    if (url.match(/^\/finetune\/tasks\/\d+\/cancel$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      const task = finetuneTasks.find(t => t.id === id)
      if (task && (task.status === 'pending' || task.status === 'running')) task.status = 'cancelled'
      return resolve(config, ok({ message: '任务已取消' }))
    }

    // ==================== Model Assets ====================
    if (url === '/models/assets' && method === 'get') {
      let list = [...baseModels, ...loraAdapters, ...datasets]
      if (query.type) list = list.filter(m => m.type === query.type)
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: list.slice(start, start + pageSize), total: list.length }))
    }

    if (url.match(/^\/models\/assets\/\d+$/) && method === 'get') {
      const id = Number(url.split('/').pop())
      const all = [...baseModels, ...loraAdapters, ...datasets]
      const model = all.find(m => m.id === id)
      return model ? resolve(config, ok(model)) : resolve(config, fail('资产不存在', 404))
    }

    if (url === '/models/bindings' && method === 'get') {
      const page = Number(query.page) || 1
      const pageSize = Number(query.pageSize) || 10
      const start = (page - 1) * pageSize
      return resolve(config, ok({ records: modelBindings.slice(start, start + pageSize), total: modelBindings.length }))
    }

    if (url === '/models/bindings' && method === 'post') {
      const newBinding = {
        id: modelBindings.length + 1,
        baseModelId: body.baseModelId,
        baseModelName: baseModels.find(m => m.id === body.baseModelId)?.name || '',
        loraAdapterId: body.loraAdapterId,
        loraAdapterName: loraAdapters.find(m => m.id === body.loraAdapterId)?.name || '',
        datasetId: body.datasetId,
        datasetName: datasets.find(m => m.id === body.datasetId)?.name || '',
        bindingName: body.bindingName,
        status: 'inactive',
        createTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
      }
      modelBindings.push(newBinding)
      return resolve(config, ok(newBinding))
    }

    if (url.match(/^\/models\/bindings\/\d+$/) && method === 'delete') {
      const id = Number(url.split('/').pop())
      const idx = modelBindings.findIndex(b => b.id === id)
      if (idx > -1) modelBindings.splice(idx, 1)
      return resolve(config, ok(null))
    }

    if (url.match(/^\/models\/bindings\/\d+\/activate$/) && method === 'post') {
      const id = Number(url.split('/')[url.split('/').length - 2])
      modelBindings.forEach(b => { b.status = b.id === id ? 'active' : 'inactive' })
      return resolve(config, ok({ message: '绑定已激活' }))
    }

    // ==================== Fall through to real API ====================
    try {
      const response = await (originalAdapter as Function)(config)
      return response as AxiosResponse
    } catch (error) {
      return Promise.reject(error)
    }
  }

  console.log('[Mock] Mock API layer enabled — 默认账号: admin / 任意6位以上密码')
}

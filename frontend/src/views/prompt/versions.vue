<template>
  <div class="page-container prompt-versions">
    <div class="page-header">
      <h2>提示词版本管理</h2>
    </div>

    <!-- Search Form -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="文档类型">
          <el-select v-model="query.documentType" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理阶段">
          <el-select v-model="query.stage" placeholder="全部" clearable style="width: 180px">
            <el-option label="全部" value="" />
            <el-option label="版面分析" value="layout_analysis" />
            <el-option label="OCR识别" value="ocr" />
            <el-option label="表格提取" value="table_extraction" />
            <el-option label="字段提取" value="field_extraction" />
            <el-option label="校验" value="validation" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch()">搜索</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建版本</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="版本名称" min-width="160" />
        <el-table-column prop="version" label="版本号" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文档类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ getDocTypeLabel(row.documentType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理阶段" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ getStageLabel(row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openViewDialog(row)">查看</el-button>
            <el-button
              v-if="row.status !== 'active'"
              type="success"
              link
              :icon="Check"
              @click="handleActivate(row)"
            >
              设为当前
            </el-button>
            <el-button
              v-if="row.status === 'archived'"
              type="warning"
              link
              :icon="RefreshLeft"
              @click="handleRollback(row)"
            >
              回滚
            </el-button>
            <el-button type="info" link :icon="Sort" @click="openDiffDialog(row)">对比</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑版本' : '新建版本'"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="版本名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入版本名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version">
              <el-input v-model="form.version" placeholder="如 v1.0.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="文档类型" prop="documentType">
              <el-select v-model="form.documentType" placeholder="请选择文档类型" style="width: 100%">
                <el-option label="合同" value="contract" />
                <el-option label="发票" value="invoice" />
                <el-option label="提单" value="bill_of_lading" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理阶段" prop="stage">
              <el-select v-model="form.stage" placeholder="请选择处理阶段" style="width: 100%">
                <el-option label="版面分析" value="layout_analysis" />
                <el-option label="OCR识别" value="ocr" />
                <el-option label="表格提取" value="table_extraction" />
                <el-option label="字段提取" value="field_extraction" />
                <el-option label="校验" value="validation" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="提示词内容" prop="content">
          <div class="code-editor-wrapper">
            <div class="code-editor-header">
              <span class="code-editor-lang">Prompt Content</span>
            </div>
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="14"
              placeholder="请输入提示词内容..."
              class="code-editor-input"
            />
          </div>
        </el-form-item>
        <el-form-item label="提交说明">
          <el-input v-model="form.commitMessage" placeholder="请输入本次变更说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- View Detail Dialog -->
    <el-dialog
      v-model="viewDialogVisible"
      title="版本详情"
      width="700px"
      :close-on-click-modal="false"
    >
      <template v-if="viewVersion">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="版本名称">{{ viewVersion.name }}</el-descriptions-item>
          <el-descriptions-item label="版本号">{{ viewVersion.version }}</el-descriptions-item>
          <el-descriptions-item label="文档类型">
            <el-tag size="small">{{ getDocTypeLabel(viewVersion.documentType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理阶段">
            <el-tag size="small" type="warning">{{ getStageLabel(viewVersion.stage) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusTagType(viewVersion.status)">{{ getStatusLabel(viewVersion.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="作者">{{ viewVersion.author }}</el-descriptions-item>
          <el-descriptions-item label="Git 提交" v-if="viewVersion.gitCommitHash">
            <el-tag size="small" type="info">{{ viewVersion.gitCommitHash?.substring(0, 7) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交说明" v-if="viewVersion.commitMessage">
            {{ viewVersion.commitMessage }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ viewVersion.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ viewVersion.updateTime }}</el-descriptions-item>
          <el-descriptions-item label="提示词内容" :span="2">
            <div class="code-preview">
              <pre>{{ viewVersion.content }}</pre>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- Diff Compare Dialog -->
    <el-dialog
      v-model="diffDialogVisible"
      title="版本对比"
      width="1000px"
      :close-on-click-modal="false"
    >
      <div class="diff-header">
        <div class="diff-selector-a">
          <span class="diff-label">版本 A:</span>
          <el-select v-model="diffVersionA" placeholder="选择版本A" @change="computeDiff" style="width: 250px">
            <el-option
              v-for="v in allVersions"
              :key="v.id"
              :label="`${v.name} (${v.version})`"
              :value="v.id"
            />
          </el-select>
        </div>
        <div class="diff-selector-b">
          <span class="diff-label">版本 B:</span>
          <el-select v-model="diffVersionB" placeholder="选择版本B" @change="computeDiff" style="width: 250px">
            <el-option
              v-for="v in allVersions"
              :key="v.id"
              :label="`${v.name} (${v.version})`"
              :value="v.id"
            />
          </el-select>
        </div>
      </div>
      <div class="diff-view" v-if="diffResult.length > 0">
        <div class="diff-pane">
          <div class="diff-pane-title">旧版本 (A)</div>
          <div
            v-for="(line, index) in diffSideA"
            :key="'a-' + index"
            class="diff-line"
            :class="'diff-line-' + line.type"
          >
            <span class="diff-line-number">{{ line.lineNumber }}</span>
            <span class="diff-line-content">{{ line.contentA || '' }}</span>
          </div>
        </div>
        <div class="diff-pane">
          <div class="diff-pane-title">新版本 (B)</div>
          <div
            v-for="(line, index) in diffSideB"
            :key="'b-' + index"
            class="diff-line"
            :class="'diff-line-' + line.type"
          >
            <span class="diff-line-number">{{ line.lineNumber }}</span>
            <span class="diff-line-content">{{ line.contentB || '' }}</span>
          </div>
        </div>
      </div>
      <div v-else class="diff-empty">
        <p>请选择两个版本进行对比</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, View, Check, RefreshLeft, Sort } from '@element-plus/icons-vue'
import type { PromptVersion, PromptDiff } from '@/api/prompt'

// ---------- Document Type & Stage Labels ----------
const docTypeMap: Record<string, string> = {
  contract: '合同',
  invoice: '发票',
  bill_of_lading: '提单'
}
function getDocTypeLabel(type: string) {
  return docTypeMap[type] || type
}

const stageMap: Record<string, string> = {
  layout_analysis: '版面分析',
  ocr: 'OCR识别',
  table_extraction: '表格提取',
  field_extraction: '字段提取',
  validation: '校验'
}
function getStageLabel(stage: string) {
  return stageMap[stage] || stage
}

const statusLabelMap: Record<string, string> = {
  draft: '草稿',
  active: '生效中',
  archived: '已归档'
}
function getStatusLabel(status: string) {
  return statusLabelMap[status] || status
}

function statusTagType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'active') return 'success'
  if (status === 'archived') return 'warning'
  return 'info'
}

// ---------- Pagination ----------
const loading = ref(false)
const tableData = ref<PromptVersion[]>([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const query = reactive({ documentType: '', stage: '' })

function resetFilters() {
  query.documentType = ''
  query.stage = ''
  pagination.page = 1
  fetchData()
}

function onPageChange(page: number) {
  pagination.page = page
  fetchData()
}

function onSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchData()
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

// ---------- Mock Data ----------
let versionIdCounter = 10

function generateMockData(): PromptVersion[] {
  const types = ['contract', 'invoice', 'bill_of_lading']
  const stages = ['layout_analysis', 'ocr', 'table_extraction', 'field_extraction', 'validation']
  const statuses: Array<'draft' | 'active' | 'archived'> = ['draft', 'active', 'archived']
  const authors = ['admin', 'zhangsan', 'lisi']
  const contentTemplates = [
    '你需要分析以下文档的版面结构，识别出标题、段落、表格、图片等区域。\n\n文档类型：${docType}\n处理阶段：版面分析\n\n请按照以下JSON格式输出结果：\n{\n  "regions": [\n    {\n      "type": "title|paragraph|table|image",\n      "bbox": [x1, y1, x2, y2],\n      "confidence": 0.95\n    }\n  ]\n}',
    '你需要对以下文档执行OCR识别，将图片中的文字转换为结构化文本。\n\n要求：\n1. 保持原始排版格式\n2. 识别所有文本区域\n3. 对低置信度字符进行标注\n4. 输出格式：按行输出，标注每行的位置信息',
    '你需要提取文档中的表格数据，识别表头和单元格内容。\n\n表格提取规则：\n1. 识别表格边界\n2. 提取表头行\n3. 逐行提取单元格数据\n4. 处理合并单元格\n5. 输出结构化JSON格式',
    '你需要从文档中提取关键字段信息。\n\n目标字段：\n- 文档编号\n- 日期\n- 当事人信息\n- 金额\n- 签署信息\n\n输出格式：JSON',
    '你需要对提取结果进行校验，确保数据的准确性和完整性。\n\n校验规则：\n1. 必填字段检查\n2. 数据类型检查\n3. 逻辑一致性检查\n4. 交叉验证'
  ]

  return types.flatMap((docType, idx) =>
    stages.map((stage, sIdx) => {
      const id = idx * 5 + sIdx + 1
      const status = id <= 5 ? 'active' : id <= 8 ? 'draft' : 'archived'
      const baseVersion = id <= 5 ? '1.2.0' : id <= 8 ? '1.1.0' : '1.0.0'
      const patchNum = id % 3
      return {
        id,
        name: `${getDocTypeLabel(docType)}-${getStageLabel(stage)}提示词`,
        version: `${baseVersion.substring(0, baseVersion.lastIndexOf('.') + 1)}${patchNum}`,
        documentType: docType,
        stage,
        content: contentTemplates[sIdx].replace('${docType}', getDocTypeLabel(docType)),
        author: authors[id % authors.length],
        status,
        commitMessage: status === 'draft' ? undefined : `更新${getStageLabel(stage)}提示词逻辑`,
        gitCommitHash: `a1b2c3d${id}e`,
        createTime: `2024-0${Math.min(9, id + 3)}-${String((id * 7) % 28 + 1).padStart(2, '0')} 10:00:00`,
        updateTime: `2025-0${Math.min(9, (idx + 1) * 2)}-${String((id * 11) % 28 + 1).padStart(2, '0')} 14:30:00`
      }
    })
  )
}

const allMockData = generateMockData()
versionIdCounter = allMockData.length + 1

function fetchData() {
  loading.value = true
  setTimeout(() => {
    let filtered = [...allMockData]
    if (query.documentType) {
      filtered = filtered.filter(v => v.documentType === query.documentType)
    }
    if (query.stage) {
      filtered = filtered.filter(v => v.stage === query.stage)
    }
    const start = (pagination.page - 1) * pagination.pageSize
    tableData.value = filtered.slice(start, start + pagination.pageSize)
    pagination.total = filtered.length
    loading.value = false
  }, 300)
}

// ---------- Create/Edit ----------
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref(0)

const defaultForm = {
  name: '',
  version: 'v1.0.0',
  documentType: '',
  stage: '',
  content: '',
  commitMessage: ''
}
const form = reactive({ ...defaultForm })

const formRules: FormRules = {
  name: [{ required: true, message: '请输入版本名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  documentType: [{ required: true, message: '请选择文档类型', trigger: 'change' }],
  stage: [{ required: true, message: '请选择处理阶段', trigger: 'change' }],
  content: [{ required: true, message: '请输入提示词内容', trigger: 'blur' }]
}

function openCreateDialog() {
  isEdit.value = false
  currentEditId.value = 0
  Object.assign(form, { ...defaultForm })
  dialogVisible.value = true
}

function openEditDialog(row: PromptVersion) {
  isEdit.value = true
  currentEditId.value = row.id
  form.name = row.name
  form.version = row.version
  form.documentType = row.documentType
  form.stage = row.stage
  form.content = row.content
  form.commitMessage = row.commitMessage || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
  if (isEdit.value) {
    const idx = allMockData.findIndex(v => v.id === currentEditId.value)
    if (idx >= 0) {
      Object.assign(allMockData[idx], { ...form, updateTime: now })
    }
    ElMessage.success('更新成功')
  } else {
    const newVersion: PromptVersion = {
      id: versionIdCounter++,
      ...form,
      author: 'admin',
      status: 'draft',
      createTime: now,
      updateTime: now
    }
    allMockData.unshift(newVersion)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

// ---------- Activate ----------
async function handleActivate(row: PromptVersion) {
  await ElMessageBox.confirm(
    `确定要将 "${row.name}" 设置为当前生效版本吗？同一文档类型和阶段的旧生效版本将被归档。`,
    '设为当前',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
  )
  allMockData.forEach(v => {
    if (v.documentType === row.documentType && v.stage === row.stage && v.status === 'active') {
      v.status = 'archived'
    }
  })
  row.status = 'active'
  ElMessage.success(`已将 "${row.name}" 设为当前生效版本`)
  fetchData()
}

// ---------- Rollback ----------
async function handleRollback(row: PromptVersion) {
  await ElMessageBox.confirm(
    `确定要回滚到 "${row.name} (${row.version})" 吗？当前生效版本将被归档。`,
    '版本回滚',
    { confirmButtonText: '确定回滚', cancelButtonText: '取消', type: 'warning' }
  )
  allMockData.forEach(v => {
    if (v.documentType === row.documentType && v.stage === row.stage && v.status === 'active') {
      v.status = 'archived'
    }
  })
  row.status = 'active'
  ElMessage.success(`已回滚到 "${row.name} (${row.version})"`)
  fetchData()
}

// ---------- View Detail ----------
const viewDialogVisible = ref(false)
const viewVersion = ref<PromptVersion | null>(null)

function openViewDialog(row: PromptVersion) {
  viewVersion.value = row
  viewDialogVisible.value = true
}

// ---------- Diff Compare ----------
const diffDialogVisible = ref(false)
const diffVersionA = ref<number | null>(null)
const diffVersionB = ref<number | null>(null)
const diffResult = ref<PromptDiff['diffLines']>([])
const allVersions = ref<PromptVersion[]>([])

const diffSideA = computed(() => {
  return diffResult.value.map((d, i) => ({
    lineNumber: i + 1,
    type: d.type,
    contentA: d.type === 'removed' || d.type === 'unchanged' ? d.content : '',
    contentB: d.type === 'added' || d.type === 'unchanged' ? d.content : ''
  }))
})

const diffSideB = computed(() => {
  return diffResult.value.map((d, i) => ({
    lineNumber: i + 1,
    type: d.type,
    contentA: d.type === 'removed' || d.type === 'unchanged' ? d.content : '',
    contentB: d.type === 'added' || d.type === 'unchanged' ? d.content : ''
  }))
})

function openDiffDialog(row: PromptVersion) {
  allVersions.value = [...allMockData].sort((a, b) => b.id - a.id)
  diffVersionA.value = row.id
  diffVersionB.value = null
  diffResult.value = []
  diffDialogVisible.value = true
}

function computeDiff() {
  if (!diffVersionA.value || !diffVersionB.value) {
    diffResult.value = []
    return
  }
  const versA = allMockData.find(v => v.id === diffVersionA.value)
  const versB = allMockData.find(v => v.id === diffVersionB.value)
  if (!versA || !versB) return

  const linesA = versA.content.split('\n')
  const linesB = versB.content.split('\n')
  const result: PromptDiff['diffLines'] = []

  // Simple line-by-line diff via LCS
  const maxLen = Math.max(linesA.length, linesB.length)
  const lcs = computeLCS(linesA, linesB)
  let i = 0, j = 0, k = 0

  while (i < linesA.length || j < linesB.length) {
    if (k < lcs.length && i < linesA.length && j < linesB.length && linesA[i] === lcs[k] && linesB[j] === lcs[k]) {
      result.push({ type: 'unchanged', lineNumber: result.length + 1, content: linesA[i] })
      i++; j++; k++
    } else if (k < lcs.length && i < linesA.length && linesA[i] !== lcs[k]) {
      result.push({ type: 'removed', lineNumber: result.length + 1, content: linesA[i] })
      i++
    } else if (k < lcs.length && j < linesB.length && linesB[j] !== lcs[k]) {
      result.push({ type: 'added', lineNumber: result.length + 1, content: linesB[j] })
      j++
    } else if (i < linesA.length) {
      result.push({ type: 'removed', lineNumber: result.length + 1, content: linesA[i] })
      i++
    } else if (j < linesB.length) {
      result.push({ type: 'added', lineNumber: result.length + 1, content: linesB[j] })
      j++
    }
  }

  diffResult.value = result
}

function computeLCS(a: string[], b: string[]): string[] {
  const m = a.length, n = b.length
  const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (a[i - 1] === b[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }
  const lcs: string[] = []
  let i = m, j = n
  while (i > 0 && j > 0) {
    if (a[i - 1] === b[j - 1]) {
      lcs.unshift(a[i - 1])
      i--; j--
    } else if (dp[i - 1][j] > dp[i][j - 1]) {
      i--
    } else {
      j--
    }
  }
  return lcs
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.prompt-versions {
  .page-header {
    margin-bottom: 16px;
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  // Code editor style textarea
  .code-editor-wrapper {
    width: 100%;
    border: 1px solid #333;
    border-radius: 6px;
    overflow: hidden;

    .code-editor-header {
      background: #2d2d2d;
      padding: 6px 14px;
      display: flex;
      align-items: center;

      .code-editor-lang {
        color: #abb2bf;
        font-size: 12px;
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      }
    }

    .code-editor-input {
      :deep(.el-textarea__inner) {
        background: #1e1e1e;
        color: #d4d4d4;
        border: none;
        border-radius: 0;
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        font-size: 13px;
        line-height: 1.6;
        resize: vertical;

        &::placeholder {
          color: #6a737d;
        }
      }
    }
  }

  // Code preview in view dialog
  .code-preview {
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 12px;
    border-radius: 6px;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.6;
    max-height: 400px;
    overflow: auto;

    pre {
      margin: 0;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }

  // Diff viewer
  .diff-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 6px;

    .diff-selector-a,
    .diff-selector-b {
      display: flex;
      align-items: center;
      gap: 8px;

      .diff-label {
        font-weight: 600;
        color: #303133;
        white-space: nowrap;
      }
    }
  }

  .diff-view {
    display: flex;
    gap: 2px;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    overflow: hidden;
    max-height: 500px;
    overflow-y: auto;
  }

  .diff-pane {
    flex: 1;
    min-width: 0;

    .diff-pane-title {
      background: #f5f7fa;
      padding: 8px 12px;
      font-weight: 600;
      font-size: 13px;
      color: #303133;
      border-bottom: 1px solid #e4e7ed;
      position: sticky;
      top: 0;
      z-index: 1;
    }
  }

  .diff-line {
    display: flex;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
    min-height: 22px;
    border-bottom: 1px solid #f0f0f0;

    .diff-line-number {
      display: inline-block;
      width: 40px;
      text-align: right;
      padding: 2px 8px;
      color: #909399;
      background: #fafafa;
      flex-shrink: 0;
      user-select: none;
    }

    .diff-line-content {
      padding: 2px 12px;
      white-space: pre;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &.diff-line-added {
      background: #e6ffec;
      .diff-line-number {
        background: #ccffd8;
      }
    }

    &.diff-line-removed {
      background: #ffebe9;
      .diff-line-number {
        background: #ffd7d5;
      }
    }

    &.diff-line-unchanged {
      background: #fff;
    }
  }

  .diff-empty {
    text-align: center;
    padding: 60px 0;
    color: #909399;
  }
}
</style>

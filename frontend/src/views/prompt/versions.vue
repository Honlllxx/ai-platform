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
import {
  getPromptVersions,
  createPromptVersion,
  updatePromptVersion,
  rollbackPromptVersion,
  getPromptDiff,
} from '@/api/prompt'

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

// ---------- Data Fetching ----------

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (query.documentType) params.documentType = query.documentType
    if (query.stage) params.stage = query.stage
    const res = await getPromptVersions(params)
    const result = res?.data || res || {}
    tableData.value = (result.records || result.list || result.data || []) as PromptVersion[]
    pagination.total = result.total || 0
  } catch (e: any) {
    console.error('Failed to load prompt versions:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
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

  try {
    if (isEdit.value) {
      await updatePromptVersion({
        id: currentEditId.value,
        ...form,
        commitMessage: form.commitMessage || undefined,
      } as any)
      ElMessage.success('更新成功')
    } else {
      await createPromptVersion({
        name: form.name,
        version: form.version,
        documentType: form.documentType,
        stage: form.stage,
        content: form.content,
        commitMessage: form.commitMessage || undefined,
      } as any)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    pagination.page = 1
    fetchData()
  } catch (e: any) {
    ElMessage.error('操作失败: ' + (e?.message || '未知错误'))
  }
}

// ---------- Activate ----------
async function handleActivate(row: PromptVersion) {
  try {
    await ElMessageBox.confirm(
      `确定要将 "${row.name}" 设置为当前生效版本吗？`,
      '设为当前',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await rollbackPromptVersion(row.id)
    ElMessage.success(`已将 "${row.name}" 设为当前生效版本`)
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败: ' + (e?.message || '未知错误'))
  }
}

// ---------- Rollback ----------
async function handleRollback(row: PromptVersion) {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到 "${row.name} (${row.version})" 吗？`,
      '版本回滚',
      { confirmButtonText: '确定回滚', cancelButtonText: '取消', type: 'warning' }
    )
    await rollbackPromptVersion(row.id)
    ElMessage.success(`已回滚到 "${row.name} (${row.version})"`)
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('回滚失败: ' + (e?.message || '未知错误'))
  }
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

async function openDiffDialog(row: PromptVersion) {
  diffVersionA.value = row.id
  diffVersionB.value = null
  diffResult.value = []
  diffDialogVisible.value = true
  // Fetch all versions for the diff selector
  try {
    const res = await getPromptVersions({ page: 1, pageSize: 200, documentType: row.documentType })
    const result = res?.data || res || {}
    allVersions.value = ((result.records || result.list || result.data || []) as PromptVersion[])
      .sort((a: PromptVersion, b: PromptVersion) => b.id - a.id)
  } catch {
    allVersions.value = []
  }
}

async function computeDiff() {
  if (!diffVersionA.value || !diffVersionB.value) {
    diffResult.value = []
    return
  }
  try {
    const res = await getPromptDiff({
      versionA: diffVersionA.value,
      versionB: diffVersionB.value,
    })
    const result = res?.data || res || {}
    diffResult.value = result.diffLines || result.lines || result || []
  } catch {
    // Fallback to client-side diff
    const versA = allVersions.value.find(v => v.id === diffVersionA.value)
    const versB = allVersions.value.find(v => v.id === diffVersionB.value)
    if (!versA || !versB) return
    diffResult.value = simpleClientDiff(versA.content, versB.content)
  }
}

function simpleClientDiff(textA: string, textB: string): PromptDiff['diffLines'] {
  const linesA = textA.split('\n')
  const linesB = textB.split('\n')
  const result: PromptDiff['diffLines'] = []
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

<template>
  <div class="page-container review-list">
    <!-- Page header -->
    <div class="page-header">
      <h2>审核工作台</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true" v-permission="'review:create'">
        创建审核任务
      </el-button>
    </div>

    <!-- Stats row -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingCount }}</div>
              <div class="stat-label">待审核</div>
            </div>
            <el-icon :size="40" class="stat-icon" color="#409eff"><Clock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayReviewed }}</div>
              <div class="stat-label">今日已审核</div>
            </div>
            <el-icon :size="40" class="stat-icon" color="#67c23a"><Select /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.approvalRate }}%</div>
              <div class="stat-label">通过率</div>
            </div>
            <el-icon :size="40" class="stat-icon" color="#e6a23c"><TrendCharts /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalReviews }}</div>
              <div class="stat-label">总审核任务</div>
            </div>
            <el-icon :size="40" class="stat-icon" color="#909399"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filter bar -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="部分通过" value="partial" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="query.documentType" placeholder="全部" clearable style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="文档名称" clearable style="width: 220px;" @keyup.enter="handleSearch()" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch()">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="data" v-loading="loading" stripe border style="width: 100%;">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="documentName" label="文档名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="文档类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="docTypeTagType(row.documentType)">
              {{ docTypeLabel(row.documentType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字段进度" min-width="190">
          <template #default="{ row }">
            <div class="progress-cell">
              <span class="progress-text">{{ row.reviewedFields }} / {{ row.totalFields }}</span>
              <el-progress
                :percentage="fieldProgress(row)"
                :stroke-width="6"
                :color="progressColor(row)"
                :show-text="false"
                style="flex: 1;"
              />
              <span class="progress-detail">
                通过 {{ row.approvedFields }} | 驳回 {{ row.rejectedFields }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核人" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.reviewer">{{ row.reviewer }}</span>
            <el-button
              v-else
              link
              type="primary"
              size="small"
              :icon="User"
              @click="openAssignDialog(row)"
              v-permission="'review:update'"
            >
              分配
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="170">
          <template #default="{ row }">
            {{ row.reviewTime || row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleStartReview(row)">
              审核
            </el-button>
            <el-button
              v-if="!row.reviewer"
              link
              type="warning"
              :icon="User"
              @click="openAssignDialog(row)"
              v-permission="'review:update'"
            >
              分配
            </el-button>
            <el-button
              link
              type="danger"
              :icon="Delete"
              @click="handleDelete(row)"
              v-permission="'review:delete'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && data.length === 0" description="暂无审核任务" />

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- Create Review Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建审核任务" width="560px" :close-on-click-modal="false">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="文档" prop="documentId">
          <el-select v-model="createForm.documentId" placeholder="请选择文档" filterable style="width: 100%;">
            <el-option
              v-for="doc in documents"
              :key="doc.id"
              :label="doc.fileName"
              :value="doc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文档类型" prop="documentType">
          <el-select v-model="createForm.documentType" placeholder="请选择文档类型" style="width: 100%;">
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核人" prop="reviewerId">
          <el-select v-model="createForm.reviewerId" placeholder="请选择审核人（可选）" clearable filterable style="width: 100%;">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="`${user.username} (${user.nickname || user.username})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Assign Reviewer Dialog -->
    <el-dialog v-model="showAssignDialog" title="分配审核人" width="450px" :close-on-click-modal="false">
      <el-form :model="assignForm" ref="assignFormRef" label-width="80px">
        <el-form-item label="审核任务">
          <span class="assign-doc-name">{{ assignTarget?.documentName }}</span>
        </el-form-item>
        <el-form-item label="审核人" prop="reviewerId">
          <el-select v-model="assignForm.reviewerId" placeholder="请选择审核人" filterable style="width: 100%;">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="`${user.username} (${user.nickname || user.username})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, Clock, Select, TrendCharts, Document, Plus, User, Delete } from '@element-plus/icons-vue'
import {
  getReviews, getReviewStats, createReview, assignReviewer, deleteReview,
  type ReviewItem, type ReviewStats
} from '@/api/review'
import { getDocuments } from '@/api/document'
import { getUsers } from '@/api/user'
import { useTable } from '@/composables/useTable'

const router = useRouter()

const {
  data,
  loading,
  pagination,
  query,
  handleSearch,
  handleReset,
  handlePageChange,
  handleSizeChange,
  fetchData
} = useTable<ReviewItem>({
  fetchApi: getReviews,
  defaultQuery: { status: '', documentType: '', keyword: '' }
})

const stats = reactive<ReviewStats>({
  pendingCount: 0,
  todayReviewed: 0,
  approvalRate: 0,
  totalReviews: 0
})

async function loadStats() {
  try {
    const res = await getReviewStats()
    const d = (res as any)?.data || res
    Object.assign(stats, d)
  } catch {
    // Fallback to local computation
    stats.pendingCount = data.value.filter(d => d.status === 'pending').length
    stats.totalReviews = data.value.length
  }
}

// ==================== Create Review ====================

const showCreateDialog = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  documentId: null as number | null,
  documentType: '',
  reviewerId: null as number | null
})
const createRules = {
  documentId: [{ required: true, message: '请选择文档', trigger: 'change' }],
  documentType: [{ required: true, message: '请选择文档类型', trigger: 'change' }]
}

const documents = ref<any[]>([])
const userList = ref<any[]>([])

async function loadDocuments() {
  try {
    const res = await getDocuments({ page: 1, pageSize: 200 })
    const d = (res as any)?.data || res
    documents.value = d.records || d || []
  } catch { /* ignore */ }
}

async function loadUsers() {
  try {
    const res = await getUsers({ page: 1, pageSize: 200 })
    const d = (res as any)?.data || res
    userList.value = (d.records || d || []).filter((u: any) => u.status !== 0)
  } catch { /* ignore */ }
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createReview({
      documentId: createForm.documentId!,
      documentType: createForm.documentType,
      reviewerId: createForm.reviewerId ?? undefined
    })
    ElMessage.success('审核任务创建成功')
    showCreateDialog.value = false
    createForm.documentId = null
    createForm.documentType = ''
    createForm.reviewerId = null
    fetchData()
    loadStats()
  } catch {
    // Error handled by interceptor
  } finally {
    creating.value = false
  }
}

// ==================== Assign Reviewer ====================

const showAssignDialog = ref(false)
const assigning = ref(false)
const assignTarget = ref<ReviewItem | null>(null)
const assignForm = reactive({ reviewerId: null as number | null })

function openAssignDialog(row: ReviewItem) {
  assignTarget.value = row
  assignForm.reviewerId = row.reviewerId || null
  showAssignDialog.value = true
}

async function handleAssign() {
  if (!assignForm.reviewerId || !assignTarget.value) return
  assigning.value = true
  try {
    await assignReviewer(assignTarget.value.id, assignForm.reviewerId)
    ElMessage.success('审核人分配成功')
    showAssignDialog.value = false
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    assigning.value = false
  }
}

// ==================== Delete ====================

async function handleDelete(row: ReviewItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除审核任务「${row.documentName}」？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteReview(row.id)
    ElMessage.success('审核任务已删除')
    fetchData()
    loadStats()
  } catch {
    // Cancelled or error
  }
}

// ==================== Helpers ====================

function docTypeTagType(type: string) {
  const map: Record<string, string> = {
    contract: 'success',
    invoice: 'warning',
    bill_of_lading: 'danger'
  }
  return map[type] || ''
}

function docTypeLabel(type: string) {
  const map: Record<string, string> = {
    contract: '合同',
    invoice: '发票',
    bill_of_lading: '提单'
  }
  return map[type] || type
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    approved: 'success',
    rejected: 'danger',
    partial: 'warning'
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    partial: '部分通过'
  }
  return map[status] || status
}

function fieldProgress(row: ReviewItem) {
  if (row.totalFields === 0) return 0
  return Math.round((row.reviewedFields / row.totalFields) * 100)
}

function progressColor(row: ReviewItem) {
  if (row.status === 'rejected') return '#f56c6c'
  if (row.status === 'approved') return '#67c23a'
  return '#409eff'
}

function handleStartReview(row: ReviewItem) {
  router.push(`/review/${row.id}`)
}

onMounted(async () => {
  await Promise.all([fetchData(), loadStats(), loadDocuments(), loadUsers()])
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.review-list {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
    }
  }

  .stats-row {
    margin-bottom: 16px;
  }

  .stat-card {
    :deep(.el-card__body) {
      padding: 16px 20px;
    }
  }

  .stat-content {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: $text-primary;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }

    .stat-icon {
      opacity: 0.65;
    }
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .progress-text {
    font-size: 12px;
    color: $text-regular;
    font-weight: 500;
    white-space: nowrap;
    min-width: 50px;
  }

  .progress-detail {
    font-size: 11px;
    color: $text-secondary;
    white-space: nowrap;
  }
}

.assign-doc-name {
  font-size: 14px;
  color: $text-primary;
  font-weight: 500;
}
</style>
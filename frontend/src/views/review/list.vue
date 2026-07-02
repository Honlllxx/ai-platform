<template>
  <div class="page-container review-list">
    <!-- Page header -->
    <div class="page-header">
      <h2>人工审核工作台</h2>
    </div>

    <!-- Stats row -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingCount }}</div>
              <div class="stat-label">待审核任务</div>
            </div>
            <el-icon :size="40" class="stat-icon" color="#409eff"><Clock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
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
      <el-col :xs="24" :sm="8">
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
    </el-row>

    <!-- Filter bar -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="部分通过" value="partial" />
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
        <el-table-column prop="documentName" label="文档名称" min-width="180" />
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
        <el-table-column label="字段进度" min-width="180">
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
        <el-table-column label="审核人" width="120" align="center">
          <template #default="{ row }">
            {{ row.reviewer || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="170">
          <template #default="{ row }">
            {{ row.reviewTime || row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleStartReview(row)">
              开始审核
            </el-button>
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
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, View, Clock, Select, TrendCharts } from '@element-plus/icons-vue'
import { getReviews, type ReviewItem } from '@/api/review'
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
  defaultQuery: { status: '', keyword: '' }
})

const stats = reactive({
  pendingCount: 0,
  todayReviewed: 0,
  approvalRate: 0
})

function updateStats() {
  stats.pendingCount = data.value.filter(d => d.status === 'pending').length
  stats.todayReviewed = data.value.filter(d => d.status !== 'pending').length
  const totalApproved = data.value.reduce((sum, d) => sum + d.approvedFields, 0)
  const totalFields = data.value.reduce((sum, d) => sum + d.totalFields, 0)
  stats.approvalRate = totalFields > 0 ? Math.round((totalApproved / totalFields) * 100) : 0
}

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

// Mock data generation for demo
function generateMockData(): ReviewItem[] {
  return [
    {
      id: 1, documentId: 101, documentName: '2026年度采购合同.pdf', documentType: 'contract',
      status: 'pending', totalFields: 15, reviewedFields: 0, approvedFields: 0, rejectedFields: 0,
      reviewer: undefined, reviewTime: undefined, createTime: '2026-06-29 09:15:00'
    },
    {
      id: 2, documentId: 102, documentName: '技术服务协议_v3.docx', documentType: 'contract',
      status: 'partial', totalFields: 12, reviewedFields: 8, approvedFields: 6, rejectedFields: 2,
      reviewer: 'admin', reviewTime: '2026-06-29 10:45:00', createTime: '2026-06-29 08:30:00'
    },
    {
      id: 3, documentId: 103, documentName: '增值税发票_0628.pdf', documentType: 'invoice',
      status: 'approved', totalFields: 8, reviewedFields: 8, approvedFields: 8, rejectedFields: 0,
      reviewer: 'zhangsan', reviewTime: '2026-06-28 16:20:00', createTime: '2026-06-28 14:00:00'
    },
    {
      id: 4, documentId: 104, documentName: '海运提单_BL20260625.pdf', documentType: 'bill_of_lading',
      status: 'pending', totalFields: 10, reviewedFields: 0, approvedFields: 0, rejectedFields: 0,
      reviewer: undefined, reviewTime: undefined, createTime: '2026-06-29 11:00:00'
    },
    {
      id: 5, documentId: 105, documentName: '设备租赁合同.pdf', documentType: 'contract',
      status: 'rejected', totalFields: 14, reviewedFields: 14, approvedFields: 3, rejectedFields: 11,
      reviewer: 'admin', reviewTime: '2026-06-28 11:30:00', createTime: '2026-06-28 09:00:00'
    },
    {
      id: 6, documentId: 106, documentName: '普通发票_0627.pdf', documentType: 'invoice',
      status: 'approved', totalFields: 6, reviewedFields: 6, approvedFields: 6, rejectedFields: 0,
      reviewer: 'zhangsan', reviewTime: '2026-06-27 17:00:00', createTime: '2026-06-27 15:30:00'
    },
  ]
}

onMounted(() => {
  if (import.meta.env.DEV) {
    const mockData = generateMockData()
    data.value = mockData
    pagination.total = mockData.length
    updateStats()
  } else {
    fetchData()
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.review-list {
  .page-header {
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
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
</style>

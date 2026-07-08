<template>
  <div class="page-container pipeline-list">
    <!-- Page Header -->
    <div class="page-header">
      <h3 class="page-title">处理流水线</h3>
      <div class="header-actions">
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          :active-value="true"
          :inactive-value="false"
          size="small"
        />
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px;">
            <el-option label="待处理" value="pending" />
            <el-option label="运行中" value="running" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索文档名称..."
            :prefix-icon="Search"
            clearable
            style="width: 240px;"
            @keyup.enter="handleSearch()"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch()">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="data" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="documentName" label="文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前步骤" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="current-step">{{ row.currentStep }}</span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="180" align="center">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="progressPercent(row)"
                :color="progressColor(row)"
                :stroke-width="10"
                :striped="row.status === 'running'"
                :striped-flow="row.status === 'running'"
              />
              <span class="progress-text">{{ row.completedSteps }} / {{ row.totalSteps }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="110" align="center">
          <template #default="{ row }">
            <span>{{ formatDuration(row.duration) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="goDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && data.length === 0" description="暂无流水线记录" />

      <!-- Pagination -->
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
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import { getPipelines, type PipelineItem } from '@/api/pipeline'
import { useTable } from '@/composables/useTable'

const router = useRouter()
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

const {
  data,
  loading,
  pagination,
  query,
  handleSearch: baseSearch,
  handleReset: baseReset,
  handlePageChange,
  handleSizeChange,
  fetchData
} = useTable<PipelineItem>({
  fetchApi: getPipelines,
  defaultQuery: { status: '', keyword: '' },
  defaultPageSize: 10
})

function handleSearch() {
  baseSearch()
}

function handleReset() {
  baseReset({ status: '', keyword: '' })
}

function progressPercent(row: PipelineItem): number {
  if (row.totalSteps === 0) return 0
  return Math.round((row.completedSteps / row.totalSteps) * 100)
}

function progressColor(row: PipelineItem): string {
  if (row.status === 'failed') return '#f56c6c'
  if (row.status === 'completed') return '#67c23a'
  if (row.status === 'running') return '#409eff'
  return '#909399'
}

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    pending: 'info',
    running: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    running: '运行中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

function formatDuration(seconds: number): string {
  if (seconds === undefined || seconds === null) return '-'
  if (seconds < 60) return `${seconds}s`
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins < 60) return `${mins}m ${secs}s`
  const hours = Math.floor(mins / 60)
  const remainingMins = mins % 60
  return `${hours}h ${remainingMins}m`
}

function goDetail(row: PipelineItem) {
  router.push(`/pipeline/${row.id}`)
}

// Auto-refresh logic
function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    fetchData()
  }, 5000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

watch(autoRefresh, (val) => {
  if (val) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
})

onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped lang="scss">
.pipeline-list {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

.current-step {
  color: #409eff;
  font-weight: 500;
  font-size: 13px;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .progress-text {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
    min-width: 50px;
  }

  :deep(.el-progress) {
    flex: 1;
  }
}
</style>

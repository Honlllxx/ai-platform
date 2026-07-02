<template>
  <div class="page-container document-list">
    <!-- Page Header -->
    <div class="page-header">
      <h2 class="page-title">文档列表</h2>
    </div>

    <!-- Search Filters -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="文件类型">
          <el-select v-model="query.fileType" placeholder="全部" clearable style="width: 130px;">
            <el-option label="全部" value="" />
            <el-option label="PDF" value="pdf" />
            <el-option label="JPG" value="jpg" />
            <el-option label="PNG" value="png" />
            <el-option label="Word" value="word" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px;">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="pending" />
            <el-option label="预处理中" value="preprocessing" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px;"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="输入文件名搜索"
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch()">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Action Bar & Table -->
    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Upload" @click="goToUpload">上传文档</el-button>
          <el-button
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
            <template v-if="selectedIds.length">({{ selectedIds.length }})</template>
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button :icon="RefreshRight" @click="refresh">刷新</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        :data="data"
        v-loading="loading"
        stripe
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon :size="18" class="file-icon"><Document /></el-icon>
              <span>{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="fileTypeTagType(row.fileType)" size="small" effect="plain">
              {{ fileTypeLabel(row.fileType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="110" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="页数" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.pageCount">{{ row.pageCount }}</span>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploadUser" label="上传用户" width="110" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="warning"
              link
              :icon="SetUp"
              :loading="preprocessingIds.has(row.id)"
              @click="handlePreprocess(row)"
            >
              预处理
            </el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">
              删除
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

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="文档详情" width="600px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="文件 ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ detail.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">
          <el-tag :type="fileTypeTagType(detail.fileType)" size="small" effect="plain">
            {{ fileTypeLabel(detail.fileType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(detail.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="页数">{{ detail.pageCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)" size="small">
            {{ statusLabel(detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上传用户">{{ detail.uploadUser || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ detail.uploadTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  RefreshRight,
  Upload,
  Delete,
  View,
  SetUp,
  Document
} from '@element-plus/icons-vue'
import {
  getDocuments,
  getDocument,
  deleteDocument,
  preprocessDocument,
  type DocumentItem
} from '@/api/document'
import { useTable } from '@/composables/useTable'

const router = useRouter()

// Date range picker
const dateRange = ref<[string, string] | null>(null)

// useTable composable
const {
  data,
  loading,
  pagination,
  query,
  handleSearch: baseSearch,
  handleReset: baseReset,
  handlePageChange,
  handleSizeChange,
  fetchData,
  refresh
} = useTable<DocumentItem>({
  fetchApi: getDocuments,
  defaultQuery: { fileType: '', status: '', keyword: '' }
})

// Sync date range with query
watch(dateRange, (val) => {
  if (val) {
    query.startDate = val[0]
    query.endDate = val[1]
  } else {
    delete query.startDate
    delete query.endDate
  }
})

function handleSearch() {
  baseSearch()
}

function handleReset() {
  dateRange.value = null
  baseReset({ fileType: '', status: '', keyword: '' })
}

// Selection
const tableRef = ref()
const selectedIds = ref<number[]>([])

function handleSelectionChange(rows: DocumentItem[]) {
  selectedIds.value = rows.map((r) => r.id)
}

// Utilities
function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let unitIndex = 0
  let size = bytes
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

function fileTypeLabel(type: string): string {
  const map: Record<string, string> = {
    pdf: 'PDF',
    jpg: 'JPG',
    png: 'PNG',
    word: 'Word'
  }
  return map[type] || type.toUpperCase()
}

function fileTypeTagType(type: string): 'danger' | 'success' | '' | 'warning' {
  const map: Record<string, 'danger' | 'success' | '' | 'warning'> = {
    pdf: 'danger',
    jpg: '',
    png: 'success',
    word: 'warning'
  }
  return map[type] || ''
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    preprocessing: '预处理中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

function statusTagType(status: string): 'info' | 'warning' | 'success' | 'danger' {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    pending: 'info',
    preprocessing: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

// Navigation
function goToUpload() {
  router.push('/documents/upload')
}

// Detail dialog
const detailVisible = ref(false)
const detail = ref<DocumentItem | null>(null)

async function handleViewDetail(row: DocumentItem) {
  try {
    const res = await getDocument(row.id)
    detail.value = (res?.data || res) as DocumentItem
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// Delete single
async function handleDelete(row: DocumentItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档"${row.fileName}"吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

// Batch delete
async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个文档吗？删除后不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  loading.value = true
  let deleted = 0
  let failed = 0

  for (const id of selectedIds.value) {
    try {
      await deleteDocument(id)
      deleted++
    } catch {
      failed++
    }
  }

  loading.value = false

  if (deleted > 0) {
    ElMessage.success(`成功删除 ${deleted} 个文档${failed > 0 ? `，${failed} 个失败` : ''}`)
  } else {
    ElMessage.error('批量删除失败')
  }

  fetchData()
}

// Preprocess
const preprocessingIds = ref(new Set<number>())

async function handlePreprocess(row: DocumentItem) {
  preprocessingIds.value.add(row.id)
  try {
    await preprocessDocument(row.id)
    ElMessage.success(`已触发预处理：${row.fileName}`)
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    preprocessingIds.value.delete(row.id)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.document-list {
  .page-header {
    margin-bottom: 20px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }

  .table-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .toolbar-left {
      display: flex;
      gap: 12px;
    }
  }

  .file-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .file-icon {
      color: #409eff;
      flex-shrink: 0;
    }
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .text-secondary {
    color: #909399;
  }
}
</style>

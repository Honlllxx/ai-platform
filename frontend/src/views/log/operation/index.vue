<template>
  <div class="page-container operation-log">
    <!-- Search Filters -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
          />
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="query.username" placeholder="输入用户名" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.operationType" placeholder="全部" clearable multiple collapse-tags collapse-tags-tooltip style="width: 200px;">
            <el-option
              v-for="t in operationTypes"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="输入模块名" clearable style="width: 140px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px;">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch()">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset()">重置</el-button>
          <el-button :icon="Download" :loading="exportLoading" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="data" v-loading="loading" stripe border>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="operationTagType(row.operationType)" size="small">
              {{ row.operationType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="description" label="操作描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="140" />
        <el-table-column label="耗时" width="80" align="center">
          <template #default="{ row }">
            {{ row.duration }}ms
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
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
    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="operationTagType(detail.operationType)" size="small">{{ detail.operationType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 'success' ? 'success' : 'danger'" size="small">
            {{ detail.status === 'success' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ detail.description }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="formattedParams" class="json-section">
        <h4>请求参数</h4>
        <pre class="json-block">{{ formattedParams }}</pre>
      </div>
      <div v-if="formattedResult" class="json-section">
        <h4>响应结果</h4>
        <pre class="json-block">{{ formattedResult }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, View } from '@element-plus/icons-vue'
import { getOperationLogs, getOperationLogById, exportOperationLogs, type OperationLog, type OperationLogQuery } from '@/api/operation-log'
import { useTable } from '@/composables/useTable'

const dateRange = ref<[string, string] | null>(null)

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
} = useTable<OperationLog>({
  fetchApi: getOperationLogs,
  defaultQuery: { username: '', operationType: '', module: '', status: '' }
})

// Sync date range with query
watch(dateRange, (val) => {
  if (val) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    delete query.startTime
    delete query.endTime
  }
})

function handleSearch() {
  baseSearch()
}

function handleReset() {
  dateRange.value = null
  baseReset({ username: '', operationType: '', module: '', status: '' })
}

const operationTypes = [
  { label: '登录', value: 'login' },
  { label: '查询', value: 'query' },
  { label: '新增', value: 'create' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '导出', value: 'export' },
  { label: '授权', value: 'grant' },
  { label: '其他', value: 'other' }
]

function operationTagType(type?: string) {
  const map: Record<string, string> = {
    login: 'success',
    query: '',
    create: 'primary',
    update: 'warning',
    delete: 'danger',
    export: 'info',
    grant: 'success'
  }
  return (map as Record<string, string>)[type || ''] || ''
}

const exportLoading = ref(false)
async function handleExport() {
  exportLoading.value = true
  try {
    const params: any = { ...query, page: pagination.page, pageSize: pagination.pageSize }
    if (dateRange.value) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    await exportOperationLogs(params as OperationLogQuery)
    ElMessage.success('导出成功，文件正在下载')
  } catch {
    // Error handled by interceptor
  } finally {
    exportLoading.value = false
  }
}

// Detail dialog
const detailVisible = ref(false)
const detail = ref<Partial<OperationLog>>({})

const formattedParams = computed(() => {
  try {
    const parsed = JSON.parse(detail.value.requestParams || 'null')
    return parsed ? JSON.stringify(parsed, null, 2) : ''
  } catch {
    return detail.value.requestParams || ''
  }
})

const formattedResult = computed(() => {
  try {
    const parsed = JSON.parse(detail.value.responseResult || 'null')
    return parsed ? JSON.stringify(parsed, null, 2) : ''
  } catch {
    return detail.value.responseResult || ''
  }
})

async function openDetail(row: OperationLog) {
  try {
    const res = await getOperationLogById(row.id)
    detail.value = res?.data || res || row
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.operation-log {
  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .json-section {
    margin-top: 16px;

    h4 {
      font-size: 14px;
      color: #303133;
      margin-bottom: 8px;
    }

    .json-block {
      background: #f5f7fa;
      padding: 12px 16px;
      border-radius: 4px;
      font-size: 12px;
      line-height: 1.6;
      overflow-x: auto;
      max-height: 240px;
      white-space: pre-wrap;
      word-break: break-all;
      color: #303133;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    }
  }
}
</style>

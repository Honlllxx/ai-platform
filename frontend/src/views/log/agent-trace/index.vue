<template>
  <div class="page-container agent-trace-page">
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
          <el-input v-model="query.username" placeholder="输入用户名" clearable style="width: 140px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px;">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
            <el-option label="运行中" value="running" />
          </el-select>
        </el-form-item>
        <el-form-item label="Trace ID">
          <el-input v-model="query.traceId" placeholder="输入 Trace ID" clearable style="width: 180px;" />
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
        <el-table-column prop="traceId" label="Trace ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sessionId" label="Session ID" width="180" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="query" label="查询内容" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总耗时" width="100" align="center">
          <template #default="{ row }">
            {{ row.totalDuration ? row.totalDuration + 's' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Token 数" width="90" align="center">
          <template #default="{ row }">
            {{ row.totalTokens || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
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
    <el-dialog
      v-model="detailVisible"
      :title="`Agent 调用链路 - ${currentTrace?.traceId}`"
      width="900px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <!-- Summary Info -->
      <div class="trace-summary">
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="用户">{{ currentTrace?.username }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(currentTrace?.status)" size="small">
              {{ statusLabel(currentTrace?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总耗时">{{ currentTrace?.totalDuration }}s</el-descriptions-item>
          <el-descriptions-item label="Token 消耗">{{ currentTrace?.totalTokens }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ currentTrace?.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ currentTrace?.endTime }}</el-descriptions-item>
          <el-descriptions-item label="查询内容" :span="3">{{ currentTrace?.query }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- View Switcher -->
      <div class="view-switcher">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="timeline">
            <el-icon><Sort /></el-icon>
            时间线视图
          </el-radio-button>
          <el-radio-button value="gantt">
            <el-icon><Histogram /></el-icon>
            甘特图
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- Trace Content -->
      <div v-loading="loadingDetail" class="trace-content">
        <!-- Timeline View -->
        <TraceTimeline
          v-if="viewMode === 'timeline'"
          :nodes="traceNodes"
          :expanded-nodes="expandedNodes"
          @toggle="handleNodeToggle"
        />

        <!-- Gantt View -->
        <TraceGantt
          v-else
          :nodes="traceNodes"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { Search, Refresh, View, Sort, Histogram } from '@element-plus/icons-vue'
import { getAgentTraces, getTraceTree, type TraceInfo, type TraceNode } from '@/api/agent-trace'
import { useTable } from '@/composables/useTable'
import TraceTimeline from '@/components/trace/TraceTimeline.vue'
import TraceGantt from '@/components/trace/TraceGantt.vue'

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
} = useTable<TraceInfo>({
  fetchApi: getAgentTraces,
  defaultQuery: { username: '', status: '', traceId: '' }
})

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
  baseReset({ username: '', status: '', traceId: '' })
}

// Detail dialog
const detailVisible = ref(false)
const loadingDetail = ref(false)
const viewMode = ref<'timeline' | 'gantt'>('timeline')
const currentTrace = ref<TraceInfo | null>(null)
const traceNodes = ref<TraceNode[]>([])
const expandedNodes = ref<Set<string>>(new Set())

function statusTag(status?: string) {
  const map: Record<string, string> = { running: 'warning', success: 'success', fail: 'danger' }
  return map[status || ''] || 'info'
}

function statusLabel(status?: string) {
  const map: Record<string, string> = { running: '运行中', success: '成功', fail: '失败' }
  return map[status || ''] || status || ''
}

function expandAll(nodes: TraceNode[], set: Set<string>) {
  for (const node of nodes) {
    set.add(node.id)
    if (node.children && node.children.length > 0) {
      expandAll(node.children, set)
    }
  }
}

async function openDetail(trace: TraceInfo) {
  currentTrace.value = trace
  detailVisible.value = true
  viewMode.value = 'timeline'

  // Load trace tree
  loadingDetail.value = true
  try {
    const res = await getTraceTree(trace.traceId)
    traceNodes.value = (res?.data || res || []) as TraceNode[]
    // Auto-expand all nodes
    const newSet = new Set<string>()
    expandAll(traceNodes.value, newSet)
    expandedNodes.value = newSet
  } catch {
    // If tree API fails, use nodes from trace object
    traceNodes.value = trace.nodes || []
    const newSet = new Set<string>()
    expandAll(traceNodes.value, newSet)
    expandedNodes.value = newSet
  } finally {
    loadingDetail.value = false
  }
}

function handleNodeToggle(node: TraceNode) {
  const newSet = new Set(expandedNodes.value)
  if (newSet.has(node.id)) {
    newSet.delete(node.id)
  } else {
    newSet.add(node.id)
  }
  expandedNodes.value = newSet
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.agent-trace-page {
  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .trace-summary {
    margin-bottom: 16px;
  }

  .view-switcher {
    margin-bottom: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .trace-content {
    min-height: 200px;
    max-height: 55vh;
    overflow-y: auto;
  }
}
</style>

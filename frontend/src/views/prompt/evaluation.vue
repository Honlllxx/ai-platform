<template>
  <div class="page-container evaluation-page">
    <div class="page-header">
      <h2>自动化评测</h2>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">评测总数</div>
      </div>
      <div class="stat-card stat-card-primary">
        <div class="stat-value">{{ stats.avgAccuracy }}%</div>
        <div class="stat-label">平均准确率</div>
      </div>
      <div class="stat-card stat-card-success">
        <div class="stat-value">{{ stats.bestF1 }}%</div>
        <div class="stat-label">最佳 F1 分数</div>
      </div>
      <div class="stat-card stat-card-warning">
        <div class="stat-value">{{ stats.runningCount }}</div>
        <div class="stat-label">运行中</div>
      </div>
    </div>

    <!-- Action Bar -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建评测</el-button>
        <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
      </div>

      <!-- Chart: Metrics Comparison -->
      <div class="chart-section" v-if="chartData.length > 0">
        <h4 class="chart-title">评测指标对比</h4>
        <div class="chart-container">
          <div class="chart-bars">
            <div
              v-for="(item, idx) in chartData"
              :key="idx"
              class="chart-group"
            >
              <div class="chart-group-label">{{ item.taskName }}</div>
              <div class="chart-group-bars">
                <div class="chart-bar-wrap" v-for="metric in chartMetrics" :key="metric.key">
                  <div class="chart-bar-label">{{ metric.label }}</div>
                  <div class="chart-bar-track">
                    <div
                      class="chart-bar-fill"
                      :style="{ width: item[metric.key] + '%', backgroundColor: metric.color }"
                    >
                      <span class="chart-bar-text">{{ item[metric.key] }}%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Table -->
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%; margin-top: 16px;">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="taskName" label="评测任务" min-width="160" />
        <el-table-column prop="promptVersionName" label="提示词版本" min-width="140" />
        <el-table-column label="数据集规模" width="110" align="center">
          <template #default="{ row }">
            <span>{{ row.datasetSize }} 条</span>
          </template>
        </el-table-column>
        <el-table-column label="准确率" width="140" align="center">
          <template #default="{ row }">
            <div class="metric-bar-cell">
              <span class="metric-value">{{ row.accuracy }}%</span>
              <div class="mini-progress">
                <div
                  class="mini-progress-fill"
                  :style="{ width: row.accuracy + '%' }"
                  :class="getAccuracyClass(row.accuracy)"
                />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="精确率" width="100" align="center">
          <template #default="{ row }">
            <span class="metric-text">{{ row.precision }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="召回率" width="100" align="center">
          <template #default="{ row }">
            <span class="metric-text">{{ row.recall }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="F1 分数" width="110" align="center">
          <template #default="{ row }">
            <span class="metric-text f1-score">{{ row.f1Score }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="evalStatusType(row.status)">
              {{ evalStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="center">
          <template #default="{ row }">
            {{ formatDuration(row.duration) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(v: number) => { pagination.page = v; fetchData() }"
          @size-change="(v: number) => { pagination.pageSize = v; pagination.page = 1; fetchData() }"
        />
      </div>
    </div>

    <!-- Create Evaluation Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="新建评测"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="evalForm" :rules="evalRules" label-width="110px">
        <el-form-item label="评测任务名称" prop="taskName">
          <el-input v-model="evalForm.taskName" placeholder="请输入评测任务名称" />
        </el-form-item>
        <el-form-item label="提示词版本" prop="promptVersionId">
          <el-select v-model="evalForm.promptVersionId" placeholder="请选择提示词版本" style="width: 100%">
            <el-option
              v-for="pv in promptVersionOptions"
              :key="pv.id"
              :label="`${pv.name} (${pv.version})`"
              :value="pv.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="评测数据集" prop="datasetName">
          <el-select v-model="evalForm.datasetName" placeholder="请选择数据集" style="width: 100%">
            <el-option label="合同评测集 v2 (500条)" value="contract_eval_v2" />
            <el-option label="发票评测集 v1 (300条)" value="invoice_eval_v1" />
            <el-option label="提单评测集 v1 (200条)" value="bol_eval_v1" />
            <el-option label="综合评测集 v3 (1000条)" value="comprehensive_eval_v3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRunEvaluation">开始评测</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import type { EvaluationItem } from '@/api/prompt'
import { getEvaluations, runEvaluation, getPromptVersions } from '@/api/prompt'

// ---------- Stats ----------
const stats = reactive({
  total: 0,
  avgAccuracy: 0,
  bestF1: 0,
  runningCount: 0
})

function computeStats(data: EvaluationItem[]) {
  stats.total = data.length
  const completed = data.filter(d => d.status === 'completed')
  if (completed.length > 0) {
    stats.avgAccuracy = +(completed.reduce((s, d) => s + d.accuracy, 0) / completed.length).toFixed(1)
    stats.bestF1 = +Math.max(...completed.map(d => d.f1Score)).toFixed(1)
  } else {
    stats.avgAccuracy = 0
    stats.bestF1 = 0
  }
  stats.runningCount = data.filter(d => d.status === 'running').length
}

// ---------- Status Helpers ----------
function evalStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: 'info',
    running: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

function evalStatusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待运行',
    running: '运行中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds}s`
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}m ${s}s`
}

function getAccuracyClass(val: number): string {
  if (val >= 90) return 'accuracy-high'
  if (val >= 75) return 'accuracy-mid'
  return 'accuracy-low'
}

// ---------- Pagination & Data ----------
const loading = ref(false)
const tableData = ref<EvaluationItem[]>([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

// ---------- Data Fetching ----------

async function fetchData() {
  loading.value = true
  try {
    const res = await getEvaluations({ page: pagination.page, pageSize: pagination.pageSize })
    const result = res?.data || res || {}
    const records = (result.records || result.list || result.data || []) as EvaluationItem[]
    tableData.value = records
    pagination.total = result.total || 0
    computeStats(records)
  } catch (e: any) {
    console.error('Failed to load evaluations:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ---------- Chart ----------
const chartMetrics = [
  { key: 'accuracy', label: '准确率', color: '#409eff' },
  { key: 'precision', label: '精确率', color: '#67c23a' },
  { key: 'recall', label: '召回率', color: '#e6a23c' },
  { key: 'f1Score', label: 'F1', color: '#f56c6c' }
]

const chartData = computed(() => {
  return tableData.value.filter(d => d.status === 'completed').map(d => ({
    taskName: d.taskName.length > 10 ? d.taskName.substring(0, 10) + '...' : d.taskName,
    accuracy: d.accuracy,
    precision: d.precision,
    recall: d.recall,
    f1Score: d.f1Score
  } as Record<string, number | string>))
})

// ---------- Create Dialog ----------
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const promptVersionOptions = ref<Array<{ id: number; name: string; version: string }>>([])

const evalForm = reactive({
  taskName: '',
  promptVersionId: null as number | null,
  datasetName: ''
})

const evalRules: FormRules = {
  taskName: [{ required: true, message: '请输入评测任务名称', trigger: 'blur' }],
  promptVersionId: [{ required: true, message: '请选择提示词版本', trigger: 'change' }],
  datasetName: [{ required: true, message: '请选择数据集', trigger: 'change' }]
}

async function openCreateDialog() {
  // Load prompt versions from API for the select dropdown
  try {
    const res = await getPromptVersions({ page: 1, pageSize: 200 })
    const result = res?.data || res || {}
    const pvList = ((result.records || result.list || result.data || []) as any[])
    promptVersionOptions.value = pvList.map((pv: any) => ({
      id: pv.id,
      name: pv.name,
      version: pv.version,
    }))
  } catch {
    promptVersionOptions.value = []
  }
  evalForm.taskName = ''
  evalForm.promptVersionId = null
  evalForm.datasetName = ''
  dialogVisible.value = true
}

async function handleRunEvaluation() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await runEvaluation({
      taskName: evalForm.taskName,
      promptVersionId: evalForm.promptVersionId!,
      datasetName: evalForm.datasetName,
    } as any)
    ElMessage.success('评测任务已创建，开始运行...')
    dialogVisible.value = false
    pagination.page = 1
    fetchData()
  } catch {
    // Error already handled by Axios interceptor
  }
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.evaluation-page {
  .page-header {
    margin-bottom: 16px;
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }

  .stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 16px;
  }

  .stat-card {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    text-align: center;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 6px;
    }

    &.stat-card-primary .stat-value { color: #409eff; }
    &.stat-card-success .stat-value { color: #67c23a; }
    &.stat-card-warning .stat-value { color: #e6a23c; }
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  // Chart
  .chart-section {
    .chart-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
    }
  }

  .chart-container {
    background: #fafafa;
    border-radius: 8px;
    padding: 16px;
  }

  .chart-bars {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .chart-group {
    display: flex;
    align-items: flex-start;
    gap: 10px;

    .chart-group-label {
      width: 130px;
      font-size: 12px;
      color: #606266;
      text-align: right;
      padding-top: 2px;
      flex-shrink: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .chart-group-bars {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
  }

  .chart-bar-wrap {
    display: flex;
    align-items: center;
    gap: 8px;

    .chart-bar-label {
      width: 50px;
      font-size: 11px;
      color: #909399;
      text-align: right;
      flex-shrink: 0;
    }

    .chart-bar-track {
      flex: 1;
      height: 20px;
      background: #e9ecef;
      border-radius: 4px;
      overflow: hidden;
      position: relative;
    }

    .chart-bar-fill {
      height: 100%;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      padding-right: 6px;
      transition: width 0.6s ease;
      min-width: 36px;

      .chart-bar-text {
        font-size: 11px;
        color: #fff;
        font-weight: 600;
        white-space: nowrap;
      }
    }
  }

  // Table metrics
  .metric-bar-cell {
    .metric-value {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
    }

    .mini-progress {
      height: 6px;
      background: #e9ecef;
      border-radius: 3px;
      margin-top: 4px;
      overflow: hidden;

      .mini-progress-fill {
        height: 100%;
        border-radius: 3px;
        transition: width 0.4s ease;

        &.accuracy-high { background: #67c23a; }
        &.accuracy-mid { background: #409eff; }
        &.accuracy-low { background: #f56c6c; }
      }
    }
  }

  .metric-text {
    font-size: 13px;
    font-weight: 600;
    color: #606266;

    &.f1-score {
      color: #409eff;
    }
  }
}
</style>

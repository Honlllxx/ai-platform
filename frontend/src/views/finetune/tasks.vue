<template>
  <div class="page-container finetune-tasks">
    <div class="page-header">
      <h2>LoRA 微调任务</h2>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon stat-icon-total">
          <el-icon :size="24"><List /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ taskStats.total }}</div>
          <div class="stat-label">总任务</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-running">
          <el-icon :size="24"><Loading /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ taskStats.running }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-completed">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ taskStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-failed">
          <el-icon :size="24"><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ taskStats.failed }}</div>
          <div class="stat-label">失败</div>
        </div>
      </div>
    </div>

    <!-- Table Card -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建微调任务</el-button>
        <div class="toolbar-right">
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            inactive-text="手动"
            @change="onAutoRefreshToggle"
          />
          <span class="refresh-hint" v-if="autoRefresh">
            <el-icon class="pulse-icon"><Loading /></el-icon> 每5秒刷新
          </span>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-detail">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="任务ID">{{ row.id }}</el-descriptions-item>
                <el-descriptions-item label="基础模型">{{ row.baseModel }}</el-descriptions-item>
                <el-descriptions-item label="训练轮次">{{ row.currentEpoch }} / {{ row.epochs }}</el-descriptions-item>
                <el-descriptions-item label="学习率">{{ row.learningRate }}</el-descriptions-item>
                <el-descriptions-item label="数据集">{{ row.datasetName }}</el-descriptions-item>
                <el-descriptions-item label="数据量">{{ row.datasetSize }} 条</el-descriptions-item>
                <el-descriptions-item v-if="row.metrics?.accuracy !== undefined" label="准确率">
                  <span class="metric-highlight">{{ row.metrics.accuracy }}%</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="row.metrics?.f1 !== undefined" label="F1 分数">
                  <span class="metric-highlight">{{ row.metrics.f1 }}%</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="row.outputModelName" label="输出模型" :span="2">
                  <el-tag type="success">{{ row.outputModelName }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item v-if="row.errorMessage" label="错误信息" :span="2">
                  <span class="error-text">{{ row.errorMessage }}</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="row.startTime" label="开始时间">{{ row.startTime }}</el-descriptions-item>
                <el-descriptions-item v-if="row.endTime" label="结束时间">{{ row.endTime }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" />
        <el-table-column label="基础模型" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.baseModel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据集" min-width="140">
          <template #default="{ row }">
            <span>{{ row.datasetName }}</span>
            <span class="text-secondary ml-8">({{ row.datasetSize }})</span>
          </template>
        </el-table-column>
        <el-table-column label="训练进度" width="180" align="center">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="row.progress"
                :status="row.status === 'failed' ? 'exception' : row.status === 'completed' ? 'success' : undefined"
                :stroke-width="8"
              />
              <span class="progress-text">{{ row.currentEpoch }}/{{ row.epochs }} 轮</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Loss" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.loss !== undefined" class="loss-value">{{ row.loss.toFixed(4) }}</span>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <span
                v-if="row.status === 'running'"
                class="status-pulse"
              />
              <el-tag size="small" :type="taskStatusType(row.status)">
                {{ taskStatusLabel(row.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">
            {{ row.startTime || row.createTime }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">
            {{ formatDuration(row.duration) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'running' || row.status === 'pending'"
              type="danger"
              link
              :icon="VideoPause"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <span v-else class="text-secondary">-</span>
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
          @current-change="(v: number) => { pagination.page = v; fetchData() }"
          @size-change="(v: number) => { pagination.pageSize = v; pagination.page = 1; fetchData() }"
        />
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="创建微调任务"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="基础模型" prop="baseModel">
          <el-select v-model="form.baseModel" placeholder="请选择基础模型" style="width: 100%">
            <el-option label="Qwen2-7B" value="Qwen2-7B" />
            <el-option label="Qwen2-14B" value="Qwen2-14B" />
            <el-option label="Llama3-8B" value="Llama3-8B" />
            <el-option label="ChatGLM3-6B" value="ChatGLM3-6B" />
            <el-option label="Baichuan2-13B" value="Baichuan2-13B" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集" prop="datasetName">
          <el-select v-model="form.datasetName" placeholder="请选择数据集" style="width: 100%">
            <el-option label="合同文档数据集 v2" value="contract_docs_v2" />
            <el-option label="发票数据集 v1" value="invoice_dataset_v1" />
            <el-option label="提单数据集 v1" value="bol_dataset_v1" />
            <el-option label="综合文档数据集 v3" value="comprehensive_docs_v3" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="训练轮次" prop="epochs">
              <el-input-number v-model="form.epochs" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学习率" prop="learningRate">
              <el-input v-model="form.learningRate" placeholder="1e-4" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">开始微调</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, List, Loading, CircleCheck, CircleClose, VideoPause } from '@element-plus/icons-vue'
import type { FinetuneTask } from '@/api/finetune'

// ---------- Stats ----------
const taskStats = reactive({
  total: 0,
  running: 0,
  completed: 0,
  failed: 0
})

function computeStats(data: FinetuneTask[]) {
  taskStats.total = data.length
  taskStats.running = data.filter(d => d.status === 'running').length
  taskStats.completed = data.filter(d => d.status === 'completed').length
  taskStats.failed = data.filter(d => d.status === 'failed').length
}

// ---------- Status Helpers ----------
function taskStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: 'info',
    running: 'warning',
    completed: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

function taskStatusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '等待中',
    running: '运行中',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return map[status] || status
}

function formatDuration(seconds?: number): string {
  if (!seconds) return '-'
  if (seconds < 60) return `${seconds}s`
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}m ${s}s`
}

// ---------- Pagination & Data ----------
const loading = ref(false)
const tableData = ref<FinetuneTask[]>([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

// ---------- Mock Data ----------
let taskIdCounter = 20

function generateMockTasks(): FinetuneTask[] {
  const records: FinetuneTask[] = []
  const baseModels = ['Qwen2-7B', 'Qwen2-14B', 'Llama3-8B', 'ChatGLM3-6B', 'Baichuan2-13B']
  const datasets = ['contract_docs_v2', 'invoice_dataset_v1', 'bol_dataset_v1', 'comprehensive_docs_v3']
  const datasetSizes = [500, 300, 200, 800]

  for (let i = 1; i <= 15; i++) {
    const statusIdx = i % 5
    let status: FinetuneTask['status'] = 'completed'
    let progress = 100
    let currentEpoch = 3
    let loss: number | undefined = 0.0234 + Math.random() * 0.05
    let metrics: { accuracy: number; f1: number } | undefined

    if (statusIdx === 0) { status = 'running'; progress = Math.floor(Math.random() * 80) + 10; currentEpoch = progress >= 33 ? (progress >= 66 ? 2 : 1) : 0; loss = 0.8 - Math.random() * 0.4 }
    else if (statusIdx === 1) { status = 'completed'; progress = 100; currentEpoch = 3; metrics = { accuracy: +(85 + Math.random() * 14).toFixed(1), f1: +(83 + Math.random() * 16).toFixed(1) }; loss = +(0.01 + Math.random() * 0.05).toFixed(4) }
    else if (statusIdx === 2) { status = 'failed'; progress = Math.floor(Math.random() * 60) + 5; currentEpoch = progress >= 33 ? 1 : 0; loss = undefined }
    else if (statusIdx === 3) { status = 'cancelled'; progress = Math.floor(Math.random() * 40) + 5; currentEpoch = 0; loss = undefined }
    else { status = 'pending'; progress = 0; currentEpoch = 0; loss = undefined }

    records.push({
      id: i,
      taskName: `微调任务-${String(i).padStart(2, '0')}`,
      baseModel: baseModels[i % baseModels.length],
      datasetName: datasets[i % datasets.length].replace(/_/g, ' ').replace(/(\w+)$/, m => m.charAt(0).toUpperCase() + m.slice(1)),
      datasetSize: datasetSizes[i % datasets.length],
      status,
      progress,
      epochs: 3,
      currentEpoch,
      learningRate: 1e-4,
      loss,
      metrics,
      outputModelName: status === 'completed' ? `lora-${baseModels[i % baseModels.length].toLowerCase()}-v${1 + (i % 3)}` : undefined,
      startTime: `2025-0${Math.min(9, (i % 3) + 5)}-${String((i * 3) % 28 + 1).padStart(2, '0')} ${String(9 + (i % 14)).padStart(2, '0')}:00:00`,
      endTime: status === 'completed' ? `2025-0${Math.min(9, (i % 3) + 5)}-${String((i * 3) % 28 + 1).padStart(2, '0')} ${String(14 + (i % 10)).padStart(2, '0')}:00:00` : undefined,
      duration: status === 'completed' ? Math.floor(Math.random() * 7200) + 1800 : 0,
      errorMessage: status === 'failed' ? '内存不足: CUDA out of memory. Tried to allocate 2.00 GiB' : undefined,
      createTime: `2025-0${Math.min(9, (i % 3) + 5)}-${String((i * 3) % 28 + 1).padStart(2, '0')} ${String(8 + (i % 9)).padStart(2, '0')}:30:00`
    })
  }
  return records
}

const allMockTasks = generateMockTasks()

function fetchData() {
  loading.value = true
  setTimeout(() => {
    computeStats(allMockTasks)
    const start = (pagination.page - 1) * pagination.pageSize
    tableData.value = allMockTasks.slice(start, start + pagination.pageSize)
    pagination.total = allMockTasks.length
    loading.value = false
  }, 200)
}

// ---------- Auto Refresh ----------
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function onAutoRefreshToggle(val: boolean) {
  if (val) {
    refreshTimer = setInterval(() => {
      // Simulate progress for running tasks
      allMockTasks.forEach(task => {
        if (task.status === 'running') {
          task.progress = Math.min(100, task.progress + Math.floor(Math.random() * 8) + 1)
          task.currentEpoch = task.progress >= 33 ? (task.progress >= 66 ? 2 : 1) : 0
          if (task.loss !== undefined) {
            task.loss = +Math.max(0.01, task.loss - Math.random() * 0.05).toFixed(4)
          }
          if (task.progress >= 100) {
            task.status = 'completed'
            task.progress = 100
            task.currentEpoch = 3
            task.loss = +(0.01 + Math.random() * 0.05).toFixed(4)
            task.metrics = {
              accuracy: +(85 + Math.random() * 14).toFixed(1),
              f1: +(83 + Math.random() * 16).toFixed(1)
            }
            task.outputModelName = `lora-${task.baseModel.toLowerCase()}-v${Math.ceil(Math.random() * 3)}`
            task.duration = Math.floor(Math.random() * 7200) + 1800
            task.endTime = new Date().toISOString().replace('T', ' ').substring(0, 19)
            ElMessage.success(`微调任务 "${task.taskName}" 训练完成`)
          }
        }
      })
      fetchData()
    }, 5000)
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }
}

// ---------- Cancel ----------
async function handleCancel(row: FinetuneTask) {
  await ElMessageBox.confirm(
    `确定要取消微调任务 "${row.taskName}" 吗？`,
    '取消微调',
    { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }
  )
  row.status = 'cancelled'
  row.progress = row.progress || 5
  ElMessage.warning(`微调任务 "${row.taskName}" 已取消`)
  fetchData()
}

// ---------- Create Dialog ----------
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  taskName: '',
  baseModel: '',
  datasetName: '',
  epochs: 3,
  learningRate: '1e-4'
})

const formRules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  baseModel: [{ required: true, message: '请选择基础模型', trigger: 'change' }],
  datasetName: [{ required: true, message: '请选择数据集', trigger: 'change' }],
  epochs: [{ required: true, message: '请设置训练轮次', trigger: 'blur' }],
  learningRate: [{ required: true, message: '请输入学习率', trigger: 'blur' }]
}

function openCreateDialog() {
  form.taskName = ''
  form.baseModel = ''
  form.datasetName = ''
  form.epochs = 3
  form.learningRate = '1e-4'
  dialogVisible.value = true
}

async function handleCreateTask() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
  const newTask: FinetuneTask = {
    id: taskIdCounter++,
    taskName: form.taskName,
    baseModel: form.baseModel,
    datasetName: form.datasetName.replace(/_/g, ' ').replace(/(\w+)$/, m => m.charAt(0).toUpperCase() + m.slice(1)),
    datasetSize: form.datasetName.includes('contract') ? 500 : form.datasetName.includes('invoice') ? 300 : form.datasetName.includes('bol') ? 200 : 800,
    status: 'running',
    progress: 0,
    epochs: form.epochs,
    currentEpoch: 0,
    learningRate: parseFloat(form.learningRate),
    loss: 0.85,
    startTime: now,
    duration: 0,
    createTime: now
  }

  allMockTasks.unshift(newTask)
  ElMessage.success('微调任务已创建，训练开始...')
  dialogVisible.value = false
  fetchData()
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped lang="scss">
.finetune-tasks {
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
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.stat-icon-total { background: #ecf5ff; color: #409eff; }
      &.stat-icon-running { background: #fdf6ec; color: #e6a23c; }
      &.stat-icon-completed { background: #f0f9eb; color: #67c23a; }
      &.stat-icon-failed { background: #fef0f0; color: #f56c6c; }
    }

    .stat-info {
      .stat-value {
        font-size: 26px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }

  .table-toolbar {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 10px;

      .refresh-hint {
        font-size: 12px;
        color: #e6a23c;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  // Progress cell
  .progress-cell {
    .progress-text {
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
      display: block;
    }
  }

  .loss-value {
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 13px;
    color: #e6a23c;
    font-weight: 600;
  }

  // Status pulse animation
  .status-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  .status-pulse {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #e6a23c;
    animation: pulse-animation 1.5s ease-in-out infinite;
  }

  @keyframes pulse-animation {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.4; transform: scale(1.3); }
  }

  .pulse-icon {
    animation: pulse-animation 1.5s ease-in-out infinite;
  }

  // Expand detail
  .expand-detail {
    padding: 16px 24px;
    background: #fafafa;

    .metric-highlight {
      color: #409eff;
      font-weight: 600;
    }

    .error-text {
      color: #f56c6c;
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 12px;
    }
  }

  .text-secondary {
    color: #909399;
    font-size: 12px;
  }

  .ml-8 {
    margin-left: 8px;
  }
}
</style>

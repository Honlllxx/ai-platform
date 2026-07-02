<template>
  <div class="page-container pipeline-detail">
    <!-- Page Header -->
    <div class="detail-header">
      <el-page-header @back="goBack" :title="'返回流水线列表'">
        <template #content>
          <div class="header-content">
            <span class="pipeline-title">{{ pipelineInfo.documentName || '流水线详情' }}</span>
            <el-tag :type="statusTagType(pipelineInfo.status)" size="small" effect="dark">
              {{ statusLabel(pipelineInfo.status) }}
            </el-tag>
          </div>
        </template>
        <template #extra>
          <div class="header-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatDuration(pipelineInfo.duration) }}
            </span>
            <span class="meta-item">
              进度 {{ pipelineInfo.completedSteps }} / {{ pipelineInfo.totalSteps }}
            </span>
          </div>
        </template>
      </el-page-header>
    </div>

    <!-- Pipeline Status Overview -->
    <div class="overview-card" v-if="pipelineInfo.id">
      <el-descriptions :column="4" size="small" border>
        <el-descriptions-item label="文档名称">{{ pipelineInfo.documentName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(pipelineInfo.status)" size="small" effect="dark">
            {{ statusLabel(pipelineInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前步骤">{{ pipelineInfo.currentStep }}</el-descriptions-item>
        <el-descriptions-item label="总耗时">{{ formatDuration(pipelineInfo.duration) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ pipelineInfo.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ pipelineInfo.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总步骤">{{ pipelineInfo.totalSteps }}</el-descriptions-item>
        <el-descriptions-item label="进度">
          <el-progress
            :percentage="progressPercent"
            :color="progressColor"
            :stroke-width="12"
            style="width: 150px;"
          />
        </el-descriptions-item>
        <el-descriptions-item v-if="pipelineInfo.error" label="错误信息" :span="4">
          <span class="error-text">{{ pipelineInfo.error }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- Tab Content -->
    <div class="detail-tabs" v-if="pipelineInfo.id">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="状态图" name="stateGraph">
          <template #label>
            <span class="tab-label">
              <el-icon><Share /></el-icon>
              状态图
            </span>
          </template>
          <StateGraph :graph-data="pipelineInfo.stateGraph" />
        </el-tab-pane>

        <el-tab-pane label="提取结果" name="extraction">
          <template #label>
            <span class="tab-label">
              <el-icon><List /></el-icon>
              提取结果
            </span>
          </template>
          <ExtractionTable :fields="pipelineInfo.extractionResults" :pipeline-id="pipelineInfo.id" />
        </el-tab-pane>

        <el-tab-pane label="校验报告" name="validation">
          <template #label>
            <span class="tab-label">
              <el-icon><Checked /></el-icon>
              校验报告
            </span>
          </template>
          <ValidationReport :items="pipelineInfo.validationReport" :pipeline-id="pipelineInfo.id" />
        </el-tab-pane>

        <el-tab-pane label="反思修正" name="reflection">
          <template #label>
            <span class="tab-label">
              <el-icon><RefreshRight /></el-icon>
              反思修正
            </span>
          </template>
          <ReflectionView :items="pipelineInfo.reflectionData" :pipeline-id="pipelineInfo.id" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Clock, Share, List, Checked, RefreshRight
} from '@element-plus/icons-vue'
import { getPipeline, type PipelineDetail, type StateGraphData, type ExtractionField, type ValidationItem, type ReflectionItem } from '@/api/pipeline'
import StateGraph from './components/StateGraph.vue'
import ExtractionTable from './components/ExtractionTable.vue'
import ValidationReport from './components/ValidationReport.vue'
import ReflectionView from './components/ReflectionView.vue'

const route = useRoute()
const router = useRouter()

const pipelineId = computed(() => Number(route.params.id))
const activeTab = ref('stateGraph')
const loading = ref(false)

const pipelineInfo = reactive<PipelineDetail>({
  id: 0,
  documentId: 0,
  documentName: '',
  status: 'pending',
  currentStep: '',
  totalSteps: 0,
  completedSteps: 0,
  duration: 0,
  startTime: '',
  endTime: '',
  error: '',
  stateGraph: { nodes: [], edges: [] },
  extractionResults: [],
  validationReport: [],
  reflectionData: []
})

const progressPercent = computed(() => {
  if (pipelineInfo.totalSteps === 0) return 0
  return Math.round((pipelineInfo.completedSteps / pipelineInfo.totalSteps) * 100)
})

const progressColor = computed(() => {
  if (pipelineInfo.status === 'failed') return '#f56c6c'
  if (pipelineInfo.status === 'completed') return '#67c23a'
  if (pipelineInfo.status === 'running') return '#409eff'
  return '#909399'
})

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
  if (!seconds) return '-'
  if (seconds < 60) return `${seconds}s`
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins < 60) return `${mins}m ${secs}s`
  const hours = Math.floor(mins / 60)
  const remainingMins = mins % 60
  return `${hours}h ${remainingMins}m`
}

function goBack() {
  router.push('/pipeline/list')
}

function loadPipelineDetail() {
  loading.value = true
  // Simulate pipeline detail data
  pipelineInfo.id = pipelineId.value
  pipelineInfo.documentId = 1001
  pipelineInfo.documentName = `采购合同_v${pipelineId.value}.pdf`
  pipelineInfo.status = 'completed'
  pipelineInfo.currentStep = '数据校验'
  pipelineInfo.totalSteps = 7
  pipelineInfo.completedSteps = 7
  pipelineInfo.duration = 145
  pipelineInfo.startTime = '2026-06-29 09:15:30'
  pipelineInfo.endTime = '2026-06-29 09:17:55'

  pipelineInfo.stateGraph = {
    nodes: [
      { id: '1', label: '文档上传', type: 'tool', status: 'completed', duration: 2 },
      { id: '2', label: 'OCR 识别', type: 'tool', status: 'completed', duration: 15 },
      { id: '3', label: '版面分析', type: 'llm', status: 'completed', duration: 22 },
      { id: '4', label: '表格检测', type: 'llm', status: 'completed', duration: 18 },
      { id: '5', label: '字段提取', type: 'llm', status: 'completed', duration: 35 },
      { id: '6', label: '数据校验', type: 'decision', status: 'completed', duration: 28 },
      { id: '7', label: '结果输出', type: 'tool', status: 'completed', duration: 25 }
    ],
    edges: [
      { from: '1', to: '2' },
      { from: '2', to: '3' },
      { from: '2', to: '4' },
      { from: '3', to: '5' },
      { from: '4', to: '5' },
      { from: '5', to: '6' },
      { from: '6', to: '7' }
    ]
  }

  pipelineInfo.extractionResults = [
    { id: 1, fieldName: '合同编号', extractedValue: 'HT-2026-00158', confidence: 0.98, status: 'verified' as const, pageRef: 1 },
    { id: 2, fieldName: '甲方名称', extractedValue: '北京科技有限公司', confidence: 0.96, status: 'verified' as const, pageRef: 1 },
    { id: 3, fieldName: '乙方名称', extractedValue: '上海信息技术有限公司', confidence: 0.95, status: 'verified' as const, pageRef: 1 },
    { id: 4, fieldName: '合同金额', extractedValue: '1,200,000.00', confidence: 0.92, status: 'verified' as const, pageRef: 1 },
    { id: 5, fieldName: '签订日期', extractedValue: '2026年6月15日', confidence: 0.88, status: 'uncertain' as const, pageRef: 1 },
    { id: 6, fieldName: '交货地点', extractedValue: '北京市海淀区中关村科技园区', confidence: 0.85, status: 'uncertain' as const, pageRef: 1 },
    { id: 7, fieldName: '付款方式', extractedValue: '分期付款（30%-40%-30%）', confidence: 0.72, status: 'uncertain' as const, pageRef: 2 },
    { id: 8, fieldName: '质保期限', extractedValue: '三年', confidence: 0.65, status: 'rejected' as const, pageRef: 2 }
  ]

  pipelineInfo.validationReport = [
    { id: 1, rule: '金额格式校验', field: '合同金额', severity: 'error' as const, message: '金额字段包含逗号分隔符，需转换为纯数字格式', suggestion: '建议移除金额中的逗号，统一转换为数字类型' },
    { id: 2, rule: '日期格式校验', field: '签订日期', severity: 'warning' as const, message: '日期格式为中文格式，建议标准化为 ISO 8601 格式', suggestion: '建议转换为 YYYY-MM-DD 格式' },
    { id: 3, rule: '必填字段检查', field: '卖方联系人', severity: 'error' as const, message: '合同未提取到卖方联系人信息', suggestion: '请检查合同附件或手动补充卖方联系人' },
    { id: 4, rule: '质保期限校验', field: '质保期限', severity: 'warning' as const, message: '质保期限置信度过低 (0.65)，可能存在识别错误', suggestion: '建议人工复核质保条款内容' },
    { id: 5, rule: '数值范围校验', field: '合同金额', severity: 'info' as const, message: '合同金额在正常范围内', suggestion: '' },
    { id: 6, rule: '合同编号格式', field: '合同编号', severity: 'info' as const, message: '合同编号格式符合规范 (HT-YYYY-NNNNN)', suggestion: '' }
  ]

  pipelineInfo.reflectionData = [
    { id: 1, fieldName: '合同金额', beforeValue: '1,200,000.00', afterValue: '1200000.00', reason: 'LLM 反思修正：文本中金额含千位分隔符，去除逗号转换为标准数字', timestamp: '2026-06-29 09:16:45' },
    { id: 2, fieldName: '签订日期', beforeValue: '2026年6月15日', afterValue: '2026-06-15', reason: '日期标准化修正：将中文日期格式转换为 ISO 8601 格式', timestamp: '2026-06-29 09:17:05' },
    { id: 3, fieldName: '付款方式', beforeValue: '分期付款（30%-40%-30%）', afterValue: '付款分三期：合同签订后7日内支付30%，设备到货验收后支付40%，项目终验后支付30%', reason: 'LLM 反思修正：付款方式需从合同条款中解读完整信息，原始提取过于简略', timestamp: '2026-06-29 09:17:20' }
  ]

  loading.value = false

  // Try to fetch real data
  if (pipelineId.value) {
    getPipeline(pipelineId.value).then((res: any) => {
      const data = res?.data || res
      if (data && data.id) {
        Object.assign(pipelineInfo, data)
      }
    }).catch(() => {
      // Use mock data
    })
  }
}

onMounted(() => {
  loadPipelineDetail()
})

watch(pipelineId, () => {
  loadPipelineDetail()
})
</script>

<style scoped lang="scss">
.pipeline-detail {
  .detail-header {
    background: #fff;
    padding: 16px 20px;
    border-radius: 4px;
    margin-bottom: 16px;

    :deep(.el-page-header__content),
    :deep(.el-page-header__extra) {
      white-space: normal;
      overflow: visible;
    }
  }

  .header-content {
    display: flex;
    align-items: center;
    gap: 12px;

    .pipeline-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .header-meta {
    display: flex;
    align-items: center;
    gap: 20px;
    font-size: 13px;
    color: #606266;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .overview-card {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 16px;
  }

  .error-text {
    color: #f56c6c;
    font-weight: 500;
  }

  .detail-tabs {
    :deep(.el-tabs__content) {
      padding: 20px;
      min-height: 400px;
    }
  }

  .tab-label {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
</style>

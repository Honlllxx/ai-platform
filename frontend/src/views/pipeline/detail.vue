<template>
  <div class="page-container pipeline-detail" v-loading="loading">
    <!-- Error state -->
    <el-alert v-if="!loading && loadError" type="error" title="加载流水线详情失败" :closable="false" show-icon style="margin-bottom: 16px" />
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
const loadError = ref(false)

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

function formatDuration(seconds: number | null): string {
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

async function loadPipelineDetail() {
  loading.value = true
  loadError.value = false

  try {
    const res: any = await getPipeline(pipelineId.value)
    const data = res?.data || res
    if (data && data.id) {
      // Map API field names to frontend field names
      if (data.validationResults) data.validationReport = data.validationResults
      if (data.reflectionResults) data.reflectionData = data.reflectionResults
      if (!data.stateGraph) data.stateGraph = { nodes: [], edges: [] }
      Object.assign(pipelineInfo, data)
    } else {
      loadError.value = true
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
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

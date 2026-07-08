<template>
  <div class="page-container dashboard">
    <!-- Row 1: General Stats -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#409eff"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayOperations }}</div>
              <div class="stat-label">今日操作量</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#67c23a"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.agentCalls }}</div>
              <div class="stat-label">Agent 调用次数</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#e6a23c"><Connection /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeAgents }}</div>
              <div class="stat-label">活跃 Agent 数</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#f56c6c"><Cpu /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 2: Business Stats -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card business-stat">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value success">{{ stats.autoPassRate }}<span class="unit">%</span></div>
              <div class="stat-label">自动通过率</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#67c23a"><CircleCheck /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card business-stat">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value warning">{{ stats.manualCorrectionRate }}<span class="unit">%</span></div>
              <div class="stat-label">人工校正率</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#e6a23c"><WarningFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card business-stat">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value primary">¥{{ formatNumber(stats.costSavings) }}</div>
              <div class="stat-label">成本节省</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#409eff"><Money /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card business-stat">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.avgProcessingTime }}<span class="unit">秒</span></div>
              <div class="stat-label">平均处理耗时</div>
            </div>
            <el-icon :size="48" class="stat-icon" color="#909399"><Timer /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>
            <span>近 7 天文档处理量趋势</span>
          </template>
          <v-chart :option="dailyProcessingOption" style="height: 350px;" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>
            <span>文档类型分布</span>
          </template>
          <v-chart :option="docTypePieOption" style="height: 350px;" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>
            <span>近 7 天操作日志趋势</span>
          </template>
          <v-chart :option="trendOption" style="height: 350px;" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>
            <span>操作类型分布</span>
          </template>
          <v-chart :option="pieOption" style="height: 350px;" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent
} from 'echarts/components'
import {
  getDashboardStats,
  getDailyProcessing,
  getDocTypeDistribution,
  getOperationTrend,
  getOperationTypeDistribution,
} from '@/api/dashboard'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent
])

const stats = reactive({
  userCount: 0,
  todayOperations: 0,
  agentCalls: 0,
  activeAgents: 0,
  autoPassRate: 0,
  manualCorrectionRate: 0,
  costSavings: 0,
  avgProcessingTime: 0
})

const trendOption = ref({})
const pieOption = ref({})
const dailyProcessingOption = ref({})
const docTypePieOption = ref({})

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

async function loadDashboard() {
  // Load general stats
  try {
    const res = await getDashboardStats()
    const data = res?.data || res
    if (data) {
      stats.userCount = data.userCount || 0
      stats.todayOperations = data.todayOperations || 0
      stats.agentCalls = data.agentCalls || 0
      stats.activeAgents = data.activeAgents || 0
      stats.autoPassRate = data.autoPassRate || 0
      stats.manualCorrectionRate = data.manualCorrectionRate || 0
      stats.costSavings = data.costSavings || 0
      stats.avgProcessingTime = data.avgProcessingTime || 0
    }
  } catch {
    if (import.meta.env.DEV) {
      stats.userCount = 128
      stats.todayOperations = 356
      stats.agentCalls = 89
      stats.activeAgents = 5
      stats.autoPassRate = 87.5
      stats.manualCorrectionRate = 12.5
      stats.costSavings = 158000
      stats.avgProcessingTime = 3.2
    }
    ElMessage.warning('统计数据加载失败，请稍后重试')
  }

  // Daily processing trend
  try {
    const res = await getDailyProcessing()
    const data = res?.data || []
    const dates = data.map((d: any) => d.date)
    const autoPassed = data.map((d: any) => d.autoPassed)
    const manualCorrected = data.map((d: any) => d.manualCorrected)
    dailyProcessingOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['自动通过', '人工校正'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
      xAxis: { type: 'category', data: dates, boundaryGap: false },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          name: '自动通过',
          type: 'line',
          smooth: true,
          data: autoPassed,
          areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(103,194,58,0.3)' }, { offset: 1, color: 'rgba(103,194,58,0.05)' }] } },
          lineStyle: { color: '#67c23a', width: 2 },
          itemStyle: { color: '#67c23a' }
        },
        {
          name: '人工校正',
          type: 'line',
          smooth: true,
          data: manualCorrected,
          areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(230,162,60,0.3)' }, { offset: 1, color: 'rgba(230,162,60,0.05)' }] } },
          lineStyle: { color: '#e6a23c', width: 2 },
          itemStyle: { color: '#e6a23c' }
        }
      ]
    }
  } catch {
    if (import.meta.env.DEV) {
      dailyProcessingOption.value = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['自动通过', '人工校正'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: { type: 'category', data: ['06/23', '06/24', '06/25', '06/26', '06/27', '06/28', '06/29'], boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '自动通过', type: 'line', smooth: true, data: [15, 19, 13, 25, 17, 22, 17], lineStyle: { color: '#67c23a', width: 2 }, itemStyle: { color: '#67c23a' } },
          { name: '人工校正', type: 'line', smooth: true, data: [3, 3, 2, 3, 3, 3, 2], lineStyle: { color: '#e6a23c', width: 2 }, itemStyle: { color: '#e6a23c' } }
        ]
      }
    }
  }

  // Document type distribution
  try {
    const res = await getDocTypeDistribution()
    const data = res?.data || []
    docTypePieOption.value = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center' },
      series: [{
        name: '文档类型',
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data: data.map((d: any) => ({ name: d.type, value: d.value })),
        itemStyle: { borderColor: '#fff', borderWidth: 2 }
      }]
    }
  } catch {
    if (import.meta.env.DEV) {
      docTypePieOption.value = {
        tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
        legend: { orient: 'vertical', right: '5%', top: 'center' },
        series: [{
          name: '文档类型', type: 'pie', radius: ['45%', '75%'], center: ['40%', '50%'],
          data: [{ name: '合同', value: 45 }, { name: '发票', value: 62 }, { name: '提单', value: 28 }, { name: '报关单', value: 15 }, { name: '其他', value: 8 }]
        }]
      }
    }
  }

  // Operation trend (existing)
  try {
    const trendRes = await getOperationTrend()
    const trendData = trendRes?.data || trendRes || []
    const dates = trendData.map((d: any) => d.date).reverse()
    const values = trendData.map((d: any) => d.count).reverse()
    trendOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: dates.length ? dates : ['06/23', '06/24', '06/25', '06/26', '06/27', '06/28', '06/29'], boundaryGap: false },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        name: '操作次数', type: 'line', smooth: true,
        data: values.length ? values : [42, 55, 38, 63, 48, 57, 53],
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }] } },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' }
      }]
    }
  } catch {
    if (import.meta.env.DEV) {
      trendOption.value = {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: ['06/23', '06/24', '06/25', '06/26', '06/27', '06/28', '06/29'], boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ name: '操作次数', type: 'line', smooth: true, data: [42, 55, 38, 63, 48, 57, 53], lineStyle: { color: '#409eff', width: 2 }, itemStyle: { color: '#409eff' } }]
      }
    }
  }

  // Operation type distribution (existing)
  try {
    const pieRes = await getOperationTypeDistribution()
    const pieData = pieRes?.data || pieRes || []
    pieOption.value = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center' },
      series: [{
        name: '操作类型', type: 'pie', radius: ['45%', '75%'], center: ['40%', '50%'],
        data: pieData.length ? pieData.map((d: any) => ({ name: d.type || d.name, value: d.count || d.value })) : [
          { name: '查询', value: 120 }, { name: '新增', value: 45 }, { name: '修改', value: 38 }, { name: '删除', value: 15 }, { name: '导出', value: 22 }, { name: '登录', value: 98 }
        ]
      }]
    }
  } catch { /* No fallback — chart stays empty if API fails */ }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.dashboard {
  .stats-row {
    margin-bottom: 16px;
  }

  .charts-row {
    margin-bottom: 16px;

    .el-col {
      margin-bottom: 16px;
    }
  }
}

.stat-card {
  :deep(.el-card__body) {
    padding: 20px;
  }
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .stat-info {
    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: $text-primary;
      line-height: 1.2;

      .unit {
        font-size: 16px;
        font-weight: 500;
        margin-left: 2px;
        color: $text-secondary;
      }

      &.success { color: #67c23a; }
      &.warning { color: #e6a23c; }
      &.primary { color: #409eff; font-size: 24px; }
    }

    .stat-label {
      font-size: 14px;
      color: $text-secondary;
      margin-top: 4px;
    }
  }

  .stat-icon {
    opacity: 0.7;
  }
}

.business-stat {
  border-left: 3px solid transparent;

  &:nth-child(1) { border-left-color: #67c23a; }
  &:nth-child(2) { border-left-color: #e6a23c; }
  &:nth-child(3) { border-left-color: #409eff; }
  &:nth-child(4) { border-left-color: #909399; }
}
</style>

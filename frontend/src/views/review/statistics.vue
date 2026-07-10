<template>
  <div class="page-container review-statistics">
    <div class="page-header">
      <h2>审核统计</h2>
    </div>

    <!-- Filter bar -->
    <div class="search-form">
      <el-form :model="filters" :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
          />
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="filters.documentType" placeholder="全部" clearable style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核人">
          <el-select v-model="filters.reviewerId" placeholder="全部" clearable filterable style="width: 160px;">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadStatistics">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Summary cards -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ summary.totalCount }}</div>
            <div class="stat-label">总审核任务</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('pending') }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('reviewing') }}</div>
            <div class="stat-label">审核中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('approved') }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('rejected') }}</div>
            <div class="stat-label">已驳回</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ summary.overallApprovalRate }}%</div>
            <div class="stat-label">通过率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts row -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>审核状态分布</span>
          </template>
          <v-chart :option="statusChartOption" autoresize style="height: 320px;" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>文档类型分布</span>
          </template>
          <v-chart :option="docTypeChartOption" autoresize style="height: 320px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Daily trend chart -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>每日审核趋势 (近30天)</span>
          </template>
          <v-chart :option="dailyTrendOption" autoresize style="height: 320px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Reviewer performance table -->
    <el-row :gutter="16" class="charts-row" v-if="reviewerStats.length > 0">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>审核人绩效</span>
          </template>
          <el-table :data="reviewerStats" stripe border>
            <el-table-column prop="reviewer" label="审核人" min-width="120" />
            <el-table-column prop="totalReviews" label="审核任务数" width="120" align="center" />
            <el-table-column prop="approvedCount" label="已通过" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small">{{ row.approvedCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rejectedCount" label="已驳回" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.rejectedCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="partialCount" label="部分通过" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="warning" size="small">{{ row.partialCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="通过率" width="100" align="center">
              <template #default="{ row }">
                {{ row.totalReviews > 0 ? Math.round((row.approvedCount / row.totalReviews) * 100) : 0 }}%
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getReviewStatistics } from '@/api/review'
import { getUsers } from '@/api/user'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const dateRange = ref<[string, string] | null>(null)
const filters = reactive({
  documentType: '',
  reviewerId: null as number | null
})

const userList = ref<any[]>([])

const summary = reactive({
  totalCount: 0,
  overallApprovalRate: 0
})

interface StatusDistribution {
  pending: number
  reviewing: number
  approved: number
  rejected: number
  partial: number
}

const statusDistribution = reactive<StatusDistribution>({
  pending: 0,
  reviewing: 0,
  approved: 0,
  rejected: 0,
  partial: 0
})

const docTypeDistribution = ref<Record<string, number>>({})
const dailyTrend = ref<any[]>([])
const reviewerStats = ref<any[]>([])

function getStatusCount(status: string): number {
  return (statusDistribution as any)[status] || 0
}

async function loadUsers() {
  try {
    const res = await getUsers({ page: 1, pageSize: 200 })
    const d = (res as any)?.data || res
    userList.value = (d.records || d || []).filter((u: any) => u.status !== 0)
  } catch { /* ignore */ }
}

async function loadStatistics() {
  try {
    const params: any = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (filters.documentType) params.documentType = filters.documentType
    if (filters.reviewerId) params.reviewerId = filters.reviewerId

    const res = await getReviewStatistics(params)
    const data = (res as any)?.data || res

    summary.totalCount = data.totalCount || 0
    summary.overallApprovalRate = data.overallApprovalRate || 0
    Object.assign(statusDistribution, data.statusDistribution || {})
    docTypeDistribution.value = data.docTypeDistribution || {}
    dailyTrend.value = data.dailyTrend || []
    reviewerStats.value = data.reviewerStats || []
  } catch {
    // Error handled by interceptor
  }
}

function handleReset() {
  dateRange.value = null
  filters.documentType = ''
  filters.reviewerId = null
  loadStatistics()
}

// ==================== Chart Options ====================

const statusChartOption = computed(() => ({
  tooltip: {
    trigger: 'item' as const,
    formatter: '{b}: {c} ({d}%)'
  },
  legend: {
    bottom: 0
  },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    center: ['50%', '45%'],
    avoidLabelOverlap: false,
    itemStyle: {
      borderRadius: 6,
      borderColor: '#fff',
      borderWidth: 2
    },
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' }
    },
    data: [
      { value: statusDistribution.pending, name: '待审核', itemStyle: { color: '#909399' } },
      { value: statusDistribution.reviewing, name: '审核中', itemStyle: { color: '#409eff' } },
      { value: statusDistribution.approved, name: '已通过', itemStyle: { color: '#67c23a' } },
      { value: statusDistribution.rejected, name: '已驳回', itemStyle: { color: '#f56c6c' } },
      { value: statusDistribution.partial, name: '部分通过', itemStyle: { color: '#e6a23c' } }
    ]
  }]
}))

const docTypeChartOption = computed(() => {
  const data = Object.entries(docTypeDistribution.value).map(([name, value]) => ({
    name: docTypeLabel(name),
    value
  }))
  return {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: 0
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: data.length > 0 ? data : [{ value: 0, name: '暂无数据', itemStyle: { color: '#e4e7ed' } }]
    }]
  }
})

const dailyTrendOption = computed(() => ({
  tooltip: {
    trigger: 'axis' as const
  },
  legend: {
    data: ['新增任务', '已完成']
  },
  grid: {
    left: 40,
    right: 20,
    bottom: 30,
    top: 20
  },
  xAxis: {
    type: 'category' as const,
    data: dailyTrend.value.map((d: any) => d.date.substring(5)),
    axisLabel: { rotate: 45 }
  },
  yAxis: {
    type: 'value' as const,
    minInterval: 1
  },
  series: [
    {
      name: '新增任务',
      type: 'line',
      data: dailyTrend.value.map((d: any) => d.created),
      smooth: true,
      itemStyle: { color: '#409eff' },
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' }
    },
    {
      name: '已完成',
      type: 'line',
      data: dailyTrend.value.map((d: any) => d.completed),
      smooth: true,
      itemStyle: { color: '#67c23a' },
      areaStyle: { color: 'rgba(103, 194, 58, 0.1)' }
    }
  ]
}))

function docTypeLabel(type: string) {
  const map: Record<string, string> = {
    contract: '合同',
    invoice: '发票',
    bill_of_lading: '提单'
  }
  return map[type] || type
}

onMounted(() => {
  loadUsers()
  loadStatistics()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.review-statistics {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
    }
  }

  .summary-row {
    margin-bottom: 16px;
  }

  .stat-card {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  .stat-content {
    text-align: center;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: $text-primary;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 4px;
    }
  }

  .charts-row {
    margin-bottom: 16px;
  }

  .chart-card {
    :deep(.el-card__header) {
      font-weight: 600;
      font-size: 15px;
      color: $text-primary;
      padding: 12px 16px;
    }
  }
}
</style>
<template>
  <div class="extraction-table">
    <!-- Filters -->
    <div class="extraction-filters">
      <div class="filter-group">
        <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="small" style="width: 140px;">
          <el-option label="已验证" value="verified" />
          <el-option label="不确定" value="uncertain" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-select v-model="filterConfidence" placeholder="置信度等级" clearable size="small" style="width: 140px;">
          <el-option label="高 (>= 0.9)" value="high" />
          <el-option label="中 (0.7 - 0.9)" value="medium" />
          <el-option label="低 (< 0.7)" value="low" />
        </el-select>
        <el-button size="small" :icon="Download" @click="exportJson">
          导出 JSON
        </el-button>
      </div>
      <div class="filter-summary">
        <span>共 {{ filteredFields.length }} / {{ fields.length }} 条结果</span>
      </div>
    </div>

    <!-- Fields Table -->
    <el-table
      :data="filteredFields"
      stripe
      border
      empty-text="暂无提取结果"
    >
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="fieldName" label="字段名" width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="field-name">{{ row.fieldName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="extractedValue" label="提取值" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="field-value">{{ row.extractedValue }}</span>
        </template>
      </el-table-column>
      <el-table-column label="置信度" width="180" align="center">
        <template #default="{ row }">
          <div class="confidence-cell">
            <el-progress
              :percentage="Math.round(row.confidence * 100)"
              :color="confidenceColor(row.confidence)"
              :stroke-width="8"
              style="width: 100px;"
            />
            <span class="confidence-percent" :style="{ color: confidenceColor(row.confidence) }">
              {{ (row.confidence * 100).toFixed(1) }}%
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="pageRef" label="页码" width="70" align="center">
        <template #default="{ row }">
          <span v-if="row.pageRef">P{{ row.pageRef }}</span>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { ExtractionField } from '@/api/pipeline'

const props = defineProps<{
  fields: ExtractionField[]
  pipelineId: number
}>()

const filterStatus = ref('')
const filterConfidence = ref('')

const filteredFields = computed(() => {
  let result = props.fields

  if (filterStatus.value) {
    result = result.filter(f => f.status === filterStatus.value)
  }

  if (filterConfidence.value) {
    result = result.filter(f => {
      switch (filterConfidence.value) {
        case 'high': return f.confidence >= 0.9
        case 'medium': return f.confidence >= 0.7 && f.confidence < 0.9
        case 'low': return f.confidence < 0.7
        default: return true
      }
    })
  }

  return result
})

function confidenceColor(confidence: number): string {
  if (confidence >= 0.9) return '#67c23a'
  if (confidence >= 0.7) return '#e6a23c'
  return '#f56c6c'
}

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    verified: 'success',
    uncertain: 'warning',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    verified: '已验证',
    uncertain: '不确定',
    rejected: '已拒绝'
  }
  return map[status] || status
}

function exportJson() {
  const data = filteredFields.value.map(f => ({
    fieldName: f.fieldName,
    extractedValue: f.extractedValue,
    confidence: f.confidence,
    status: f.status,
    pageRef: f.pageRef
  }))
  console.log('Extraction export:', JSON.stringify(data, null, 2))
  ElMessage.success(`已导出 ${data.length} 条提取结果到控制台`)
}
</script>

<style scoped lang="scss">
.extraction-table {
  .extraction-filters {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .filter-group {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .filter-summary {
    font-size: 13px;
    color: #909399;
  }
}

.field-name {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}

.field-value {
  font-size: 13px;
  color: #606266;
}

.confidence-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.confidence-percent {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  min-width: 40px;
}

.no-data {
  color: #c0c4cc;
}
</style>

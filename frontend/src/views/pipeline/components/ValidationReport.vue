<template>
  <div class="validation-report">
    <!-- Summary Cards -->
    <div class="summary-cards">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="summary-card">
            <div class="summary-value">{{ items.length }}</div>
            <div class="summary-label">总问题数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-card summary-error">
            <div class="summary-value">{{ errorCount }}</div>
            <div class="summary-label">错误</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-card summary-warning">
            <div class="summary-value">{{ warningCount }}</div>
            <div class="summary-label">警告</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-card summary-info">
            <div class="summary-value">{{ infoCount }}</div>
            <div class="summary-label">信息</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- Filter -->
    <div class="report-filter">
      <el-select v-model="filterSeverity" placeholder="筛选严重级别" clearable size="small" style="width: 160px;">
        <el-option label="错误" value="error" />
        <el-option label="警告" value="warning" />
        <el-option label="信息" value="info" />
      </el-select>
      <span class="filter-count">显示 {{ filteredItems.length }} / {{ items.length }} 条</span>
    </div>

    <!-- Validation Items -->
    <div class="report-list">
      <el-timeline>
        <el-timeline-item
          v-for="item in filteredItems"
          :key="item.id"
          :color="severityColor(item.severity)"
          :icon="severityIcon(item.severity)"
          :type="severityTagType(item.severity) as any"
          :hollow="item.severity === 'info'"
          size="large"
        >
          <div class="report-item" @click="toggleExpand(item.id)">
            <div class="report-item-header">
              <span class="report-severity-tag" :class="`severity-${item.severity}`">
                {{ severityLabel(item.severity) }}
              </span>
              <span class="report-rule">{{ item.rule }}</span>
              <span class="report-field">
                <el-icon :size="14"><Operation /></el-icon>
                {{ item.field }}
              </span>
              <el-icon class="expand-icon" :class="{ expanded: expandedIds.has(item.id) }">
                <ArrowDown v-if="!expandedIds.has(item.id)" />
                <ArrowUp v-else />
              </el-icon>
            </div>
            <div class="report-item-message">
              <el-icon :size="14"><InfoFilled /></el-icon>
              <span>{{ item.message }}</span>
            </div>

            <!-- Expandable Detail -->
            <transition name="report-expand">
              <div v-if="expandedIds.has(item.id)" class="report-item-detail">
                <div class="detail-section" v-if="item.suggestion">
                  <span class="detail-label">建议：</span>
                  <span class="detail-content">{{ item.suggestion }}</span>
                </div>
                <div class="detail-section">
                  <span class="detail-label">规则：</span>
                  <code class="detail-code">{{ item.rule }}</code>
                </div>
                <div class="detail-section">
                  <span class="detail-label">字段：</span>
                  <span class="detail-content">{{ item.field }}</span>
                </div>
              </div>
            </transition>
          </div>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-if="filteredItems.length === 0" description="无匹配的校验项" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  CircleCloseFilled, WarningFilled, InfoFilled,
  Operation, ArrowDown, ArrowUp
} from '@element-plus/icons-vue'
import type { ValidationItem } from '@/api/pipeline'

const props = defineProps<{
  items: ValidationItem[]
  pipelineId: number
}>()

const filterSeverity = ref('')
const expandedIds = ref<Set<number>>(new Set())

const errorCount = computed(() => props.items.filter(i => i.severity === 'error').length)
const warningCount = computed(() => props.items.filter(i => i.severity === 'warning').length)
const infoCount = computed(() => props.items.filter(i => i.severity === 'info').length)

const filteredItems = computed(() => {
  if (!filterSeverity.value) return props.items
  return props.items.filter(i => i.severity === filterSeverity.value)
})

function severityColor(severity: string): string {
  const map: Record<string, string> = {
    error: '#f56c6c',
    warning: '#e6a23c',
    info: '#409eff'
  }
  return map[severity] || '#909399'
}

function severityTagType(severity: string): string {
  const map: Record<string, string> = {
    error: 'danger',
    warning: 'warning',
    info: 'primary'
  }
  return map[severity] || 'info'
}

function severityLabel(severity: string): string {
  const map: Record<string, string> = {
    error: '错误',
    warning: '警告',
    info: '信息'
  }
  return map[severity] || severity
}

function severityIcon(severity: string) {
  if (severity === 'error') return CircleCloseFilled
  if (severity === 'warning') return WarningFilled
  return InfoFilled
}

function toggleExpand(id: number) {
  const newSet = new Set(expandedIds.value)
  if (newSet.has(id)) {
    newSet.delete(id)
  } else {
    newSet.add(id)
  }
  expandedIds.value = newSet
}
</script>

<style scoped lang="scss">
.validation-report {
  .summary-cards {
    margin-bottom: 20px;
  }

  .summary-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    padding: 16px;
    text-align: center;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }

    .summary-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
    }

    .summary-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }

    &.summary-error .summary-value { color: #f56c6c; }
    &.summary-warning .summary-value { color: #e6a23c; }
    &.summary-info .summary-value { color: #409eff; }
  }

  .report-filter {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;

    .filter-count {
      font-size: 13px;
      color: #909399;
    }
  }

  .report-list {
    :deep(.el-timeline-item__node) {
      font-size: 16px;
    }

    :deep(.el-timeline-item__wrapper) {
      padding-left: 28px;
    }

    :deep(.el-timeline-item__tail) {
      border-inline-start-width: 2px;
    }
  }

  .report-item {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    padding: 14px 16px;
    cursor: pointer;
    transition: box-shadow 0.2s, border-color 0.2s;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      border-color: #c0c4cc;
    }
  }

  .report-item-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  .report-severity-tag {
    padding: 2px 10px;
    border-radius: 3px;
    font-size: 11px;
    font-weight: 600;
    color: #fff;

    &.severity-error { background: #f56c6c; }
    &.severity-warning { background: #e6a23c; }
    &.severity-info { background: #409eff; }
  }

  .report-rule {
    font-size: 14px;
    color: #303133;
    font-weight: 600;
  }

  .report-field {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
    margin-left: auto;
  }

  .expand-icon {
    font-size: 14px;
    color: #909399;
    transition: transform 0.2s;
    flex-shrink: 0;

    &.expanded {
      color: #409eff;
    }
  }

  .report-item-message {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    font-size: 13px;
    color: #606266;
    line-height: 1.6;
  }

  .report-item-detail {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .detail-section {
    display: flex;
    gap: 8px;
    font-size: 13px;

    .detail-label {
      color: #909399;
      flex-shrink: 0;
      min-width: 40px;
    }

    .detail-content {
      color: #606266;
    }

    .detail-code {
      font-family: 'Courier New', Consolas, monospace;
      font-size: 12px;
      background: #f5f7fa;
      padding: 2px 8px;
      border-radius: 3px;
      color: #409eff;
    }
  }

  .report-expand-enter-active,
  .report-expand-leave-active {
    transition: all 0.25s ease;
    overflow: hidden;
  }

  .report-expand-enter-from,
  .report-expand-leave-to {
    opacity: 0;
    max-height: 0;
    padding: 0;
    margin: 0;
  }

  .report-expand-enter-to,
  .report-expand-leave-from {
    opacity: 1;
    max-height: 200px;
  }
}
</style>

<template>
  <div class="reflection-view">
    <div class="reflection-header">
      <div class="header-info">
        <el-icon :size="18" color="#409eff"><RefreshRight /></el-icon>
        <span class="header-title">LLM 反思修正记录</span>
        <el-tag size="small" type="info">{{ items.length }} 条修正</el-tag>
      </div>
      <div class="header-desc">
        记录 LLM 在反思阶段对低置信度字段进行的二次确认与修正
      </div>
    </div>

    <div v-if="items.length === 0" class="reflection-empty">
      <el-empty description="该流水线未触发反思修正" :image-size="80">
        <template #image>
          <el-icon :size="80" color="#c0c4cc"><Checked /></el-icon>
        </template>
      </el-empty>
    </div>

    <div v-else class="reflection-list">
      <div
        v-for="item in items"
        :key="item.id"
        class="reflection-card"
      >
        <!-- Card Header -->
        <div class="card-header">
          <div class="card-field-info">
            <el-icon :size="16" color="#e6a23c"><WarningFilled /></el-icon>
            <span class="field-name">{{ item.fieldName }}</span>
            <el-tag size="small" type="warning" effect="plain">已修正</el-tag>
          </div>
          <div class="card-time">
            <el-icon :size="13"><Clock /></el-icon>
            <span>{{ item.timestamp }}</span>
          </div>
        </div>

        <!-- Before / After Comparison -->
        <div class="card-comparison">
          <!-- Before -->
          <div class="compare-box compare-before">
            <div class="compare-label">修正前</div>
            <div class="compare-value">{{ item.beforeValue }}</div>
          </div>

          <!-- Arrow -->
          <div class="compare-arrow">
            <el-icon :size="24" color="#409eff"><DArrowRight /></el-icon>
          </div>

          <!-- After -->
          <div class="compare-box compare-after">
            <div class="compare-label">修正后</div>
            <div class="compare-value">{{ item.afterValue }}</div>
          </div>
        </div>

        <!-- Reason -->
        <div class="card-reason">
          <div class="reason-label">
            <el-icon :size="14"><InfoFilled /></el-icon>
            修正原因
          </div>
          <div class="reason-content">{{ item.reason }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  RefreshRight, WarningFilled, Clock, DArrowRight,
  InfoFilled, Checked
} from '@element-plus/icons-vue'
import type { ReflectionItem } from '@/api/pipeline'

defineProps<{
  items: ReflectionItem[]
  pipelineId: number
}>()
</script>

<style scoped lang="scss">
.reflection-view {
  .reflection-header {
    margin-bottom: 20px;

    .header-info {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .header-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .header-desc {
      font-size: 13px;
      color: #909399;
      padding-left: 28px;
    }
  }

  .reflection-empty {
    padding: 40px 0;
  }

  .reflection-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .reflection-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 18px 20px;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    }
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .card-field-info {
    display: flex;
    align-items: center;
    gap: 10px;

    .field-name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }
  }

  .card-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #c0c4cc;
  }
}

.card-comparison {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.compare-box {
  flex: 1;
  border-radius: 6px;
  padding: 12px 14px;
  min-width: 0;

  .compare-label {
    font-size: 11px;
    font-weight: 600;
    margin-bottom: 6px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .compare-value {
    font-size: 14px;
    word-break: break-all;
    line-height: 1.6;
  }

  &.compare-before {
    background: #fef0f0;
    border: 1px solid #fde2e2;

    .compare-label { color: #f56c6c; }
    .compare-value { color: #f56c6c; text-decoration: line-through; }
  }

  &.compare-after {
    background: #f0f9eb;
    border: 1px solid #e1f3d8;

    .compare-label { color: #67c23a; }
    .compare-value { color: #67c23a; }
  }
}

.compare-arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-reason {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px 14px;

  .reason-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #909399;
    font-weight: 500;
    margin-bottom: 6px;
  }

  .reason-content {
    font-size: 13px;
    color: #606266;
    line-height: 1.6;
  }
}
</style>

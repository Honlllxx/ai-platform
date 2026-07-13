<template>
  <div class="field-compare">
    <div class="field-compare__header">
      <div class="field-compare__hint">
        <el-icon><Key /></el-icon>
        <span>快捷键: Enter=确认 / Esc=跳过</span>
      </div>
      <span class="field-compare__progress">
        已审核 {{ reviewedCount }} / {{ fields.length }} 项
      </span>
    </div>

    <div class="field-compare__list" ref="listRef" @keydown="handleKeyboard">
      <div
        v-for="(field, index) in fields"
        :key="field.id"
        class="field-row"
        :class="{
          'field-row--approved': field.status === 'approved',
          'field-row--rejected': field.status === 'rejected',
          'field-row--corrected': field.status === 'corrected',
          'field-row--active': activeIndex === index
        }"
        tabindex="0"
        @click="activeIndex = index"
        @focus="activeIndex = index"
      >
        <!-- Field name -->
        <div class="field-row__header">
          <span class="field-row__name">{{ field.fieldName }}</span>
          <el-tag
            v-if="field.status !== 'pending'"
            size="small"
            :type="statusTagType(field.status)"
          >
            {{ statusLabel(field.status) }}
          </el-tag>
        </div>

        <!-- Extracted value with confidence -->
        <div class="field-row__value">
          <div class="field-row__extracted">
            <span class="field-row__label">提取值:</span>
            <span
              class="field-row__text"
              :class="confidenceClass(field.confidence)"
            >
              {{ field.extractedValue || '(空)' }}
            </span>
          </div>
          <div class="field-row__confidence">
            <el-progress
              :percentage="Math.round(field.confidence * 100)"
              :stroke-width="6"
              :color="confidenceColor(field.confidence)"
              :show-text="false"
              style="width: 80px;"
            />
            <span class="confidence-text">{{ Math.round(field.confidence * 100) }}%</span>
          </div>
        </div>

        <!-- Original image region placeholder -->
        <div class="field-row__region" v-if="field.originalImageRegion">
          <span class="field-row__label">原文区域:</span>
          <span class="region-coords">
            ({{ field.originalImageRegion.x }}, {{ field.originalImageRegion.y }})
            {{ field.originalImageRegion.width }} x {{ field.originalImageRegion.height }}
          </span>
        </div>

        <!-- Corrected value display -->
        <div v-if="field.status === 'corrected' && field.correctedValue" class="field-row__corrected">
          <span class="field-row__label">修正值:</span>
          <span class="field-row__text field-row__text--corrected">{{ field.correctedValue }}</span>
        </div>

        <!-- Correction inline input -->
        <div v-if="correctingFieldId === field.id" class="field-row__edit">
          <el-input
            v-model="editValue"
            placeholder="输入修正值"
            size="small"
            @keyup.enter="saveCorrection(field.id)"
            @keyup.escape="cancelCorrection"
          />
          <el-button size="small" type="primary" @click="saveCorrection(field.id)">保存</el-button>
          <el-button size="small" @click="cancelCorrection">取消</el-button>
        </div>

        <!-- Reject comment input -->
        <div v-if="rejectingFieldId === field.id" class="field-row__reject">
          <el-input
            v-model="rejectComment"
            placeholder="驳回原因（可选）"
            size="small"
            @keyup.enter="saveReject(field.id)"
          />
          <el-button size="small" type="danger" @click="saveReject(field.id)">确认驳回</el-button>
          <el-button size="small" @click="cancelReject">取消</el-button>
        </div>

        <!-- Comment display for rejected fields -->
        <div v-if="field.status === 'rejected' && field.comment" class="field-row__comment">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ field.comment }}</span>
        </div>

        <!-- Action buttons (only for pending) -->
        <div class="field-row__actions" v-if="canAct(field.status)">
          <el-button type="success" size="small" :icon="Check" @click.stop="handleApprove(field.id)">
            确认
          </el-button>
          <el-button type="danger" size="small" :icon="Close" @click.stop="startReject(field.id)">
            驳回
          </el-button>
          <el-button type="warning" size="small" :icon="Edit" @click.stop="startCorrect(field.id)">
            修正
          </el-button>
        </div>
      </div>

      <el-empty v-if="fields.length === 0" description="暂无字段数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Check, Close, Edit, Key, WarningFilled } from '@element-plus/icons-vue'
import type { ReviewField } from '@/api/review'

const props = defineProps<{
  fields: ReviewField[]
  documentUrl?: string
}>()

const emit = defineEmits<{
  approve: [fieldId: number]
  reject: [fieldId: number, comment?: string]
  correct: [fieldId: number, value: string]
}>()

const activeIndex = ref(0)
const correctingFieldId = ref<number | null>(null)
const editValue = ref('')
const rejectingFieldId = ref<number | null>(null)
const rejectComment = ref('')
const listRef = ref<HTMLElement>()

const reviewedCount = computed(() =>
  props.fields.filter(f => f.status !== 'pending').length
)

function canAct(status: string) {
  return status === 'pending'
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    approved: 'success',
    rejected: 'danger',
    corrected: 'warning'
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    approved: '已确认',
    rejected: '已驳回',
    corrected: '已修正'
  }
  return map[status] || status
}

function confidenceClass(confidence: number) {
  if (confidence >= 0.8) return 'confidence--high'
  if (confidence >= 0.5) return 'confidence--medium'
  return 'confidence--low'
}

function confidenceColor(confidence: number) {
  if (confidence >= 0.8) return '#67c23a'
  if (confidence >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

function handleApprove(fieldId: number) {
  emit('approve', fieldId)
}

function startReject(fieldId: number) {
  rejectingFieldId.value = fieldId
  rejectComment.value = ''
  correctingFieldId.value = null
}

function saveReject(fieldId: number) {
  emit('reject', fieldId, rejectComment.value || undefined)
  rejectingFieldId.value = null
  rejectComment.value = ''
}

function cancelReject() {
  rejectingFieldId.value = null
  rejectComment.value = ''
}

function startCorrect(fieldId: number) {
  correctingFieldId.value = fieldId
  const field = props.fields.find(f => f.id === fieldId)
  editValue.value = field?.extractedValue || ''
  rejectingFieldId.value = null
}

function saveCorrection(fieldId: number) {
  if (editValue.value.trim()) {
    emit('correct', fieldId, editValue.value.trim())
  }
  correctingFieldId.value = null
  editValue.value = ''
}

function cancelCorrection() {
  correctingFieldId.value = null
  editValue.value = ''
}

function handleKeyboard(e: KeyboardEvent) {
  // Don't handle keyboard shortcuts when editing input fields
  const target = e.target as HTMLElement
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
    return
  }

  if (e.key === 'Enter') {
    const currentField = props.fields[activeIndex.value]
    if (currentField && canAct(currentField.status)) {
      handleApprove(currentField.id)
    }
  } else if (e.key === 'Escape') {
    cancelCorrection()
    cancelReject()
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = Math.min(activeIndex.value + 1, props.fields.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeyboard)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboard)
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.field-compare {
  height: 100%;
  display: flex;
  flex-direction: column;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;
  }

  &__hint {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $text-secondary;
  }

  &__progress {
    font-size: 13px;
    color: $text-regular;
    font-weight: 500;
  }

  &__list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
  }
}

.field-row {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;

  &:hover {
    border-color: $primary-color;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
  }

  &--active {
    border-color: $primary-color;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  }

  &--approved {
    background: #f0f9eb;
    border-color: #c2e7b0;
  }

  &--rejected {
    background: #fef0f0;
    border-color: #fbc4c4;
  }

  &--corrected {
    background: #fdf6ec;
    border-color: #f5dab1;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__name {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  &__value {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
    gap: 12px;
  }

  &__extracted {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__label {
    font-size: 12px;
    color: $text-secondary;
    white-space: nowrap;
  }

  &__text {
    font-size: 14px;
    color: $text-primary;
    padding: 2px 8px;
    border-radius: 4px;

    &.confidence--high {
      background: rgba(103, 194, 58, 0.12);
    }

    &.confidence--medium {
      background: rgba(230, 162, 60, 0.12);
    }

    &.confidence--low {
      background: rgba(245, 108, 108, 0.12);
    }

    &--corrected {
      color: $warning-color;
      font-weight: 500;
    }
  }

  &__confidence {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;

    .confidence-text {
      font-size: 12px;
      color: $text-secondary;
      min-width: 36px;
    }
  }

  &__region {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
    font-size: 12px;
    color: $text-secondary;

    .region-coords {
      color: $info-color;
      font-family: monospace;
    }
  }

  &__corrected,
  &__comment {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px;
    font-size: 13px;
  }

  &__comment {
    color: $danger-color;
  }

  &__edit,
  &__reject {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed #e4e7ed;
  }
}

</style>

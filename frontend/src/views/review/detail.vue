<template>
  <div class="page-container review-detail">
    <!-- Header bar -->
    <div class="review-header">
      <div class="review-header__left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2 class="review-title">{{ review.documentName }}</h2>
        <el-tag size="small" :type="statusTagType(review.status)">
          {{ statusLabel(review.status) }}
        </el-tag>
      </div>
      <div class="review-header__right">
        <span class="review-progress">
          进度: {{ review.reviewedFields }} / {{ review.totalFields }}
        </span>
        <el-progress
          :percentage="fieldProgress"
          :stroke-width="8"
          :color="progressColor"
          style="width: 150px;"
        />
      </div>
    </div>

    <!-- Split layout -->
    <div class="review-body">
      <!-- Left: Document preview -->
      <div class="review-preview-panel">
        <div class="preview-header">
          <span>文档预览</span>
        </div>
        <div class="preview-content">
          <!-- Document placeholder with field positions -->
          <div class="document-preview">
            <div class="document-preview__placeholder">
              <el-icon :size="64" color="#c0c4cc"><PictureFilled /></el-icon>
              <p>文档图片预览</p>
              <p class="preview-hint">{{ review.documentName }}</p>
            </div>

            <!-- Field position overlays -->
            <div
              v-for="field in reviewFields"
              :key="field.id"
              v-show="field.originalImageRegion"
              class="field-overlay"
              :class="fieldOverlayClass(field)"
              :style="fieldOverlayStyle(field)"
              :title="field.fieldName"
            >
              <span class="field-overlay__label">{{ field.fieldName }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Field review list -->
      <div class="review-work-panel">
        <FieldCompare
          :fields="reviewFields"
          @approve="handleApprove"
          @reject="handleReject"
          @correct="handleCorrect"
        />
      </div>
    </div>

    <!-- Bottom action bar -->
    <div class="review-actions">
      <div class="review-actions__left">
        <span class="remaining-count">
          剩余 {{ pendingCount }} / {{ reviewFields.length }} 项待审核
        </span>
      </div>
      <div class="review-actions__right">
        <el-button
          type="success"
          :icon="Select"
          :disabled="pendingCount === 0"
          @click="handleBatchApprove"
        >
          一键通过
        </el-button>
        <el-button
          type="danger"
          :icon="CloseBold"
          :disabled="pendingCount === 0"
          @click="handleBatchReject"
        >
          全部驳回
        </el-button>
      </div>
    </div>

    <!-- Review history -->
    <div class="review-history" v-if="reviewHistory.length > 0">
      <h3 class="history-title">审核记录</h3>
      <el-timeline>
        <el-timeline-item
          v-for="item in reviewHistory"
          :key="item.id"
          :timestamp="item.timestamp"
          :type="historyIconType(item.action)"
          placement="top"
        >
          <div class="history-item">
            <span class="history-item__field">{{ item.fieldName }}</span>
            <el-tag size="small" :type="historyActionTag(item.action)">
              {{ historyActionLabel(item.action) }}
            </el-tag>
            <span v-if="item.operator" class="history-item__operator">{{ item.operator }}</span>
            <span v-if="item.comment" class="history-item__comment">
              备注: {{ item.comment }}
            </span>
            <span v-if="item.newValue" class="history-item__value">
              &rarr; {{ item.newValue }}
            </span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Select, CloseBold, PictureFilled } from '@element-plus/icons-vue'
import {
  getReviewDetail, getReviewFields,
  approveField, rejectField, correctField,
  batchApprove, batchReject, getReviewHistory,
  type ReviewItem, type ReviewField
} from '@/api/review'
import FieldCompare from '@/components/review/FieldCompare.vue'

const route = useRoute()
const router = useRouter()

const reviewId = computed(() => Number(route.params.id))

const review = reactive<ReviewItem>({
  id: 0,
  documentId: 0,
  documentName: '',
  documentType: '',
  status: 'pending',
  totalFields: 0,
  reviewedFields: 0,
  approvedFields: 0,
  rejectedFields: 0,
  reviewer: '',
  reviewTime: '',
  createTime: ''
})

const reviewFields = ref<ReviewField[]>([])
const reviewHistory = ref<any[]>([])

const fieldProgress = computed(() => {
  if (review.totalFields === 0) return 0
  return Math.round((review.reviewedFields / review.totalFields) * 100)
})

const progressColor = computed(() => {
  if (review.status === 'rejected') return '#f56c6c'
  if (review.status === 'approved') return '#67c23a'
  return '#409eff'
})

const pendingCount = computed(() =>
  reviewFields.value.filter(f => f.status === 'pending').length
)

function statusTagType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    approved: 'success',
    rejected: 'danger',
    partial: 'warning'
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    partial: '部分通过'
  }
  return map[status] || status
}

function fieldOverlayClass(field: ReviewField) {
  return {
    'field-overlay--pending': field.status === 'pending',
    'field-overlay--approved': field.status === 'approved',
    'field-overlay--rejected': field.status === 'rejected',
    'field-overlay--corrected': field.status === 'corrected',
  }
}

function fieldOverlayStyle(field: ReviewField) {
  if (!field.originalImageRegion) return {}
  return {
    left: field.originalImageRegion.x + 'px',
    top: field.originalImageRegion.y + 'px',
    width: field.originalImageRegion.width + 'px',
    height: field.originalImageRegion.height + 'px'
  }
}

// Field review actions
async function handleApprove(fieldId: number) {
  try {
    await approveField(reviewId.value, fieldId)
    updateLocalFieldStatus(fieldId, 'approved')
    updateReviewCounts()
    ElMessage.success('字段已确认')
  } catch {
    // Error handled by interceptor
  }
}

async function handleReject(fieldId: number, comment?: string) {
  try {
    await rejectField(reviewId.value, fieldId, comment)
    updateLocalFieldStatus(fieldId, 'rejected')
    updateReviewCounts()
    ElMessage.success('字段已驳回')
  } catch {
    // Error handled by interceptor
  }
}

async function handleCorrect(fieldId: number, correctedValue: string) {
  try {
    await correctField(reviewId.value, fieldId, correctedValue)
    const field = reviewFields.value.find(f => f.id === fieldId)
    if (field) {
      field.status = 'corrected'
      field.correctedValue = correctedValue
      review.reviewedFields++
    }
    updateReviewCounts()
    ElMessage.success('字段已修正')
  } catch {
    // Error handled by interceptor
  }
}

function updateLocalFieldStatus(fieldId: number, status: string) {
  const field = reviewFields.value.find(f => f.id === fieldId)
  if (field && field.status === 'pending') {
    field.status = status as ReviewField['status']
    review.reviewedFields++
    if (status === 'approved') review.approvedFields++
    if (status === 'rejected') review.rejectedFields++
  }
}

function updateReviewCounts() {
  const totalApproved = reviewFields.value.filter(f => f.status === 'approved' || f.status === 'corrected').length
  const totalRejected = reviewFields.value.filter(f => f.status === 'rejected').length
  const totalReviewed = reviewFields.value.filter(f => f.status !== 'pending').length

  review.approvedFields = totalApproved
  review.rejectedFields = totalRejected
  review.reviewedFields = totalReviewed

  if (totalReviewed === review.totalFields) {
    if (totalRejected === 0) {
      review.status = 'approved'
    } else if (totalApproved === 0) {
      review.status = 'rejected'
    } else {
      review.status = 'partial'
    }
  } else if (totalReviewed > 0) {
    review.status = 'partial'
  }
}

async function handleBatchApprove() {
  try {
    await ElMessageBox.confirm(
      '确认一键通过所有待审核字段？',
      '批量操作',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    const pendingIds = reviewFields.value
      .filter(f => f.status === 'pending')
      .map(f => f.id)
    if (pendingIds.length === 0) return

    await batchApprove(reviewId.value, pendingIds)
    pendingIds.forEach(id => updateLocalFieldStatus(id, 'approved'))
    updateReviewCounts()
    ElMessage.success(`已通过 ${pendingIds.length} 个字段`)
  } catch {
    // Cancelled or error
  }
}

async function handleBatchReject() {
  try {
    await ElMessageBox.confirm(
      '确认驳回所有待审核字段？',
      '批量操作',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    const pendingIds = reviewFields.value
      .filter(f => f.status === 'pending')
      .map(f => f.id)
    if (pendingIds.length === 0) return

    await batchReject(reviewId.value, pendingIds)
    pendingIds.forEach(id => updateLocalFieldStatus(id, 'rejected'))
    updateReviewCounts()
    ElMessage.success(`已驳回 ${pendingIds.length} 个字段`)
  } catch {
    // Cancelled or error
  }
}

function historyIconType(action: string) {
  const map: Record<string, string> = {
    approve: 'success',
    reject: 'danger',
    correct: 'warning',
    batch_approve: 'success',
    batch_reject: 'danger'
  }
  return map[action] || 'info'
}

function historyActionTag(action: string) {
  const map: Record<string, string> = {
    approve: 'success',
    reject: 'danger',
    correct: 'warning',
    batch_approve: 'success',
    batch_reject: 'danger'
  }
  return map[action] || 'info'
}

function historyActionLabel(action: string) {
  const map: Record<string, string> = {
    approve: '确认',
    reject: '驳回',
    correct: '修正',
    batch_approve: '批量通过',
    batch_reject: '批量驳回'
  }
  return map[action] || action
}

function goBack() {
  router.push('/review')
}

async function loadReview() {
  try {
    const res = await getReviewDetail(reviewId.value)
    const data = res?.data || res
    if (data) Object.assign(review, data)
  } catch {
    // Use mock data
    Object.assign(review, {
      id: reviewId.value,
      documentId: 101,
      documentName: '2026年度采购合同.pdf',
      documentType: 'contract',
      status: 'pending',
      totalFields: 6,
      reviewedFields: 0,
      approvedFields: 0,
      rejectedFields: 0,
      createTime: '2026-06-29 09:15:00'
    })
  }
}

async function loadFields() {
  try {
    const res = await getReviewFields(reviewId.value)
    const data = res?.data || res
    reviewFields.value = data.records || data || []
  } catch {
    // Use mock fields
    reviewFields.value = [
      { id: 1, fieldName: '合同编号', fieldCode: 'contract_no', extractedValue: 'HT-2026-08812', confidence: 0.95, status: 'pending', originalImageRegion: { x: 80, y: 40, width: 200, height: 35 } },
      { id: 2, fieldName: '合同金额', fieldCode: 'contract_amount', extractedValue: '￥580,000.00', confidence: 0.72, status: 'pending', originalImageRegion: { x: 80, y: 300, width: 180, height: 35 } },
      { id: 3, fieldName: '签署日期', fieldCode: 'sign_date', extractedValue: '2026年6月15日', confidence: 0.88, status: 'pending', originalImageRegion: { x: 500, y: 40, width: 150, height: 35 } },
      { id: 4, fieldName: '甲方名称', fieldCode: 'party_a', extractedValue: '上海XX科技有限公司', confidence: 0.91, status: 'pending', originalImageRegion: { x: 80, y: 140, width: 260, height: 35 } },
      { id: 5, fieldName: '乙方名称', fieldCode: 'party_b', extractedValue: '北京YY信息技术有限公司', confidence: 0.43, status: 'pending', originalImageRegion: { x: 80, y: 210, width: 280, height: 35 } },
      { id: 6, fieldName: '是否已签', fieldCode: 'is_signed', extractedValue: '是', confidence: 0.99, status: 'pending', originalImageRegion: { x: 80, y: 420, width: 60, height: 30 } },
    ]
    review.totalFields = reviewFields.value.length
  }
}

async function loadHistory() {
  try {
    const res = await getReviewHistory(reviewId.value)
    const data = res?.data || res
    reviewHistory.value = data.records || data || []
  } catch {
    // No mock history for fresh review
    reviewHistory.value = []
  }
}

onMounted(() => {
  loadReview()
  loadFields()
  loadHistory()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.review-detail {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;

    .review-progress {
      font-size: 13px;
      color: $text-regular;
      font-weight: 500;
    }
  }

  .review-title {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
  }
}

.review-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

// Left: Document preview
.review-preview-panel {
  width: 40%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e4e7ed;
  background: #f5f7fa;

  .preview-header {
    padding: 10px 16px;
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
  }

  .preview-content {
    flex: 1;
    padding: 16px;
    overflow: auto;
  }
}

.document-preview {
  position: relative;
  width: 100%;
  min-height: 500px;
  background: #fff;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;

  &__placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;

    p {
      font-size: 14px;
      color: $text-secondary;
      margin: 0;
    }

    .preview-hint {
      font-size: 12px;
      color: $text-placeholder;
    }
  }
}

.field-overlay {
  position: absolute;
  border: 2px solid;
  border-radius: 3px;
  cursor: pointer;
  transition: opacity 0.2s;

  &--pending {
    border-color: #e6a23c;
    background: rgba(230, 162, 60, 0.1);
  }

  &--approved {
    border-color: #67c23a;
    background: rgba(103, 194, 58, 0.1);
  }

  &--rejected {
    border-color: #f56c6c;
    background: rgba(245, 108, 108, 0.1);
  }

  &--corrected {
    border-color: #409eff;
    background: rgba(64, 158, 255, 0.1);
  }

  &:hover {
    opacity: 0.8;
  }

  &__label {
    position: absolute;
    top: -22px;
    left: 0;
    font-size: 10px;
    background: rgba(0, 0, 0, 0.7);
    color: #fff;
    padding: 1px 6px;
    border-radius: 2px;
    white-space: nowrap;
    pointer-events: none;
    line-height: 18px;
  }
}

// Right: Work panel
.review-work-panel {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

// Bottom action bar
.review-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;

  &__left {
    .remaining-count {
      font-size: 14px;
      color: $text-regular;
      font-weight: 500;
    }
  }

  &__right {
    display: flex;
    gap: 12px;
  }
}

// Review history
.review-history {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
  max-height: 250px;
  overflow-y: auto;

  .history-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 16px;
  }

  .history-item {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    &__field {
      font-size: 13px;
      font-weight: 500;
      color: $text-primary;
    }

    &__operator {
      font-size: 12px;
      color: $text-secondary;
    }

    &__comment {
      font-size: 12px;
      color: $text-secondary;
    }

    &__value {
      font-size: 12px;
      color: $primary-color;
    }
  }
}
</style>

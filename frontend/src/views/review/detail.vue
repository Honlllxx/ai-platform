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
        <!-- Start review button -->
        <el-button
          v-if="review.status === 'pending'"
          type="warning"
          :icon="VideoPlay"
          :loading="startingReview"
          @click="handleStartReview"
        >
          开始审核
        </el-button>
        <!-- Reviewer assignment -->
        <div class="reviewer-section">
          <span class="reviewer-label">审核人:</span>
          <span v-if="review.reviewer" class="reviewer-name">{{ review.reviewer }}</span>
          <el-button
            v-if="!review.reviewer || review.status === 'pending'"
            link
            type="primary"
            size="small"
            :icon="Switch"
            @click="showAssignDialog = true"
            v-permission="'review:update'"
          >
            {{ review.reviewer ? '更换' : '分配' }}
          </el-button>
        </div>
        <span v-if="review.startedAt" class="review-started">
          开始: {{ formatTime(review.startedAt) }}
        </span>
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
          <el-tag v-if="review.documentType" size="small" :type="docTypeTagType(review.documentType)">
            {{ docTypeLabel(review.documentType) }}
          </el-tag>
        </div>
        <div class="preview-content">
          <!-- Document image if available -->
          <div class="document-preview" v-if="imageUrl">
            <img :src="imageUrl" class="document-preview__image" alt="文档预览" />
            <!-- Field position overlays -->
            <div
              v-for="field in reviewFields"
              :key="field.id"
              v-show="fieldOverlayStyle(field)"
              class="field-overlay"
              :class="fieldOverlayClass(field)"
              :style="fieldOverlayStyle(field)"
              :title="field.fieldName"
            >
              <span class="field-overlay__label">{{ field.fieldName }}</span>
            </div>
          </div>
          <!-- Placeholder -->
          <div class="document-preview" v-else>
            <div class="document-preview__placeholder">
              <el-icon :size="64" color="#c0c4cc"><PictureFilled /></el-icon>
              <p>文档图片预览</p>
              <p class="preview-hint">{{ review.documentName }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Field review list -->
      <div class="review-work-panel">
        <FieldCompare
          :fields="reviewFields"
          :document-url="imageUrl"
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
        <el-button
          type="primary"
          :icon="Finished"
          :disabled="review.status === 'approved' || review.status === 'rejected'"
          @click="handleFinalize"
        >
          完成审核
        </el-button>
      </div>
    </div>

    <!-- Review comment -->
    <div class="review-comment" v-if="review.status === 'reviewing' || review.status === 'approved' || review.status === 'rejected' || review.status === 'partial'">
      <div class="comment-header">
        <h3>审核意见</h3>
        <el-button
          v-if="!editingComment"
          link
          type="primary"
          size="small"
          :icon="Edit"
          @click="startEditComment"
        >
          {{ review.reviewComment ? '编辑' : '填写' }}
        </el-button>
      </div>
      <div v-if="editingComment" class="comment-edit">
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="3"
          placeholder="请输入审核意见..."
          maxlength="500"
          show-word-limit
        />
        <div class="comment-actions">
          <el-button size="small" type="primary" :loading="savingComment" @click="handleSaveComment">
            保存
          </el-button>
          <el-button size="small" @click="cancelEditComment">取消</el-button>
        </div>
      </div>
      <div v-else-if="review.reviewComment" class="comment-content">
        <el-icon><ChatLineSquare /></el-icon>
        <span>{{ review.reviewComment }}</span>
      </div>
      <div v-else class="comment-content comment-content--empty">
        暂无审核意见
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
            <span class="history-item__field" v-if="item.fieldName">{{ item.fieldName }}</span>
            <el-tag size="small" :type="historyActionTag(item.action)">
              {{ historyActionLabel(item.action) }}
            </el-tag>
            <span v-if="item.operator" class="history-item__operator">{{ item.operator }}</span>
            <span v-if="item.detailParsed?.comment" class="history-item__comment">
              意见: {{ item.detailParsed.comment }}
            </span>
            <span v-if="item.detailParsed?.oldStatus" class="history-item__change">
              {{ item.detailParsed.oldStatus }} → {{ item.detailParsed.newStatus }}
            </span>
            <span v-if="item.detailParsed?.from && item.detailParsed?.to" class="history-item__change">
              {{ item.detailParsed.from }} → {{ item.detailParsed.to }}
            </span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <!-- Assign Reviewer Dialog -->
    <el-dialog v-model="showAssignDialog" title="分配审核人" width="450px" :close-on-click-modal="false">
      <el-form :model="assignForm" ref="assignFormRef" label-width="80px">
        <el-form-item label="审核任务">
          <span class="assign-doc-name">{{ review.documentName }}</span>
        </el-form-item>
        <el-form-item label="审核人" prop="reviewerId">
          <el-select v-model="assignForm.reviewerId" placeholder="请选择审核人" filterable style="width: 100%;">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="`${user.username} (${user.nickname || user.username})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Select, CloseBold, PictureFilled, Switch, Finished, VideoPlay, Edit, ChatLineSquare } from '@element-plus/icons-vue'
import {
  getReviewDetail, getReviewFields,
  approveField, rejectField, correctField,
  batchApprove, batchReject, getReviewHistory,
  assignReviewer, updateReviewStatus,
  startReview as startReviewApi, saveReviewComment,
  type ReviewItem, type ReviewField
} from '@/api/review'
import { getUsers } from '@/api/user'
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
  reviewerId: undefined,
  reviewTime: '',
  createTime: ''
})

const reviewFields = ref<ReviewField[]>([])
const reviewHistory = ref<any[]>([])
const imageUrl = computed(() => {
  // Try to construct document image URL from documentId
  if (review.documentId) {
    return `/api/documents/${review.documentId}/file`
  }
  return ''
})

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

// ==================== Assignment ====================

const showAssignDialog = ref(false)
const assigning = ref(false)
const assignForm = reactive({ reviewerId: null as number | null })
const userList = ref<any[]>([])

async function loadUsers() {
  try {
    const res = await getUsers({ page: 1, pageSize: 200 })
    const d = (res as any)?.data || res
    userList.value = (d.records || d || []).filter((u: any) => u.status !== 0)
  } catch { /* ignore */ }
}

async function handleAssign() {
  if (!assignForm.reviewerId) return
  assigning.value = true
  try {
    const res = await assignReviewer(reviewId.value, assignForm.reviewerId)
    const d = (res as any)?.data || res
    review.reviewerId = d.reviewerId
    review.reviewer = d.reviewer
    showAssignDialog.value = false
    ElMessage.success('审核人分配成功')
  } catch {
    // Error handled by interceptor
  } finally {
    assigning.value = false
  }
}

// ==================== Finalize ====================

async function handleFinalize() {
  try {
    const remaining = pendingCount.value
    const message = remaining > 0
      ? `还有 ${remaining} 个字段未审核，确认完成审核？未审核的字段将保持待处理状态。`
      : '确认完成审核？'
    await ElMessageBox.confirm(message, '完成审核', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // Determine final status based on field results
    const rejectedCount = reviewFields.value.filter(f => f.status === 'rejected').length
    const finalStatus = rejectedCount === 0 ? 'approved' : 'partial'
    await updateReviewStatus(reviewId.value, finalStatus)
    review.status = finalStatus
    ElMessage.success('审核已完成')
    loadHistory()
  } catch {
    // Cancelled
  }
}

// ==================== Start Review ====================

const startingReview = ref(false)

async function handleStartReview() {
  startingReview.value = true
  try {
    const res = await startReviewApi(reviewId.value)
    const d = (res as any)?.data || res
    review.status = d.status || 'reviewing'
    review.startedAt = d.startedAt
    ElMessage.success('开始审核')
  } catch {
    // Error handled by interceptor
  } finally {
    startingReview.value = false
  }
}

// ==================== Review Comment ====================

const editingComment = ref(false)
const savingComment = ref(false)
const commentText = ref('')

function startEditComment() {
  commentText.value = review.reviewComment || ''
  editingComment.value = true
}

function cancelEditComment() {
  editingComment.value = false
  commentText.value = ''
}

async function handleSaveComment() {
  savingComment.value = true
  try {
    await saveReviewComment(reviewId.value, commentText.value)
    review.reviewComment = commentText.value
    editingComment.value = false
    ElMessage.success('审核意见已保存')
    loadHistory()
  } catch {
    // Error handled by interceptor
  } finally {
    savingComment.value = false
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// ==================== Helpers ====================

function statusTagType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    reviewing: '',
    approved: 'success',
    rejected: 'danger',
    partial: 'warning'
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    reviewing: '审核中',
    approved: '已通过',
    rejected: '已驳回',
    partial: '部分通过'
  }
  return map[status] || status
}

function docTypeTagType(type: string) {
  const map: Record<string, string> = {
    contract: 'success',
    invoice: 'warning',
    bill_of_lading: 'danger'
  }
  return map[type] || ''
}

function docTypeLabel(type: string) {
  const map: Record<string, string> = {
    contract: '合同',
    invoice: '发票',
    bill_of_lading: '提单'
  }
  return map[type] || type
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
  const region = field.originalImageRegion
  if (!region || typeof region === 'string') return null
  return {
    left: region.x + 'px',
    top: region.y + 'px',
    width: region.width + 'px',
    height: region.height + 'px'
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
  }
}

function updateReviewCounts() {
  const totalApproved = reviewFields.value.filter(f => f.status === 'approved' || f.status === 'corrected').length
  const totalRejected = reviewFields.value.filter(f => f.status === 'rejected').length
  const totalReviewed = reviewFields.value.filter(f => f.status !== 'pending').length

  review.approvedFields = totalApproved
  review.rejectedFields = totalRejected
  review.reviewedFields = totalReviewed

  if (totalReviewed >= review.totalFields && review.totalFields > 0) {
    review.status = totalRejected === 0 ? 'approved' : 'partial'
  } else if (totalReviewed > 0) {
    review.status = 'partial'
  } else if (review.status === 'pending') {
    // Keep pending if no fields reviewed yet
  } else {
    review.status = 'reviewing'
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
    approved: 'success',
    rejected: 'danger',
    corrected: 'warning',
    batch_approved: 'success',
    batch_rejected: 'danger',
    batch_corrected: 'warning',
    assign: 'info',
    start_review: 'primary',
    comment: 'info',
    status_change: 'primary'
  }
  return map[action] || 'info'
}

function historyActionTag(action: string) {
  const map: Record<string, string> = {
    approved: 'success',
    rejected: 'danger',
    corrected: 'warning',
    batch_approved: 'success',
    batch_rejected: 'danger',
    batch_corrected: 'warning',
    assign: 'info',
    start_review: 'primary',
    comment: 'info',
    status_change: 'primary'
  }
  return map[action] || 'info'
}

function historyActionLabel(action: string) {
  const map: Record<string, string> = {
    approved: '确认',
    rejected: '驳回',
    corrected: '修正',
    batch_approved: '批量通过',
    batch_rejected: '批量驳回',
    batch_corrected: '批量修正',
    assign: '分配审核人',
    start_review: '开始审核',
    comment: '审核意见',
    status_change: '状态变更'
  }
  return map[action] || action
}

function goBack() {
  router.push('/review/list')
}

async function loadReview() {
  try {
    const res = await getReviewDetail(reviewId.value)
    const data = res?.data || res
    if (data) {
      Object.assign(review, data)
      if (data.fields) {
        reviewFields.value = data.fields.map((f: any) => ({
          ...f,
          originalImageRegion: typeof f.originalImageRegion === 'string'
            ? JSON.parse(f.originalImageRegion)
            : f.originalImageRegion
        }))
      }
    }
  } catch {
    // Use mock fallback
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
    const res = await getReviewHistory(reviewId.value, { page: 1, pageSize: 100 })
    const data = res?.data || res
    reviewHistory.value = data.records || data || []
  } catch {
    reviewHistory.value = []
  }
}

onMounted(() => {
  loadReview()
  loadHistory()
  loadUsers()
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
    gap: 16px;

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

.reviewer-section {
  display: flex;
  align-items: center;
  gap: 6px;

  .reviewer-label {
    font-size: 13px;
    color: $text-secondary;
  }

  .reviewer-name {
    font-size: 13px;
    color: $text-primary;
    font-weight: 500;
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
    display: flex;
    align-items: center;
    justify-content: space-between;
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

  &__image {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
    border-radius: 4px;
  }

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

    &__change {
      font-size: 12px;
      color: $primary-color;
      font-weight: 500;
    }

    &__value {
      font-size: 12px;
      color: $primary-color;
    }
  }
}

.assign-doc-name {
  font-size: 14px;
  color: $text-primary;
  font-weight: 500;
}

.review-started {
  font-size: 12px;
  color: $text-secondary;
}

// Review comment
.review-comment {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;

  .comment-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
    }
  }

  .comment-edit {
    .comment-actions {
      display: flex;
      gap: 8px;
      margin-top: 8px;
    }
  }

  .comment-content {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-size: 14px;
    color: $text-regular;
    padding: 10px 12px;
    background: #f5f7fa;
    border-radius: 6px;
    line-height: 1.6;

    &--empty {
      color: $text-placeholder;
      font-style: italic;
    }
  }
}
</style>
<template>
  <div class="page-container document-upload">
    <!-- Page Header -->
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/documents' }">文档管理</el-breadcrumb-item>
        <el-breadcrumb-item>文档上传</el-breadcrumb-item>
      </el-breadcrumb>
      <h2 class="page-title">文档上传</h2>
    </div>

    <!-- Upload Drop Zone -->
    <div class="upload-card">
      <el-upload
        ref="uploadRef"
        class="upload-drop-zone"
        drag
        multiple
        :auto-upload="false"
        :accept="acceptExtensions"
        :file-list="fileList"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        action="#"
        :http-request="() => {}"
      >
        <div class="drop-zone-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p class="upload-title">将文件拖到此处，或<em>点击上传</em></p>
            <p class="upload-hint">支持 PDF、JPG、PNG、Word 格式，单次最多上传 20 个文件</p>
          </div>
        </div>
      </el-upload>
    </div>

    <!-- Upload Queue -->
    <div v-if="uploadQueue.length > 0" class="queue-card">
      <div class="queue-header">
        <h3>上传队列</h3>
        <div class="queue-summary">
          <span v-if="remainingCount > 0">剩余 {{ remainingCount }} 个文件待上传</span>
          <span v-else-if="failedCount > 0" class="text-danger">上传完成，{{ failedCount }} 个失败</span>
          <span v-else class="text-success">全部上传成功</span>
        </div>
      </div>

      <el-table :data="uploadQueue" stripe border class="queue-table">
        <el-table-column label="文件名" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon :size="18"><Document /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="110" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'uploading'" type="warning" size="small">上传中</el-tag>
            <el-tag v-else-if="row.status === 'success'" type="success" size="small">成功</el-tag>
            <el-tag v-else-if="row.status === 'failed'" type="danger" size="small">失败</el-tag>
            <el-tag v-else type="info" size="small">待上传</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="row.percent"
              :status="row.status === 'failed' ? 'exception' : row.status === 'success' ? 'success' : undefined"
              :stroke-width="16"
              :text-inside="true"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row, $index }">
            <el-button
              v-if="row.status === 'failed'"
              type="primary"
              link
              :icon="RefreshRight"
              @click="retryFile($index)"
            >
              重试
            </el-button>
            <el-button
              v-if="row.status !== 'uploading'"
              type="danger"
              link
              :icon="Delete"
              @click="removeFromQueue($index)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Action Buttons -->
      <div class="queue-actions">
        <el-button
          type="primary"
          :icon="Upload"
          :loading="uploading"
          :disabled="pendingFiles.length === 0"
          @click="startUpload"
        >
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
        <el-button
          v-if="uploading"
          :icon="Close"
          @click="cancelUpload"
        >
          取消上传
        </el-button>
        <el-button
          :icon="Delete"
          :disabled="uploadQueue.length === 0"
          @click="clearQueue"
        >
          清空队列
        </el-button>
      </div>
    </div>

    <!-- Success Summary -->
    <div v-if="showSummary" class="summary-card">
      <el-result icon="success" title="上传完成" :sub-title="summaryText">
        <template #extra>
          <el-button type="primary" @click="goToList">查看文档列表</el-button>
          <el-button @click="resetUpload">继续上传</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled,
  Upload,
  Document,
  RefreshRight,
  Delete,
  Close
} from '@element-plus/icons-vue'
import { uploadDocument } from '@/api/document'
import type { UploadFile, UploadInstance } from 'element-plus'

const router = useRouter()
const uploadRef = ref<UploadInstance>()

const acceptExtensions = '.pdf,.jpg,.jpeg,.png,.doc,.docx'
const MAX_FILES = 20

interface QueueItem {
  name: string
  size: number
  status: 'pending' | 'uploading' | 'success' | 'failed'
  percent: number
  rawFile: File
}

const uploadQueue = shallowRef<QueueItem[]>([])
const uploading = ref(false)
const cancelToken = shallowRef<boolean>(false)
const showSummary = ref(false)
const summaryText = ref('')

// Derived
const fileList = computed(() => {
  return uploadQueue.value.map((item) => ({
    name: item.name,
    size: item.size
  }))
})

const pendingFiles = computed(() =>
  uploadQueue.value.filter((f) => f.status === 'pending')
)

const remainingCount = computed(() => pendingFiles.value.length)

const failedCount = computed(
  () => uploadQueue.value.filter((f) => f.status === 'failed').length
)

const successCount = computed(
  () => uploadQueue.value.filter((f) => f.status === 'success').length
)

// Utility
function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let unitIndex = 0
  let size = bytes
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

function validateFile(file: File): boolean {
  const allowedExts = ['.pdf', '.jpg', '.jpeg', '.png', '.doc', '.docx']
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!allowedExts.includes(ext)) {
    ElMessage.warning(`文件 "${file.name}" 格式不支持，请上传 PDF/JPG/PNG/Word 文件`)
    return false
  }
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.warning(`文件 "${file.name}" 超过 100MB 大小限制`)
    return false
  }
  return true
}

// Event handlers
function handleFileChange(file: UploadFile) {
  const rawFile = file.raw
  if (!rawFile) return

  if (uploadQueue.value.length >= MAX_FILES) {
    ElMessage.warning(`最多只能上传 ${MAX_FILES} 个文件`)
    return
  }

  if (!validateFile(rawFile)) {
    return
  }

  const exists = uploadQueue.value.some((item) => item.name === rawFile.name && item.size === rawFile.size)
  if (exists) {
    ElMessage.warning(`文件 "${rawFile.name}" 已在队列中`)
    return
  }

  uploadQueue.value = [
    ...uploadQueue.value,
    {
      name: rawFile.name,
      size: rawFile.size,
      status: 'pending',
      percent: 0,
      rawFile
    }
  ]
}

function handleFileRemove(file: UploadFile) {
  const idx = uploadQueue.value.findIndex(
    (item) => item.name === file.name && item.size === (file.size || 0)
  )
  if (idx !== -1) {
    removeFromQueue(idx)
  }
}

function removeFromQueue(index: number) {
  const newQueue = [...uploadQueue.value]
  newQueue.splice(index, 1)
  uploadQueue.value = newQueue
  showSummary.value = false
}

function clearQueue() {
  if (uploading.value) {
    cancelToken.value = true
  }
  uploadQueue.value = []
  showSummary.value = false
}

async function uploadSingleFile(item: QueueItem): Promise<void> {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', item.rawFile)

    item.status = 'uploading'
    item.percent = 0
    uploadQueue.value = [...uploadQueue.value]

    // Use real Axios upload progress instead of a simulated timer
    uploadDocument(formData, (progressEvent: ProgressEvent) => {
      if (cancelToken.value) return
      if (progressEvent.lengthComputable) {
        // Cap at 90% until the server responds
        item.percent = Math.min(90, Math.round((progressEvent.loaded * 90) / progressEvent.total))
        uploadQueue.value = [...uploadQueue.value]
      }
    })
      .then(() => {
        item.status = 'success'
        item.percent = 100
        uploadQueue.value = [...uploadQueue.value]
        resolve()
      })
      .catch((err) => {
        if (err?.message === 'Cancelled') {
          resolve()
          return
        }
        item.status = 'failed'
        item.percent = 0
        uploadQueue.value = [...uploadQueue.value]
        reject(err)
      })
  })
}

async function startUpload() {
  if (pendingFiles.value.length === 0) return

  uploading.value = true
  cancelToken.value = false
  showSummary.value = false

  const files = [...pendingFiles.value]
  let success = 0
  let fail = 0

  for (const file of files) {
    if (cancelToken.value) break
    try {
      await uploadSingleFile(file)
      success++
    } catch {
      fail++
    }
  }

  uploading.value = false
  cancelToken.value = false

  if (fail === 0 && success > 0 && uploadQueue.value.every((f) => f.status === 'success')) {
    showSummary.value = true
    summaryText.value = `成功上传 ${success} 个文件`
  } else if (fail > 0 && success > 0) {
    showSummary.value = true
    summaryText.value = `${success} 个成功，${fail} 个失败`
  } else if (fail > 0 && success === 0) {
    ElMessage.error('所有文件上传失败')
  }
}

function cancelUpload() {
  cancelToken.value = true
  uploading.value = false

  // Reset in-progress items back to pending
  uploadQueue.value = uploadQueue.value.map((item) => {
    if (item.status === 'uploading') {
      return { ...item, status: 'pending' as const, percent: 0 }
    }
    return item
  })
}

async function retryFile(index: number) {
  const item = uploadQueue.value[index]
  if (!item || item.status !== 'failed') return

  item.status = 'pending'
  item.percent = 0
  uploading.value = true
  cancelToken.value = false
  showSummary.value = false

  try {
    await uploadSingleFile(item)
  } catch {
    // handled in uploadSingleFile
  } finally {
    uploading.value = false

    const allDone = uploadQueue.value.every((f) => f.status === 'success')
    const hasFailed = uploadQueue.value.some((f) => f.status === 'failed')
    if (allDone) {
      showSummary.value = true
      summaryText.value = `成功上传 ${successCount.value} 个文件`
    } else if (hasFailed && successCount.value > 0) {
      showSummary.value = true
      summaryText.value = `${successCount.value} 个成功，${failedCount.value} 个失败`
    }
  }
}

function resetUpload() {
  uploadQueue.value = []
  showSummary.value = false
  summaryText.value = ''
}

function goToList() {
  router.push('/documents')
}
</script>

<style scoped lang="scss">
.document-upload {
  .page-header {
    margin-bottom: 20px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin-top: 12px;
    }
  }

  .upload-card {
    background: #fff;
    border-radius: 4px;
    padding: 32px;
    margin-bottom: 16px;
  }

  .upload-drop-zone {
    width: 100%;

    :deep(.el-upload-dragger) {
      width: 100%;
    }
  }

  .drop-zone-content {
    padding: 40px 0;
    text-align: center;

    .upload-icon {
      font-size: 48px;
      color: #409eff;
      margin-bottom: 16px;
    }

    .upload-text {
      .upload-title {
        font-size: 16px;
        color: #303133;
        margin-bottom: 8px;

        em {
          color: #409eff;
          font-style: normal;
        }
      }

      .upload-hint {
        font-size: 13px;
        color: #909399;
      }
    }
  }

  .queue-card {
    background: #fff;
    border-radius: 4px;
    padding: 20px;
    margin-bottom: 16px;

    .queue-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      h3 {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .queue-summary {
        font-size: 14px;
        color: #606266;

        .text-success {
          color: #67c23a;
        }

        .text-danger {
          color: #f56c6c;
        }
      }
    }

    .queue-table {
      .file-name-cell {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #303133;
      }
    }

    .queue-actions {
      display: flex;
      gap: 12px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;
    }
  }

  .summary-card {
    background: #fff;
    border-radius: 4px;
    padding: 40px;
  }

  .text-success {
    color: #67c23a;
  }

  .text-danger {
    color: #f56c6c;
  }
}
</style>

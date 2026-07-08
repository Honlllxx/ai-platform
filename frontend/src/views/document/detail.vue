<template>
  <div class="page-container document-detail" v-loading="loading">
    <!-- Error state -->
    <el-alert v-if="loadError" type="error" title="加载文档信息失败" :closable="false" show-icon style="margin-bottom: 16px" />

    <!-- Document Header -->
    <div class="doc-header">
      <el-page-header @back="goBack" :title="'返回'">
        <template #content>
          <div class="doc-header-content">
            <el-icon :size="20" color="#409eff"><Document /></el-icon>
            <span class="doc-name">{{ docInfo.name }}</span>
            <el-tag :type="fileTypeTag(docInfo.type)" size="small">{{ docInfo.type?.toUpperCase() }}</el-tag>
            <el-tag :type="statusTag(docInfo.status)" size="small">{{ statusLabel(docInfo.status) }}</el-tag>
          </div>
        </template>
        <template #extra>
          <span class="doc-time">上传时间：{{ docInfo.uploadTime }}</span>
        </template>
      </el-page-header>
    </div>

    <!-- Tab Views -->
    <div class="doc-tabs">
      <el-tabs :model-value="activeTab" @tab-click="handleTabClick" type="border-card">
        <el-tab-pane label="版面分析" name="layout">
          <template #label>
            <span class="tab-label">
              <el-icon><Picture /></el-icon>
              版面分析
            </span>
          </template>
          <LayoutView :document-id="docId" />
        </el-tab-pane>
        <el-tab-pane label="OCR 结果" name="ocr">
          <template #label>
            <span class="tab-label">
              <el-icon><Reading /></el-icon>
              OCR 结果
            </span>
          </template>
          <OcrView :document-id="docId" />
        </el-tab-pane>
        <el-tab-pane label="表格提取" name="table">
          <template #label>
            <span class="tab-label">
              <el-icon><Grid /></el-icon>
              表格提取
            </span>
          </template>
          <TableView :document-id="docId" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Picture, Reading, Grid } from '@element-plus/icons-vue'
import { getDocument } from '@/api/document'
import LayoutView from './components/LayoutView.vue'
import OcrView from './components/OcrView.vue'
import TableView from './components/TableView.vue'

const route = useRoute()
const router = useRouter()

const docId = computed(() => Number(route.params.id))
const loading = ref(false)
const loadError = ref(false)

const docInfo = reactive({
  id: 0,
  name: '',
  type: '',
  status: '',
  uploadTime: ''
})

const activeTab = ref('layout')

const tabRouteMap: Record<string, string> = {
  layout: `/documents/${docId.value}/layout`,
  ocr: `/documents/${docId.value}/ocr`,
  table: `/documents/${docId.value}/table`
}

function fileTypeTag(type: string) {
  const map: Record<string, string> = {
    pdf: 'danger',
    docx: 'primary',
    doc: 'primary',
    xlsx: 'success',
    xls: 'success',
    png: 'warning',
    jpg: 'warning',
    jpeg: 'warning',
    txt: 'info'
  }
  return map[type?.toLowerCase()] || 'info'
}

function statusTag(status: string) {
  const map: Record<string, string> = {
    uploaded: 'primary',
    processing: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    uploaded: '已上传',
    processing: '处理中',
    completed: '已完成',
    failed: '处理失败'
  }
  return map[status] || status
}

function syncTabFromRoute() {
  const path = route.path
  if (path.endsWith('/ocr') || path.includes('/ocr')) {
    activeTab.value = 'ocr'
  } else if (path.endsWith('/table') || path.includes('/table')) {
    activeTab.value = 'table'
  } else {
    activeTab.value = 'layout'
  }
}

function handleTabClick(tab: any) {
  const tabName = tab.props?.name || tab.paneName || tab
  const newPath = `/documents/${docId.value}/${tabName}`
  if (route.path !== newPath) {
    router.push(newPath)
  }
}

function goBack() {
  router.push('/documents')
}

async function loadDocInfo() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getDocument(docId.value)
    const data = res?.data || res
    docInfo.id = data.id || docId.value
    docInfo.name = data.fileName || data.name || `文档_${docId.value}`
    docInfo.type = data.fileType || data.type || ''
    docInfo.status = data.status || ''
    docInfo.uploadTime = data.uploadTime || data.createdAt || ''
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  syncTabFromRoute()
  loadDocInfo()
})

watch(docId, () => {
  syncTabFromRoute()
  loadDocInfo()
})
</script>

<style scoped lang="scss">
.document-detail {
  .doc-header {
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

  .doc-header-content {
    display: flex;
    align-items: center;
    gap: 10px;

    .doc-name {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .doc-time {
    font-size: 13px;
    color: #909399;
  }

  .doc-tabs {
    :deep(.el-tabs__content) {
      padding: 20px;
      min-height: 500px;
    }
  }

  .tab-label {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
</style>

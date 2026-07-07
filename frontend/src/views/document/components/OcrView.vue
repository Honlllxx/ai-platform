<template>
  <div class="ocr-view">
    <!-- Stats Cards -->
    <div class="ocr-stats">
      <div class="stat-item">
        <span class="stat-label">总字符数</span>
        <span class="stat-value">{{ ocrStats.totalChars }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">平均置信度</span>
        <span class="stat-value stat-confidence">{{ (ocrStats.avgConfidence * 100).toFixed(1) }}%</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">检测语言</span>
        <span class="stat-value">{{ ocrStats.language }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">文本条目</span>
        <span class="stat-value">{{ filteredEntries.length }} / {{ ocrEntries.length }}</span>
      </div>
    </div>

    <!-- Search -->
    <div class="ocr-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文本内容..."
        :prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>

    <!-- Split View -->
    <div class="ocr-main">
      <!-- Left: Document Image -->
      <div class="ocr-image-panel">
        <ImageAnnotator
          :regions="ocrRegions"
          :highlighted-id="activeEntryId"
          @region-click="handleRegionClick"
        />
      </div>

      <!-- Right: Text Entries -->
      <div class="ocr-entries-panel">
        <div class="entries-header">
          <span class="entries-title">识别结果</span>
          <el-tag size="small" type="info">{{ filteredEntries.length }} 条</el-tag>
        </div>

        <div class="entries-list" ref="entriesListRef">
          <div
            v-for="entry in filteredEntries"
            :key="entry.id"
            class="entry-card"
            :class="{ 'entry-active': activeEntryId === entry.id }"
            @click="handleEntryClick(entry)"
          >
            <div class="entry-text">{{ entry.text }}</div>
            <div class="entry-meta">
              <div class="entry-confidence">
                <span class="confidence-label">置信度</span>
                <el-progress
                  :percentage="Math.round(entry.confidence * 100)"
                  :color="confidenceColor(entry.confidence)"
                  :stroke-width="6"
                />
              </div>
              <div class="entry-bbox">
                <el-icon :size="12"><Aim /></el-icon>
                <span>
                  ({{ entry.bbox.x }}, {{ entry.bbox.y }}) {{ entry.bbox.width }}x{{ entry.bbox.height }}
                </span>
              </div>
            </div>
          </div>

          <el-empty v-if="filteredEntries.length === 0" description="未找到匹配的文本" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Aim } from '@element-plus/icons-vue'
import ImageAnnotator, { type AnnotatorRegion } from './ImageAnnotator.vue'
import { getDocumentOcr } from '@/api/pipeline'

const props = defineProps<{
  documentId: number
}>()

interface OcrEntry {
  id: number
  text: string
  confidence: number
  bbox: { x: number; y: number; width: number; height: number }
}

const searchKeyword = ref('')
const activeEntryId = ref<number | string | null>(null)
const entriesListRef = ref<HTMLElement>()
const ocrEntries = ref<OcrEntry[]>([])

const ocrStats = reactive({
  totalChars: 0,
  avgConfidence: 0,
  language: '中文 (zh-CN)'
})

const filteredEntries = computed(() => {
  if (!searchKeyword.value.trim()) {
    return ocrEntries.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return ocrEntries.value.filter(entry =>
    entry.text.toLowerCase().includes(keyword)
  )
})

const ocrRegions = computed<AnnotatorRegion[]>(() => {
  return ocrEntries.value.map((entry, index) => ({
    id: entry.id,
    label: `文本${index + 1}`,
    x: entry.bbox.x,
    y: entry.bbox.y,
    width: entry.bbox.width,
    height: entry.bbox.height,
    color: confidenceColor(entry.confidence),
    type: 'text' as const,
    confidence: entry.confidence
  }))
})

function confidenceColor(confidence: number): string {
  if (confidence >= 0.9) return '#67c23a'
  if (confidence >= 0.7) return '#e6a23c'
  return '#f56c6c'
}

function handleEntryClick(entry: OcrEntry) {
  if (activeEntryId.value === entry.id) {
    activeEntryId.value = null
  } else {
    activeEntryId.value = entry.id
  }
}

function handleRegionClick(region: AnnotatorRegion) {
  handleEntryClick(region as unknown as OcrEntry)
}

function loadOcrData() {
  // Calculate stats from OCR entries
  function calcStats(entries: OcrEntry[]) {
    ocrStats.totalChars = entries.reduce((sum, e) => sum + e.text.length, 0)
    ocrStats.avgConfidence = entries.length > 0
      ? entries.reduce((sum, e) => sum + e.confidence, 0) / entries.length
      : 0
    ocrStats.language = '中文 (zh-CN)'
  }

  // Mock fallback
  const mockEntries: OcrEntry[] = [
    { id: 1, text: '采购合同协议书', confidence: 0.99, bbox: { x: 5, y: 3, width: 90, height: 6 } },
    { id: 2, text: '甲方（买方）：北京科技有限公司', confidence: 0.96, bbox: { x: 5, y: 11, width: 44, height: 5 } },
    { id: 3, text: '第一条 合同标的', confidence: 0.97, bbox: { x: 5, y: 27, width: 44, height: 5 } },
    { id: 4, text: '第二条 产品质量标准', confidence: 0.96, bbox: { x: 51, y: 27, width: 44, height: 5 } },
    { id: 5, text: '第三条 合同价款', confidence: 0.98, bbox: { x: 5, y: 50, width: 30, height: 5 } },
    { id: 6, text: '合同总金额为人民币壹佰贰拾万元整（¥1,200,000.00），含13%增值税。', confidence: 0.92, bbox: { x: 5, y: 56, width: 90, height: 5 } },
    { id: 7, text: '第四条 交货时间与地点', confidence: 0.97, bbox: { x: 5, y: 75, width: 44, height: 5 } },
    { id: 8, text: '乙方应于合同生效后30个工作日内完成交货。', confidence: 0.89, bbox: { x: 5, y: 81, width: 90, height: 10 } },
  ]

  // Try real API first, use mock as fallback
  if (props.documentId) {
    getDocumentOcr(props.documentId)
      .then((res: any) => {
        const entries = res?.data?.entries || res?.data
        if (entries && entries.length > 0) {
          ocrEntries.value = entries
          calcStats(entries)
        }
      })
      .catch(() => {
        ocrEntries.value = mockEntries
        calcStats(mockEntries)
      })
  } else {
    ocrEntries.value = mockEntries
    calcStats(mockEntries)
  }
}

onMounted(() => {
  loadOcrData()
})
</script>

<style scoped lang="scss">
.ocr-view {
  .ocr-stats {
    display: flex;
    gap: 16px;
    margin-bottom: 16px;
  }

  .stat-item {
    flex: 1;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .stat-label {
    font-size: 12px;
    color: #909399;
  }

  .stat-value {
    font-size: 20px;
    font-weight: 700;
    color: #303133;

    &.stat-confidence {
      color: #67c23a;
    }
  }

  .ocr-search {
    margin-bottom: 16px;
  }

  .search-input {
    max-width: 400px;
  }

  .ocr-main {
    display: flex;
    gap: 16px;
    min-height: 520px;
  }

  .ocr-image-panel {
    flex: 1;
    min-width: 0;
  }

  .ocr-entries-panel {
    width: 380px;
    flex-shrink: 0;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.entries-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;

  .entries-title {
    font-size: 15px;
    color: #303133;
    font-weight: 500;
  }
}

.entries-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.entry-card {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    border-color: #409eff;
    background: #f5f7fa;
  }

  &.entry-active {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.3);
  }
}

.entry-text {
  font-size: 13px;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.6;
  word-break: break-all;
}

.entry-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.entry-confidence {
  display: flex;
  align-items: center;
  gap: 8px;

  .confidence-label {
    font-size: 11px;
    color: #909399;
    flex-shrink: 0;
    min-width: 40px;
  }

  :deep(.el-progress) {
    flex: 1;
  }
}

.entry-bbox {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #c0c4cc;
}
</style>

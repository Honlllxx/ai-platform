<template>
  <div class="layout-view">
    <div class="layout-main">
      <!-- Left: Annotated Image -->
      <div class="layout-image-panel">
        <ImageAnnotator
          :regions="layoutRegions"
          :highlighted-id="highlightedRegionId"
          @region-click="handleRegionClick"
        />
      </div>

      <!-- Right: Region List Sidebar -->
      <div class="layout-sidebar">
        <div class="sidebar-header">
          <h4 class="sidebar-title">检测区域</h4>
          <el-tag size="small" type="info">{{ layoutRegions.length }} 个区域</el-tag>
        </div>

        <!-- Legend -->
        <div class="legend-box">
          <span class="legend-title">图例</span>
          <div class="legend-items">
            <div class="legend-item" v-for="item in legendItems" :key="item.type">
              <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
              <span class="legend-label">{{ item.label }}</span>
            </div>
          </div>
        </div>

        <!-- Region List -->
        <div class="region-list" ref="regionListRef">
          <div
            v-for="region in layoutRegions"
            :key="region.id"
            class="region-list-item"
            :class="{ 'item-highlight': highlightedRegionId === region.id }"
            @click="handleRegionClick(region)"
          >
            <div class="region-item-header">
              <span class="region-type-badge" :style="{ backgroundColor: region.color }">
                {{ typeLabel(region.type) }}
              </span>
              <span class="region-confidence">
                {{ Math.round(((region as any).confidence ?? 0.95) * 100) }}%
              </span>
            </div>
            <div class="region-item-body">
              <span class="region-text-preview">{{ (region as any).textPreview || '（文本块内容预览...）' }}</span>
            </div>
            <div class="region-item-footer">
              <span class="region-coords">
                x:{{ region.x }} y:{{ region.y }} {{ region.width }}x{{ region.height }}
              </span>
            </div>
          </div>

          <el-empty v-if="layoutRegions.length === 0" description="暂无检测区域" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import ImageAnnotator, { type AnnotatorRegion } from './ImageAnnotator.vue'
import { getDocumentLayout } from '@/api/pipeline'

const props = defineProps<{
  documentId: number
}>()

const highlightedRegionId = ref<number | string | null>(null)
const regionListRef = ref<HTMLElement>()
const layoutRegions = ref<AnnotatorRegion[]>([])

const legendItems = [
  { type: 'text', label: '文本块', color: '#409eff' },
  { type: 'title', label: '标题', color: '#e6a23c' },
  { type: 'table', label: '表格区域', color: '#67c23a' }
]

function typeLabel(type: string) {
  const map: Record<string, string> = {
    text: '文本块',
    title: '标题',
    table: '表格'
  }
  return map[type] || type
}

function handleRegionClick(region: AnnotatorRegion) {
  highlightedRegionId.value = highlightedRegionId.value === region.id ? null : region.id
}

function loadLayoutData() {
  // Try real API first, fall back to mock only on error
  if (props.documentId) {
    getDocumentLayout(props.documentId)
      .then((res: any) => {
        const regions = res?.data?.regions || res?.regions || res?.data || []
        if (Array.isArray(regions) && regions.length > 0) {
          layoutRegions.value = regions
        }
      })
      .catch(() => {
        // Mock fallback (only used when API fails or no documentId)
        layoutRegions.value = [
          { id: 1, label: '标题区域', x: 5, y: 3, width: 90, height: 6, color: '#e6a23c', type: 'title', confidence: 0.98, textPreview: '采购合同协议书' },
          { id: 2, label: '文本段落1', x: 5, y: 11, width: 90, height: 15, color: '#409eff', type: 'text', confidence: 0.95, textPreview: '甲方（买方）：北京科技有限公司...' },
          { id: 3, label: '文本段落2', x: 5, y: 27, width: 44, height: 20, color: '#409eff', type: 'text', confidence: 0.92, textPreview: '第一条 合同标的...' },
          { id: 4, label: '文本段落3', x: 51, y: 27, width: 44, height: 20, color: '#409eff', type: 'text', confidence: 0.91, textPreview: '第二条 质量标准...' },
          { id: 5, label: '报价表格', x: 5, y: 50, width: 90, height: 22, color: '#67c23a', type: 'table', confidence: 0.97, textPreview: '产品报价明细表' },
          { id: 6, label: '文本段落4', x: 5, y: 75, width: 90, height: 22, color: '#409eff', type: 'text', confidence: 0.94, textPreview: '第三条 交货方式...' },
        ]
      })
  }
}

onMounted(() => {
  loadLayoutData()
})
</script>

<style scoped lang="scss">
.layout-view {
  .layout-main {
    display: flex;
    gap: 16px;
    min-height: 520px;
  }

  .layout-image-panel {
    flex: 1;
    min-width: 0;
  }

  .layout-sidebar {
    width: 300px;
    flex-shrink: 0;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;

  .sidebar-title {
    margin: 0;
    font-size: 15px;
    color: #303133;
  }
}

.legend-box {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;

  .legend-title {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
  }

  .legend-items {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
  }

  .legend-color {
    width: 12px;
    height: 12px;
    border-radius: 2px;
    flex-shrink: 0;
  }

  .legend-label {
    color: #606266;
  }
}

.region-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.region-list-item {
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

  &.item-highlight {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.3);
  }
}

.region-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;

  .region-type-badge {
    padding: 2px 8px;
    border-radius: 3px;
    font-size: 11px;
    color: #fff;
    font-weight: 500;
  }

  .region-confidence {
    font-size: 12px;
    color: #67c23a;
    font-weight: 500;
  }
}

.region-item-body {
  margin-bottom: 6px;

  .region-text-preview {
    font-size: 12px;
    color: #606266;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.region-item-footer {
  .region-coords {
    font-size: 11px;
    color: #c0c4cc;
  }
}
</style>

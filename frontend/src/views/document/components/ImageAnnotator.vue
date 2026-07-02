<template>
  <div class="image-annotator" ref="containerRef">
    <!-- Toolbar -->
    <div class="annotator-toolbar">
      <span class="toolbar-title">文档预览</span>
      <div class="toolbar-actions">
        <el-button-group size="small">
          <el-button :icon="ZoomOut" :disabled="scale <= 0.3" @click="zoomOut" />
          <el-button disabled class="scale-display">{{ Math.round(scale * 100) }}%</el-button>
          <el-button :icon="ZoomIn" :disabled="scale >= 3" @click="zoomIn" />
          <el-button :icon="FullScreen" @click="resetZoom" />
        </el-button-group>
      </div>
    </div>

    <!-- Image Preview Area -->
    <div class="annotator-viewport" ref="viewportRef" @wheel.prevent="handleWheel">
      <div class="annotator-canvas" :style="canvasStyle">
        <!-- Placeholder document image -->
        <div class="doc-placeholder">
          <el-icon :size="56" color="#c0c4cc"><Document /></el-icon>
          <span class="placeholder-text">文档预览区域</span>
          <span class="placeholder-sub">支持 PDF、DOCX、图片等格式</span>
        </div>

        <!-- Overlay regions -->
        <div
          v-for="region in regions"
          :key="region.id"
          class="annotator-region"
          :class="{ 'region-highlight': highlightedId === region.id }"
          :style="regionStyle(region)"
          @mouseenter="handleRegionHover(region)"
          @mouseleave="handleRegionLeave"
          @click="$emit('region-click', region)"
        >
          <div class="region-label" :style="{ backgroundColor: region.color }">
            <span class="region-label-text">{{ region.label || region.type }}</span>
          </div>

          <!-- Tooltip on hover -->
          <transition name="tooltip-fade">
            <div v-if="hoveredId === region.id" class="region-tooltip">
              <div class="tooltip-item">
                <span class="tooltip-key">类型：</span>
                <span class="tooltip-value">{{ typeLabel(region.type) }}</span>
              </div>
              <div class="tooltip-item">
                <span class="tooltip-key">置信度：</span>
                <span class="tooltip-value">{{ Math.round(((region as any).confidence ?? 0.95) * 100) }}%</span>
              </div>
              <div class="tooltip-item">
                <span class="tooltip-key">坐标：</span>
                <span class="tooltip-value">({{ region.x }}, {{ region.y }}, {{ region.width }}x{{ region.height }})</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ZoomIn, ZoomOut, FullScreen, Document } from '@element-plus/icons-vue'

export interface AnnotatorRegion {
  id: number | string
  label: string
  x: number
  y: number
  width: number
  height: number
  color: string
  type: 'text' | 'title' | 'table'
  confidence?: number
  textPreview?: string
}

const props = defineProps<{
  imageUrl?: string
  regions: AnnotatorRegion[]
  highlightedId?: number | string | null
}>()

defineEmits<{
  'region-click': [region: AnnotatorRegion]
}>()

const scale = ref(1)
const hoveredId = ref<number | string | null>(null)
const containerRef = ref<HTMLElement>()
const viewportRef = ref<HTMLElement>()

const canvasStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transformOrigin: 'center center',
  transition: 'transform 0.2s ease'
}))

function regionStyle(region: AnnotatorRegion) {
  return {
    left: `${region.x}%`,
    top: `${region.y}%`,
    width: `${region.width}%`,
    height: `${region.height}%`,
    borderColor: region.color
  }
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    text: '文本块',
    title: '标题',
    table: '表格区域'
  }
  return map[type] || type
}

function handleRegionHover(region: AnnotatorRegion) {
  hoveredId.value = region.id
}

function handleRegionLeave() {
  hoveredId.value = null
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.15, 3)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.15, 0.3)
}

function resetZoom() {
  scale.value = 1
}

function handleWheel(e: WheelEvent) {
  if (e.ctrlKey || e.metaKey) {
    e.preventDefault()
    if (e.deltaY < 0) {
      zoomIn()
    } else {
      zoomOut()
    }
  }
}
</script>

<style scoped lang="scss">
.image-annotator {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.annotator-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;

  .toolbar-title {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .scale-display {
    min-width: 50px;
    cursor: default;
    opacity: 1 !important;
  }
}

.annotator-viewport {
  position: relative;
  width: 100%;
  height: 520px;
  overflow: auto;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.annotator-canvas {
  position: relative;
  width: 80%;
  height: 90%;
  min-width: 600px;
  min-height: 400px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.doc-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;

  .placeholder-text {
    font-size: 18px;
    color: #909399;
    font-weight: 500;
  }

  .placeholder-sub {
    font-size: 13px;
    color: #c0c4cc;
  }
}

.annotator-region {
  position: absolute;
  border: 2px dashed;
  background: rgba(64, 158, 255, 0.08);
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(64, 158, 255, 0.18);
  }

  &.region-highlight {
    border-width: 3px;
    background: rgba(64, 158, 255, 0.2);
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
    z-index: 10;
  }
}

.region-label {
  position: absolute;
  top: -2px;
  left: -2px;
  padding: 2px 8px;
  border-radius: 0 0 4px 0;
  font-size: 11px;
  color: #fff;
  white-space: nowrap;
  font-weight: 500;
  max-width: calc(100% + 4px);
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.region-tooltip {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 100;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  min-width: 160px;
  pointer-events: none;
}

.tooltip-item {
  display: flex;
  font-size: 12px;
  line-height: 1.8;

  .tooltip-key {
    color: #909399;
    flex-shrink: 0;
  }

  .tooltip-value {
    color: #303133;
    font-weight: 500;
  }
}

.tooltip-fade-enter-active,
.tooltip-fade-leave-active {
  transition: opacity 0.15s ease;
}

.tooltip-fade-enter-from,
.tooltip-fade-leave-to {
  opacity: 0;
}
</style>

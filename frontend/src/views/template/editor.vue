<template>
  <div class="page-container template-editor">
    <!-- Header bar -->
    <div class="editor-header">
      <div class="editor-header__left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2 class="editor-title">{{ templateName }}</h2>
        <el-tag size="small" type="info">v{{ templateVersion }}</el-tag>
      </div>
      <div class="editor-header__right">
        <span class="region-count">已标注 {{ regions.length }} 个区域</span>
        <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <!-- Main editor area -->
    <div class="editor-body">
      <!-- Left: Document image with region drawing -->
      <div class="editor-image-panel">
        <div class="image-toolbar">
          <span class="image-toolbar__label">文档预览</span>
          <span class="image-toolbar__hint">拖拽鼠标框选区域</span>
        </div>
        <div
          ref="canvasRef"
          class="image-canvas"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
          @mouseleave="onMouseUp"
        >
          <!-- Document placeholder -->
          <div class="document-placeholder">
            <el-icon :size="80" color="#c0c4cc"><PictureFilled /></el-icon>
            <p class="placeholder-text">文档图片区域</p>
            <p class="placeholder-sub">在此区域拖拽鼠标绘制识别区域</p>
            <p class="placeholder-dim">{{ imageWidth }} x {{ imageHeight }}</p>
          </div>

          <!-- Draw regions as colored overlays -->
          <div
            v-for="region in regions"
            :key="region.id"
            class="drawn-region"
            :class="{ 'drawn-region--selected': selectedRegionId === region.id }"
            :style="regionStyle(region)"
            @mousedown.stop="selectRegion(region.id)"
          >
            <span class="drawn-region__label">{{ region.fieldName || region.fieldCode || '未绑定' }}</span>
          </div>

          <!-- Active selection rectangle -->
          <div
            v-if="isDrawing && selectionRect"
            class="selection-rect"
            :style="{
              left: selectionRect.left + 'px',
              top: selectionRect.top + 'px',
              width: selectionRect.width + 'px',
              height: selectionRect.height + 'px'
            }"
          />

          <!-- Crosshair cursor indicator -->
          <div class="canvas-overlay" :class="{ 'canvas-overlay--drawing': isDrawing }" />
        </div>
      </div>

      <!-- Right: Region list and properties -->
      <div class="editor-properties-panel">
        <div class="properties-header">
          <span>区域列表</span>
        </div>

        <!-- Region list -->
        <div class="region-list" v-if="regions.length > 0">
          <div
            v-for="region in regions"
            :key="region.id"
            class="region-item"
            :class="{ 'region-item--selected': selectedRegionId === region.id }"
            @click="selectRegion(region.id)"
          >
            <div class="region-item__header">
              <span
                class="region-item__color"
                :style="{ background: regionColors[regions.indexOf(region) % regionColors.length] }"
              />
              <span class="region-item__name">{{ region.fieldName || '区域 ' + (regions.indexOf(region) + 1) }}</span>
              <el-tag size="small" type="info">{{ typeLabel(region.type) }}</el-tag>
            </div>
            <div class="region-item__coords">
              ({{ region.x }}, {{ region.y }}) {{ region.width }}x{{ region.height }}
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无区域，请在左侧文档上拖拽绘制" :image-size="60" />

        <!-- Selected region properties -->
        <div class="region-properties" v-if="selectedRegion">
          <h4>区域属性</h4>
          <el-form label-width="60px" size="small">
            <el-form-item label="字段">
              <el-select
                v-model="selectedRegion.fieldCode"
                placeholder="选择元数据字段"
                filterable
                style="width: 100%;"
                @change="onFieldChange"
              >
                <el-option
                  v-for="field in availableFields"
                  :key="field.code"
                  :label="field.name"
                  :value="field.code"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="selectedRegion.type" style="width: 100%;">
                <el-option label="文本" value="text" />
                <el-option label="表格" value="table" />
                <el-option label="键值对" value="key_value" />
              </el-select>
            </el-form-item>
            <el-form-item label="X">
              <el-input-number v-model="selectedRegion.x" :min="0" :max="imageWidth" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="Y">
              <el-input-number v-model="selectedRegion.y" :min="0" :max="imageHeight" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="宽度">
              <el-input-number v-model="selectedRegion.width" :min="1" :max="imageWidth" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="高度">
              <el-input-number v-model="selectedRegion.height" :min="1" :max="imageHeight" style="width: 100%;" />
            </el-form-item>
          </el-form>
          <el-button type="danger" :icon="Delete" size="small" @click="deleteSelectedRegion">
            删除区域
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Delete, PictureFilled } from '@element-plus/icons-vue'
import { getTemplate, updateTemplate, type TemplateItem, type TemplateRegion } from '@/api/template'
import { getMetadataFields, type MetadataField } from '@/api/metadata'

const route = useRoute()
const router = useRouter()

const templateId = computed(() => Number(route.params.id))

// Template state
const templateName = ref('加载中...')
const templateVersion = ref(0)
const templateDocumentType = ref('')
const saving = ref(false)
const regions = reactive<TemplateRegion[]>([])
const selectedRegionId = ref<string | null>(null)
const availableFields = ref<MetadataField[]>([])
let regionIdCounter = 0

const regionColors = [
  'rgba(64, 158, 255, 0.35)',
  'rgba(103, 194, 58, 0.35)',
  'rgba(230, 162, 60, 0.35)',
  'rgba(245, 108, 108, 0.35)',
  'rgba(144, 147, 153, 0.35)',
  'rgba(160, 60, 200, 0.35)',
  'rgba(60, 200, 190, 0.35)',
  'rgba(200, 120, 60, 0.35)',
]

const selectedRegion = computed(() =>
  selectedRegionId.value
    ? regions.find(r => r.id === selectedRegionId.value) || null
    : null
)

// Drawing state
const canvasRef = ref<HTMLElement>()
const imageWidth = 800
const imageHeight = 600
const isDrawing = ref(false)
const selectionRect = ref<{ left: number; top: number; width: number; height: number } | null>(null)
const dragStart = reactive({ x: 0, y: 0 })

function getCanvasOffset() {
  if (!canvasRef.value) return { left: 0, top: 0 }
  const rect = canvasRef.value.getBoundingClientRect()
  const computedStyle = getComputedStyle(canvasRef.value)
  const paddingLeft = parseFloat(computedStyle.paddingLeft) || 0
  const paddingTop = parseFloat(computedStyle.paddingTop) || 0
  return {
    left: rect.left + paddingLeft,
    top: rect.top + paddingTop
  }
}

function onMouseDown(e: MouseEvent) {
  const offset = getCanvasOffset()
  dragStart.x = e.clientX - offset.left
  dragStart.y = e.clientY - offset.top
  isDrawing.value = true
  selectionRect.value = {
    left: dragStart.x,
    top: dragStart.y,
    width: 0,
    height: 0
  }
}

function onMouseMove(e: MouseEvent) {
  if (!isDrawing.value || !selectionRect.value) return
  const offset = getCanvasOffset()
  const currentX = e.clientX - offset.left
  const currentY = e.clientY - offset.top

  selectionRect.value.left = Math.min(dragStart.x, currentX)
  selectionRect.value.top = Math.min(dragStart.y, currentY)
  selectionRect.value.width = Math.abs(currentX - dragStart.x)
  selectionRect.value.height = Math.abs(currentY - dragStart.y)
}

function onMouseUp() {
  if (!isDrawing.value || !selectionRect.value) {
    isDrawing.value = false
    selectionRect.value = null
    return
  }

  // Only create region if width and height are meaningful (min 10px)
  if (selectionRect.value.width >= 10 && selectionRect.value.height >= 10) {
    const newRegion: TemplateRegion = {
      id: `region_${++regionIdCounter}`,
      x: Math.round(selectionRect.value.left),
      y: Math.round(selectionRect.value.top),
      width: Math.round(selectionRect.value.width),
      height: Math.round(selectionRect.value.height),
      fieldCode: '',
      fieldName: '',
      type: 'text'
    }
    regions.push(newRegion)
    selectedRegionId.value = newRegion.id
  }

  isDrawing.value = false
  selectionRect.value = null
}

function selectRegion(id: string) {
  selectedRegionId.value = id
}

function regionStyle(region: TemplateRegion) {
  const idx = regions.indexOf(region)
  const color = regionColors[idx % regionColors.length]
  const isSelected = selectedRegionId.value === region.id
  return {
    left: region.x + 'px',
    top: region.y + 'px',
    width: region.width + 'px',
    height: region.height + 'px',
    background: isSelected ? color.replace('0.35', '0.55') : color,
    borderColor: isSelected ? '#409eff' : color.replace('0.35', '0.7'),
    borderWidth: isSelected ? '2px' : '1px',
  }
}

function onFieldChange(fieldCode: string) {
  if (!selectedRegion.value) return
  const field = availableFields.value.find(f => f.code === fieldCode)
  if (field) {
    selectedRegion.value.fieldName = field.name
  }
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    text: '文本',
    table: '表格',
    key_value: '键值对'
  }
  return map[type] || type
}

function deleteSelectedRegion() {
  const idx = regions.findIndex(r => r.id === selectedRegionId.value)
  if (idx !== -1) {
    regions.splice(idx, 1)
    selectedRegionId.value = null
  }
}

// Keyboard: Delete to remove selected region
function handleKeyboard(e: KeyboardEvent) {
  if (e.key === 'Delete' || e.key === 'Backspace') {
    // Don't delete if focus is in an input/select
    const tag = (e.target as HTMLElement)?.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
    if (selectedRegion.value) {
      deleteSelectedRegion()
    }
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateTemplate(templateId.value, {
      regions: JSON.parse(JSON.stringify(regions))
    })
    ElMessage.success('模板保存成功')
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/templates')
}

async function loadTemplate() {
  try {
    const res = await getTemplate(templateId.value)
    const data = res?.data || res
    if (data) {
      templateName.value = data.name || '未命名模板'
      templateVersion.value = data.version || 1
      templateDocumentType.value = data.documentType || ''
      if (data.regions && data.regions.length > 0) {
        regions.length = 0
        data.regions.forEach((r: TemplateRegion) => {
          regions.push({ ...r })
          const idNum = parseInt(r.id.replace(/\D/g, '')) || 0
          if (idNum > regionIdCounter) regionIdCounter = idNum
        })
      }
    }
  } catch {
    // Load mock data for dev
    templateName.value = '通用合同模板'
    templateVersion.value = 3
    templateDocumentType.value = 'contract'
    const mockRegions: TemplateRegion[] = [
      { id: 'r1', x: 80, y: 40, width: 220, height: 35, fieldCode: 'contract_no', fieldName: '合同编号', type: 'text' },
      { id: 'r2', x: 500, y: 40, width: 160, height: 35, fieldCode: 'sign_date', fieldName: '签署日期', type: 'text' },
      { id: 'r3', x: 80, y: 140, width: 280, height: 35, fieldCode: 'party_a', fieldName: '甲方名称', type: 'text' },
      { id: 'r4', x: 80, y: 210, width: 280, height: 35, fieldCode: 'party_b', fieldName: '乙方名称', type: 'text' },
      { id: 'r5', x: 80, y: 300, width: 200, height: 35, fieldCode: 'contract_amount', fieldName: '合同金额', type: 'text' },
    ]
    regions.length = 0
    mockRegions.forEach(r => regions.push({ ...r }))
    regionIdCounter = 5
  }
}

async function loadFields() {
  try {
    const res = await getMetadataFields({ page: 1, pageSize: 1000 })
    const result = res?.data || res
    availableFields.value = result.records || result || []
  } catch {
    // Mock fields
    availableFields.value = [
      { id: 1, name: '合同编号', code: 'contract_no', type: 'string', length: 50, groupCode: 'contract', groupName: '合同', required: true, sort: 1, status: 1, createTime: '' },
      { id: 2, name: '合同金额', code: 'contract_amount', type: 'number', groupCode: 'contract', groupName: '合同', required: true, sort: 2, status: 1, createTime: '' },
      { id: 3, name: '签署日期', code: 'sign_date', type: 'date', groupCode: 'contract', groupName: '合同', required: true, sort: 3, status: 1, createTime: '' },
      { id: 4, name: '甲方名称', code: 'party_a', type: 'string', length: 100, groupCode: 'contract', groupName: '合同', required: true, sort: 4, status: 1, createTime: '' },
      { id: 5, name: '乙方名称', code: 'party_b', type: 'string', length: 100, groupCode: 'contract', groupName: '合同', required: true, sort: 5, status: 1, createTime: '' },
      { id: 6, name: '发票号码', code: 'invoice_no', type: 'string', length: 20, groupCode: 'invoice', groupName: '发票', required: true, sort: 1, status: 1, createTime: '' },
      { id: 7, name: '开票日期', code: 'invoice_date', type: 'date', groupCode: 'invoice', groupName: '发票', required: true, sort: 2, status: 1, createTime: '' },
      { id: 8, name: '价税合计', code: 'total_amount', type: 'number', groupCode: 'invoice', groupName: '发票', required: true, sort: 3, status: 1, createTime: '' },
      { id: 9, name: '提单号', code: 'bl_no', type: 'string', length: 30, groupCode: 'bill_of_lading', groupName: '提单', required: true, sort: 1, status: 1, createTime: '' },
      { id: 10, name: '船名', code: 'vessel_name', type: 'string', length: 100, groupCode: 'bill_of_lading', groupName: '提单', required: true, sort: 2, status: 1, createTime: '' },
    ]
  }
}

onMounted(() => {
  loadTemplate()
  loadFields()
  document.addEventListener('keydown', handleKeyboard)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboard)
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.template-editor {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-header {
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

    .region-count {
      font-size: 13px;
      color: $text-secondary;
    }
  }

  .editor-title {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
  }
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// Left panel
.editor-image-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;

  .image-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 16px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;

    &__label {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }

    &__hint {
      font-size: 12px;
      color: $text-secondary;
    }
  }
}

.image-canvas {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  cursor: crosshair;
  user-select: none;
  overflow: hidden;
}

.document-placeholder {
  width: 100%;
  max-width: 900px;
  height: 640px;
  background: #fff;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;

  .placeholder-text {
    font-size: 16px;
    color: $text-secondary;
    margin: 0;
  }

  .placeholder-sub {
    font-size: 13px;
    color: $text-placeholder;
    margin: 0;
  }

  .placeholder-dim {
    font-size: 12px;
    color: $text-placeholder;
    font-family: monospace;
    margin-top: 8px;
  }
}

// Drawn regions
.drawn-region {
  position: absolute;
  border: 1px solid;
  border-radius: 3px;
  transition: border-color 0.15s, border-width 0.15s;
  cursor: pointer;
  pointer-events: auto;

  &:hover {
    filter: brightness(1.1);
  }

  &--selected {
    z-index: 10;
  }

  &__label {
    position: absolute;
    top: -22px;
    left: 0;
    font-size: 11px;
    background: #303133;
    color: #fff;
    padding: 2px 8px;
    border-radius: 3px;
    white-space: nowrap;
    pointer-events: none;
    line-height: 18px;
  }
}

// Active selection rectangle
.selection-rect {
  position: absolute;
  border: 2px dashed #409eff;
  background: rgba(64, 158, 255, 0.1);
  pointer-events: none;
  z-index: 20;
}

.canvas-overlay {
  pointer-events: none;

  &--drawing {
    position: absolute;
    inset: 0;
  }
}

// Right panel
.editor-properties-panel {
  width: 340px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;

  .properties-header {
    padding: 12px 16px;
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;
  }
}

.region-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 0;
}

.region-item {
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: $primary-color;
    background: #f0f8ff;
  }

  &--selected {
    border-color: $primary-color;
    background: #ecf5ff;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }

  &__color {
    width: 12px;
    height: 12px;
    border-radius: 2px;
    flex-shrink: 0;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: $text-primary;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__coords {
    font-size: 11px;
    color: $text-secondary;
    font-family: monospace;
    margin-left: 20px;
  }
}

.region-properties {
  border-top: 1px solid #e4e7ed;
  padding: 16px;
  flex-shrink: 0;
  overflow-y: auto;
  max-height: 55%;

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
  }

  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>

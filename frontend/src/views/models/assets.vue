<template>
  <div class="page-container model-assets">
    <div class="page-header">
      <h2>模型资产管理</h2>
    </div>

    <div class="table-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- Tab 1: Base Models -->
        <el-tab-pane label="基础模型" name="base">
          <el-table :data="baseModelData" v-loading="baseLoading" stripe border style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="name" label="模型名称" min-width="140" />
            <el-table-column prop="version" label="版本" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.version }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参数规模" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.size }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="framework" label="框架" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.framework || '未知' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="assetStatusType(row.status)">
                  {{ assetStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="准确率" width="90" align="center">
              <template #default="{ row }">
                <span v-if="row.metrics?.accuracy" class="metric-value">{{ row.metrics.accuracy }}%</span>
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
            <el-table-column label="F1" width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.metrics?.f1" class="metric-value">{{ row.metrics.f1 }}%</span>
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="basePagination.page"
              v-model:page-size="basePagination.pageSize"
              :total="basePagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="(v: number) => { basePagination.page = v; fetchBase() }"
              @size-change="(v: number) => { basePagination.pageSize = v; basePagination.page = 1; fetchBase() }"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 2: LoRA Adapters -->
        <el-tab-pane label="LoRA 适配器" name="lora">
          <el-table :data="loraData" v-loading="loraLoading" stripe border style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="name" label="适配器名称" min-width="160" />
            <el-table-column prop="version" label="版本" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.version }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="兼容模型" min-width="140">
              <template #default="{ row }">
                <el-tag size="small" type="warning">{{ row.description }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参数量" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.size }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Rank" width="70" align="center">
              <template #default="{ row }">
                <span class="rank-value">r={{ row.framework || '16' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="assetStatusType(row.status)">
                  {{ assetStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="loraPagination.page"
              v-model:page-size="loraPagination.pageSize"
              :total="loraPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="(v: number) => { loraPagination.page = v; fetchLora() }"
              @size-change="(v: number) => { loraPagination.pageSize = v; loraPagination.page = 1; fetchLora() }"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 3: Datasets -->
        <el-tab-pane label="数据集" name="dataset">
          <el-table :data="datasetData" v-loading="datasetLoading" stripe border style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="name" label="数据集名称" min-width="160" />
            <el-table-column prop="version" label="版本" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.version }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="记录数" width="90" align="center">
              <template #default="{ row }">
                {{ row.description }}
              </template>
            </el-table-column>
            <el-table-column label="文档类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.size }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="assetStatusType(row.status)">
                  {{ assetStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="datasetPagination.page"
              v-model:page-size="datasetPagination.pageSize"
              :total="datasetPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="(v: number) => { datasetPagination.page = v; fetchDataset() }"
              @size-change="(v: number) => { datasetPagination.pageSize = v; datasetPagination.page = 1; fetchDataset() }"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 4: Bindings -->
        <el-tab-pane label="三元组绑定" name="bindings">
          <div class="table-toolbar">
            <el-button type="primary" :icon="Plus" @click="openBindingDialog">创建绑定</el-button>
          </div>

          <el-table :data="bindingData" v-loading="bindingLoading" stripe border style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="bindingName" label="绑定名称" min-width="140" />
            <el-table-column label="三元组关系" min-width="360">
              <template #default="{ row }">
                <div class="triad-row">
                  <el-tag size="small" type="primary">{{ row.baseModelName }}</el-tag>
                  <span class="triad-arrow">
                    <el-icon><ArrowRight /></el-icon>
                  </span>
                  <el-tag size="small" type="success">{{ row.loraAdapterName }}</el-tag>
                  <span class="triad-arrow">
                    <el-icon><ArrowRight /></el-icon>
                  </span>
                  <el-tag size="small" type="warning">{{ row.datasetName }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                  {{ row.status === 'active' ? '生效中' : '未激活' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status !== 'active'"
                  type="success"
                  link
                  :icon="Check"
                  @click="handleActivateBinding(row)"
                >
                  激活
                </el-button>
                <el-popconfirm
                  title="确定删除该绑定？"
                  @confirm="handleDeleteBinding(row.id)"
                >
                  <template #reference>
                    <el-button type="danger" link :icon="Delete">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="bindingPagination.page"
              v-model:page-size="bindingPagination.pageSize"
              :total="bindingPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="(v: number) => { bindingPagination.page = v; fetchBindings() }"
              @size-change="(v: number) => { bindingPagination.pageSize = v; bindingPagination.page = 1; fetchBindings() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Create Binding Dialog -->
    <el-dialog
      v-model="bindingDialogVisible"
      title="创建三元组绑定"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="bindingFormRef" :model="bindingForm" :rules="bindingRules" label-width="110px">
        <el-form-item label="绑定名称" prop="bindingName">
          <el-input v-model="bindingForm.bindingName" placeholder="请输入绑定名称" />
        </el-form-item>
        <el-form-item label="基础模型" prop="baseModelId">
          <el-select v-model="bindingForm.baseModelId" placeholder="选择基础模型" style="width: 100%">
            <el-option
              v-for="m in baseModelSelectOptions"
              :key="m.id"
              :label="`${m.name} (${m.version})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="LoRA 适配器" prop="loraAdapterId">
          <el-select v-model="bindingForm.loraAdapterId" placeholder="选择 LoRA 适配器" style="width: 100%">
            <el-option
              v-for="m in loraSelectOptions"
              :key="m.id"
              :label="`${m.name} (${m.version})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集" prop="datasetId">
          <el-select v-model="bindingForm.datasetId" placeholder="选择数据集" style="width: 100%">
            <el-option
              v-for="m in datasetSelectOptions"
              :key="m.id"
              :label="`${m.name} (${m.version})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>

        <!-- Visual triad preview -->
        <el-form-item label="绑定预览">
          <div class="triad-preview">
            <div class="triad-preview-node triad-base">
              <span class="triad-preview-label">基础模型</span>
              <span class="triad-preview-value">{{ getSelectedBaseModelName() || '-' }}</span>
            </div>
            <span class="triad-preview-arrow">
              <el-icon :size="20"><ArrowRight /></el-icon>
            </span>
            <div class="triad-preview-node triad-lora">
              <span class="triad-preview-label">LoRA 适配器</span>
              <span class="triad-preview-value">{{ getSelectedLoraName() || '-' }}</span>
            </div>
            <span class="triad-preview-arrow">
              <el-icon :size="20"><ArrowRight /></el-icon>
            </span>
            <div class="triad-preview-node triad-dataset">
              <span class="triad-preview-label">数据集</span>
              <span class="triad-preview-value">{{ getSelectedDatasetName() || '-' }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateBinding">确定创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Check, Delete, ArrowRight } from '@element-plus/icons-vue'
import type { BaseModel, ModelBinding } from '@/api/model'
import {
  getBaseModels,
  createModelBinding,
  getModelBindings,
  deleteModelBinding,
  setActiveBinding,
} from '@/api/model'

// ---------- Helpers ----------
function assetStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    active: 'success',
    inactive: 'info',
    training: 'warning'
  }
  return map[status] || 'info'
}

function assetStatusLabel(status: string) {
  const map: Record<string, string> = {
    active: '活跃',
    inactive: '未激活',
    training: '训练中'
  }
  return map[status] || status
}

// ---------- Tab State ----------
const activeTab = ref('base')

function onTabChange(tab: string) {
  switch (tab) {
    case 'base': fetchBase(); break
    case 'lora': fetchLora(); break
    case 'dataset': fetchDataset(); break
    case 'bindings': fetchBindings(); break
  }
}

// ───── Real API Helpers ─────

async function apiFetchModels(type: string, pagination: { page: number; pageSize: number }) {
  try {
    const res = await getBaseModels({ page: pagination.page, pageSize: pagination.pageSize, type })
    const result = res?.data || res || {}
    return {
      records: (result.records || result.list || result.data || []) as BaseModel[],
      total: result.total || 0,
    }
  } catch (e) {
    console.error(`Failed to load ${type} models:`, e)
    return { records: [], total: 0 }
  }
}

// ---------- Tab 1: Base Models ----------
const baseLoading = ref(false)
const baseModelData = ref<BaseModel[]>([])
const basePagination = reactive({ page: 1, pageSize: 10, total: 0 })

async function fetchBase() {
  baseLoading.value = true
  const { records, total } = await apiFetchModels('base', basePagination)
  baseModelData.value = records
  basePagination.total = total
  baseLoading.value = false
}

// ---------- Tab 2: LoRA ----------
const loraLoading = ref(false)
const loraData = ref<BaseModel[]>([])
const loraPagination = reactive({ page: 1, pageSize: 10, total: 0 })

async function fetchLora() {
  loraLoading.value = true
  const { records, total } = await apiFetchModels('lora', loraPagination)
  loraData.value = records
  loraPagination.total = total
  loraLoading.value = false
}

// ---------- Tab 3: Dataset ----------
const datasetLoading = ref(false)
const datasetData = ref<BaseModel[]>([])
const datasetPagination = reactive({ page: 1, pageSize: 10, total: 0 })

async function fetchDataset() {
  datasetLoading.value = true
  const { records, total } = await apiFetchModels('dataset', datasetPagination)
  datasetData.value = records
  datasetPagination.total = total
  datasetLoading.value = false
}

// ---------- Tab 4: Bindings ----------
const bindingLoading = ref(false)
const bindingData = ref<ModelBinding[]>([])
const bindingPagination = reactive({ page: 1, pageSize: 10, total: 0 })

async function fetchBindings() {
  bindingLoading.value = true
  try {
    const res = await getModelBindings({ page: bindingPagination.page, pageSize: bindingPagination.pageSize })
    const result = res?.data || res || {}
    bindingData.value = (result.records || result.list || result.data || []) as ModelBinding[]
    bindingPagination.total = result.total || 0
  } catch (e) {
    console.error('Failed to load bindings:', e)
    bindingData.value = []
    bindingPagination.total = 0
  } finally {
    bindingLoading.value = false
  }
}

// ---------- Binding Actions ----------
async function handleActivateBinding(row: ModelBinding) {
  try {
    await ElMessageBox.confirm(
      `确定要激活绑定 "${row.bindingName}" 吗？`,
      '激活绑定',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await setActiveBinding(row.id)
    ElMessage.success(`绑定 "${row.bindingName}" 已激活`)
    fetchBindings()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('激活失败: ' + (e?.message || '未知错误'))
  }
}

async function handleDeleteBinding(id: number) {
  try {
    await deleteModelBinding(id)
    ElMessage.success('绑定已删除')
    fetchBindings()
  } catch (e: any) {
    ElMessage.error('删除失败: ' + (e?.message || '未知错误'))
  }
}

// ---------- Create Binding Dialog ----------
const bindingDialogVisible = ref(false)
const bindingFormRef = ref<FormInstance>()

const bindingForm = reactive({
  bindingName: '',
  baseModelId: null as number | null,
  loraAdapterId: null as number | null,
  datasetId: null as number | null
})

const bindingRules: FormRules = {
  bindingName: [{ required: true, message: '请输入绑定名称', trigger: 'blur' }],
  baseModelId: [{ required: true, message: '请选择基础模型', trigger: 'change' }],
  loraAdapterId: [{ required: true, message: '请选择LoRA适配器', trigger: 'change' }],
  datasetId: [{ required: true, message: '请选择数据集', trigger: 'change' }]
}

// Select options loaded from API
const baseModelSelectOptions = ref<BaseModel[]>([])
const loraSelectOptions = ref<BaseModel[]>([])
const datasetSelectOptions = ref<BaseModel[]>([])

async function loadSelectOptions() {
  const [baseRes, loraRes, datasetRes] = await Promise.all([
    getBaseModels({ page: 1, pageSize: 100, type: 'base' }),
    getBaseModels({ page: 1, pageSize: 100, type: 'lora' }),
    getBaseModels({ page: 1, pageSize: 100, type: 'dataset' }),
  ])
  const extract = (res: any) => res?.data?.records || res?.data?.list || res?.records || res?.list || []
  baseModelSelectOptions.value = extract(baseRes)
  loraSelectOptions.value = extract(loraRes)
  datasetSelectOptions.value = extract(datasetRes)
}

function getSelectedBaseModelName() {
  const m = baseModelSelectOptions.value.find(m => m.id === bindingForm.baseModelId)
  return m ? `${m.name} (${m.version})` : ''
}

function getSelectedLoraName() {
  const m = loraSelectOptions.value.find(m => m.id === bindingForm.loraAdapterId)
  return m ? `${m.name} (${m.version})` : ''
}

function getSelectedDatasetName() {
  const m = datasetSelectOptions.value.find(m => m.id === bindingForm.datasetId)
  return m ? `${m.name} (${m.version})` : ''
}

function openBindingDialog() {
  bindingForm.bindingName = ''
  bindingForm.baseModelId = null
  bindingForm.loraAdapterId = null
  bindingForm.datasetId = null
  loadSelectOptions()
  bindingDialogVisible.value = true
}

async function handleCreateBinding() {
  if (!bindingFormRef.value) return
  const valid = await bindingFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await createModelBinding({
      baseModelId: bindingForm.baseModelId!,
      loraAdapterId: bindingForm.loraAdapterId!,
      datasetId: bindingForm.datasetId!,
      bindingName: bindingForm.bindingName,
    })
    ElMessage.success('三元组绑定创建成功')
    bindingDialogVisible.value = false
    if (activeTab.value === 'bindings') {
      fetchBindings()
    }
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e?.message || '未知错误'))
  }
}

// ---------- Lifecycle ----------
onMounted(() => {
  fetchBase()
  fetchLora()
  fetchDataset()
  fetchBindings()
})
</script>

<style scoped lang="scss">
.model-assets {
  .page-header {
    margin-bottom: 16px;
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }

  .table-toolbar {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .metric-value {
    font-weight: 600;
    color: #409eff;
  }

  .rank-value {
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 13px;
    color: #606266;
  }

  .text-secondary {
    color: #909399;
  }

  // Triad row in table
  .triad-row {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: nowrap;

    .triad-arrow {
      color: #c0c4cc;
      display: flex;
      align-items: center;
      flex-shrink: 0;
    }
  }

  // Triad preview in dialog
  .triad-preview {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    width: 100%;
    overflow-x: auto;
  }

  .triad-preview-node {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 10px 16px;
    border-radius: 8px;
    min-width: 100px;

    &.triad-base {
      background: #ecf5ff;
      border: 1px solid #b3d8ff;
    }

    &.triad-lora {
      background: #f0f9eb;
      border: 1px solid #c2e7b0;
    }

    &.triad-dataset {
      background: #fdf6ec;
      border: 1px solid #f5dab1;
    }

    .triad-preview-label {
      font-size: 11px;
      color: #909399;
      white-space: nowrap;
    }

    .triad-preview-value {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
      white-space: nowrap;
    }
  }

  .triad-preview-arrow {
    color: #c0c4cc;
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }
}
</style>

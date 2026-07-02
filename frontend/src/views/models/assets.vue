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

// ---------- Mock Data ----------
const allMockModels: BaseModel[] = [
  // Base Models (type: 'base')
  { id: 1, name: 'Qwen2-7B-Instruct', version: 'v2.1.0', type: 'base', description: '', size: '7B', status: 'active', framework: 'PyTorch', metrics: { accuracy: 87.5, f1: 86.2, inferenceTime: 45 }, createTime: '2024-08-15 10:00:00' },
  { id: 2, name: 'Qwen2-14B-Instruct', version: 'v2.1.0', type: 'base', description: '', size: '14B', status: 'active', framework: 'PyTorch', metrics: { accuracy: 91.3, f1: 90.1, inferenceTime: 78 }, createTime: '2024-08-15 10:30:00' },
  { id: 3, name: 'Llama3-8B-Instruct', version: 'v1.0.0', type: 'base', description: '', size: '8B', status: 'active', framework: 'Transformers', metrics: { accuracy: 86.8, f1: 85.4, inferenceTime: 52 }, createTime: '2024-09-01 08:00:00' },
  { id: 4, name: 'ChatGLM3-6B', version: 'v3.0.0', type: 'base', description: '', size: '6B', status: 'inactive', framework: 'PyTorch', metrics: { accuracy: 83.2, f1: 82.0, inferenceTime: 38 }, createTime: '2024-07-20 16:00:00' },
  { id: 5, name: 'Baichuan2-13B-Chat', version: 'v2.0.0', type: 'base', description: '', size: '13B', status: 'active', framework: 'Transformers', metrics: { accuracy: 89.1, f1: 88.3, inferenceTime: 65 }, createTime: '2024-10-05 14:00:00' },
  { id: 6, name: 'DeepSeek-V2-Lite', version: 'v1.0.0', type: 'base', description: '', size: '16B', status: 'training', framework: 'PyTorch', metrics: { accuracy: 85.0, f1: 83.5, inferenceTime: 55 }, createTime: '2024-11-01 09:00:00' },

  // LoRA Adapters (type: 'lora')
  { id: 11, name: 'contract-layout-lora', version: 'v1.2.0', type: 'lora', description: 'Qwen2-7B', size: '24MB', status: 'active', framework: '16', metrics: undefined, createTime: '2024-09-10 11:00:00' },
  { id: 12, name: 'contract-field-lora', version: 'v1.3.0', type: 'lora', description: 'Qwen2-14B', size: '48MB', status: 'active', framework: '32', metrics: undefined, createTime: '2024-09-12 14:00:00' },
  { id: 13, name: 'invoice-ocr-lora', version: 'v1.0.0', type: 'lora', description: 'Llama3-8B', size: '16MB', status: 'active', framework: '16', metrics: undefined, createTime: '2024-09-20 09:30:00' },
  { id: 14, name: 'bol-table-lora', version: 'v1.1.0', type: 'lora', description: 'Qwen2-7B', size: '22MB', status: 'inactive', framework: '16', metrics: undefined, createTime: '2024-10-02 15:00:00' },
  { id: 15, name: 'comprehensive-lora', version: 'v2.0.0', type: 'lora', description: 'Baichuan2-13B', size: '60MB', status: 'active', framework: '32', metrics: undefined, createTime: '2024-11-10 10:00:00' },

  // Datasets (type: 'dataset')
  { id: 21, name: '合同文档数据集', version: 'v2.1', type: 'dataset', description: '5,200 条', size: '合同', status: 'active', framework: undefined, metrics: undefined, createTime: '2024-08-20 10:00:00' },
  { id: 22, name: '发票数据集', version: 'v1.3', type: 'dataset', description: '3,800 条', size: '发票', status: 'active', framework: undefined, metrics: undefined, createTime: '2024-08-25 11:00:00' },
  { id: 23, name: '提单数据集', version: 'v1.0', type: 'dataset', description: '2,100 条', size: '提单', status: 'active', framework: undefined, metrics: undefined, createTime: '2024-09-05 08:30:00' },
  { id: 24, name: '综合文档数据集', version: 'v3.0', type: 'dataset', description: '12,000 条', size: '综合', status: 'active', framework: undefined, metrics: undefined, createTime: '2024-10-15 13:00:00' },
  { id: 25, name: '表格提取专项集', version: 'v1.0', type: 'dataset', description: '1,500 条', size: '综合', status: 'inactive', framework: undefined, metrics: undefined, createTime: '2024-11-01 16:00:00' }
]

// Bindings
let bindingIdCounter = 10
const allBindings: ModelBinding[] = [
  { id: 1, baseModelId: 1, baseModelName: 'Qwen2-7B-Instruct', loraAdapterId: 11, loraAdapterName: 'contract-layout-lora', datasetId: 21, datasetName: '合同文档数据集', bindingName: '合同版面分析组合', status: 'active', createTime: '2024-10-01 10:00:00' },
  { id: 2, baseModelId: 2, baseModelName: 'Qwen2-14B-Instruct', loraAdapterId: 12, loraAdapterName: 'contract-field-lora', datasetId: 21, datasetName: '合同文档数据集', bindingName: '合同字段提取组合', status: 'active', createTime: '2024-10-03 10:00:00' },
  { id: 3, baseModelId: 3, baseModelName: 'Llama3-8B-Instruct', loraAdapterId: 13, loraAdapterName: 'invoice-ocr-lora', datasetId: 22, datasetName: '发票数据集', bindingName: '发票OCR识别组合', status: 'active', createTime: '2024-10-05 09:30:00' },
  { id: 4, baseModelId: 1, baseModelName: 'Qwen2-7B-Instruct', loraAdapterId: 14, loraAdapterName: 'bol-table-lora', datasetId: 23, datasetName: '提单数据集', bindingName: '提单表格提取组合', status: 'inactive', createTime: '2024-10-08 14:00:00' },
  { id: 5, baseModelId: 5, baseModelName: 'Baichuan2-13B-Chat', loraAdapterId: 15, loraAdapterName: 'comprehensive-lora', datasetId: 24, datasetName: '综合文档数据集', bindingName: '综合分析组合', status: 'active', createTime: '2024-11-12 10:00:00' },
  { id: 6, baseModelId: 2, baseModelName: 'Qwen2-14B-Instruct', loraAdapterId: 15, loraAdapterName: 'comprehensive-lora', datasetId: 24, datasetName: '综合文档数据集', bindingName: '综合大模型组合', status: 'inactive', createTime: '2024-11-15 16:00:00' }
]

// ---------- Tab 1: Base Models ----------
const baseLoading = ref(false)
const baseModelData = ref<BaseModel[]>([])
const basePagination = reactive({ page: 1, pageSize: 10, total: 0 })

function fetchBase() {
  baseLoading.value = true
  setTimeout(() => {
    const filtered = allMockModels.filter(m => m.type === 'base')
    const start = (basePagination.page - 1) * basePagination.pageSize
    baseModelData.value = filtered.slice(start, start + basePagination.pageSize)
    basePagination.total = filtered.length
    baseLoading.value = false
  }, 200)
}

// ---------- Tab 2: LoRA ----------
const loraLoading = ref(false)
const loraData = ref<BaseModel[]>([])
const loraPagination = reactive({ page: 1, pageSize: 10, total: 0 })

function fetchLora() {
  loraLoading.value = true
  setTimeout(() => {
    const filtered = allMockModels.filter(m => m.type === 'lora')
    const start = (loraPagination.page - 1) * loraPagination.pageSize
    loraData.value = filtered.slice(start, start + loraPagination.pageSize)
    loraPagination.total = filtered.length
    loraLoading.value = false
  }, 200)
}

// ---------- Tab 3: Dataset ----------
const datasetLoading = ref(false)
const datasetData = ref<BaseModel[]>([])
const datasetPagination = reactive({ page: 1, pageSize: 10, total: 0 })

function fetchDataset() {
  datasetLoading.value = true
  setTimeout(() => {
    const filtered = allMockModels.filter(m => m.type === 'dataset')
    const start = (datasetPagination.page - 1) * datasetPagination.pageSize
    datasetData.value = filtered.slice(start, start + datasetPagination.pageSize)
    datasetPagination.total = filtered.length
    datasetLoading.value = false
  }, 200)
}

// ---------- Tab 4: Bindings ----------
const bindingLoading = ref(false)
const bindingData = ref<ModelBinding[]>([])
const bindingPagination = reactive({ page: 1, pageSize: 10, total: 0 })

function fetchBindings() {
  bindingLoading.value = true
  setTimeout(() => {
    const start = (bindingPagination.page - 1) * bindingPagination.pageSize
    bindingData.value = allBindings.slice(start, start + bindingPagination.pageSize)
    bindingPagination.total = allBindings.length
    bindingLoading.value = false
  }, 200)
}

// ---------- Binding Actions ----------
async function handleActivateBinding(row: ModelBinding) {
  await ElMessageBox.confirm(
    `确定要激活绑定 "${row.bindingName}" 吗？同一基础模型的旧绑定将被停用。`,
    '激活绑定',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
  )
  allBindings.forEach(b => {
    if (b.baseModelId === row.baseModelId && b.status === 'active') {
      b.status = 'inactive'
    }
  })
  row.status = 'active'
  ElMessage.success(`绑定 "${row.bindingName}" 已激活`)
  fetchBindings()
}

async function handleDeleteBinding(id: number) {
  const idx = allBindings.findIndex(b => b.id === id)
  if (idx >= 0) {
    const name = allBindings[idx].bindingName
    allBindings.splice(idx, 1)
    ElMessage.success(`绑定 "${name}" 已删除`)
    fetchBindings()
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

const baseModelSelectOptions = computed(() => allMockModels.filter(m => m.type === 'base'))
const loraSelectOptions = computed(() => allMockModels.filter(m => m.type === 'lora'))
const datasetSelectOptions = computed(() => allMockModels.filter(m => m.type === 'dataset'))

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
  bindingDialogVisible.value = true
}

async function handleCreateBinding() {
  if (!bindingFormRef.value) return
  const valid = await bindingFormRef.value.validate().catch(() => false)
  if (!valid) return

  const baseModel = baseModelSelectOptions.value.find(m => m.id === bindingForm.baseModelId)
  const loraModel = loraSelectOptions.value.find(m => m.id === bindingForm.loraAdapterId)
  const datasetModel = datasetSelectOptions.value.find(m => m.id === bindingForm.datasetId)

  const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

  const newBinding: ModelBinding = {
    id: bindingIdCounter++,
    baseModelId: bindingForm.baseModelId!,
    baseModelName: baseModel?.name || '未知',
    loraAdapterId: bindingForm.loraAdapterId!,
    loraAdapterName: loraModel?.name || '未知',
    datasetId: bindingForm.datasetId!,
    datasetName: datasetModel?.name || '未知',
    bindingName: bindingForm.bindingName,
    status: 'inactive',
    createTime: now
  }

  allBindings.unshift(newBinding)
  ElMessage.success('三元组绑定创建成功')
  bindingDialogVisible.value = false
  if (activeTab.value === 'bindings') {
    fetchBindings()
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

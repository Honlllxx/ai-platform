<template>
  <div class="page-container metadata-fields">
    <!-- Page header -->
    <div class="page-header">
      <h2>元数据字段库</h2>
    </div>

    <!-- Stats row -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">字段总数</div>
            </div>
            <el-icon :size="36" class="stat-icon" color="#409eff"><Collection /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.contractCount }}</div>
              <div class="stat-label">合同字段</div>
            </div>
            <el-icon :size="36" class="stat-icon" color="#67c23a"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.invoiceCount }}</div>
              <div class="stat-label">发票字段</div>
            </div>
            <el-icon :size="36" class="stat-icon" color="#e6a23c"><Tickets /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.billCount }}</div>
              <div class="stat-label">提单字段</div>
            </div>
            <el-icon :size="36" class="stat-icon" color="#f56c6c"><Ship /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filter bar -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="文档类型">
          <el-select v-model="query.groupCode" placeholder="全部" clearable style="width: 160px;">
            <el-option label="全部" value="" />
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="字段名/编码" clearable style="width: 220px;" @keyup.enter="handleSearch()" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch()">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增字段</el-button>
      </div>
      <el-table :data="data" v-loading="loading" stripe border style="width: 100%;">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="字段名" min-width="130" />
        <el-table-column prop="code" label="编码" min-width="130" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTagType(row.type)">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属文档" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="groupTagType(row.groupCode)">
              {{ row.groupName || groupLabel(row.groupCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="length" label="长度" width="80" align="center" />
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'" size="small">
              {{ row.required ? '必填' : '选填' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-popconfirm title="确定删除该字段？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑字段' : '新增字段'"
      width="580px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="字段名" prop="name">
          <el-input v-model="form.name" placeholder="请输入字段名" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="请输入字段编码（英文）" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择字段类型" style="width: 100%;">
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="日期" value="date" />
            <el-option label="枚举" value="enum" />
            <el-option label="布尔" value="boolean" />
          </el-select>
        </el-form-item>
        <el-form-item label="长度" prop="length" v-if="form.type === 'string'">
          <el-input-number v-model="form.length" :min="1" :max="5000" placeholder="最大长度" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="枚举值" prop="enumValues" v-if="form.type === 'enum'">
          <el-input v-model="form.enumValues" placeholder="逗号分隔，如: 选项A,选项B,选项C" />
        </el-form-item>
        <el-form-item label="校验规则" prop="validationRule">
          <el-input v-model="form.validationRule" placeholder="正则或规则描述（可选）" />
        </el-form-item>
        <el-form-item label="文档类型" prop="groupCode">
          <el-select v-model="form.groupCode" placeholder="请选择文档类型" style="width: 100%;">
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必填" prop="required">
          <el-switch v-model="form.required" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="字段描述（可选）" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  Search, Refresh, Plus, Edit, Delete,
  Collection, Document, Tickets, Ship
} from '@element-plus/icons-vue'
import {
  getMetadataFields, createMetadataField,
  updateMetadataField, deleteMetadataField,
  type MetadataField
} from '@/api/metadata'
import { useTable } from '@/composables/useTable'

const {
  data,
  loading,
  pagination,
  query,
  handleSearch,
  handleReset,
  handlePageChange,
  handleSizeChange,
  fetchData
} = useTable<MetadataField>({
  fetchApi: getMetadataFields,
  defaultQuery: { groupCode: '', keyword: '' }
})

const stats = reactive({
  total: 0,
  contractCount: 0,
  invoiceCount: 0,
  billCount: 0
})

function updateStats() {
  // In real app, fetched from API. Here computed from mock/data
  stats.total = pagination.total
  stats.contractCount = data.value.filter(d => d.groupCode === 'contract').length
  stats.invoiceCount = data.value.filter(d => d.groupCode === 'invoice').length
  stats.billCount = data.value.filter(d => d.groupCode === 'bill_of_lading').length
}

function typeTagType(type: string) {
  const map: Record<string, string> = {
    string: '',
    number: 'warning',
    date: 'success',
    enum: 'primary',
    boolean: 'info'
  }
  return map[type] || ''
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    string: '字符串',
    number: '数字',
    date: '日期',
    enum: '枚举',
    boolean: '布尔'
  }
  return map[type] || type
}

function groupTagType(code: string) {
  const map: Record<string, string> = {
    contract: 'success',
    invoice: 'warning',
    bill_of_lading: 'danger'
  }
  return map[code] || ''
}

function groupLabel(code: string) {
  const map: Record<string, string> = {
    contract: '合同',
    invoice: '发票',
    bill_of_lading: '提单'
  }
  return map[code] || code
}

// Form
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number>(0)

const form = reactive({
  name: '',
  code: '',
  type: 'string' as MetadataField['type'],
  length: undefined as number | undefined,
  enumValues: '',
  validationRule: '',
  groupCode: 'contract' as string,
  required: false,
  description: '',
  sort: 0
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入字段编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '编码必须是英文标识符格式', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择字段类型', trigger: 'change' }],
  groupCode: [{ required: true, message: '请选择文档类型', trigger: 'change' }]
}

function resetForm() {
  form.name = ''
  form.code = ''
  form.type = 'string'
  form.length = undefined
  form.enumValues = ''
  form.validationRule = ''
  form.groupCode = 'contract'
  form.required = false
  form.description = ''
  form.sort = 0
  editingId.value = 0
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: MetadataField) {
  isEdit.value = true
  resetForm()
  editingId.value = row.id
  form.name = row.name
  form.code = row.code
  form.type = row.type
  form.length = row.length
  form.enumValues = row.enumValues || ''
  form.validationRule = row.validationRule || ''
  form.groupCode = row.groupCode
  form.required = row.required
  form.description = row.description || ''
  form.sort = row.sort
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateMetadataField(editingId.value, form)
      ElMessage.success('字段更新成功')
    } else {
      await createMetadataField(form)
      ElMessage.success('字段创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteMetadataField(id)
    ElMessage.success('字段已删除')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

// Mock data generation for demo
function generateMockData(): MetadataField[] {
  const mockFields: MetadataField[] = [
    { id: 1, name: '合同编号', code: 'contract_no', type: 'string', length: 50, groupCode: 'contract', groupName: '合同', required: true, sort: 1, status: 1, createTime: '2026-06-20 10:00:00' },
    { id: 2, name: '合同金额', code: 'contract_amount', type: 'number', groupCode: 'contract', groupName: '合同', required: true, sort: 2, status: 1, createTime: '2026-06-20 10:00:00' },
    { id: 3, name: '签署日期', code: 'sign_date', type: 'date', groupCode: 'contract', groupName: '合同', required: true, sort: 3, status: 1, createTime: '2026-06-20 10:00:00' },
    { id: 4, name: '甲方名称', code: 'party_a', type: 'string', length: 100, groupCode: 'contract', groupName: '合同', required: true, sort: 4, status: 1, createTime: '2026-06-20 10:00:00' },
    { id: 5, name: '乙方名称', code: 'party_b', type: 'string', length: 100, groupCode: 'contract', groupName: '合同', required: true, sort: 5, status: 1, createTime: '2026-06-20 10:00:00' },
    { id: 6, name: '发票号码', code: 'invoice_no', type: 'string', length: 20, groupCode: 'invoice', groupName: '发票', required: true, sort: 1, status: 1, createTime: '2026-06-21 09:00:00' },
    { id: 7, name: '开票日期', code: 'invoice_date', type: 'date', groupCode: 'invoice', groupName: '发票', required: true, sort: 2, status: 1, createTime: '2026-06-21 09:00:00' },
    { id: 8, name: '价税合计', code: 'total_amount', type: 'number', groupCode: 'invoice', groupName: '发票', required: true, sort: 3, status: 1, createTime: '2026-06-21 09:00:00' },
    { id: 9, name: '销售方名称', code: 'seller_name', type: 'string', length: 100, groupCode: 'invoice', groupName: '发票', required: true, sort: 4, status: 1, createTime: '2026-06-21 09:00:00' },
    { id: 10, name: '提单号', code: 'bl_no', type: 'string', length: 30, groupCode: 'bill_of_lading', groupName: '提单', required: true, sort: 1, status: 1, createTime: '2026-06-22 14:00:00' },
    { id: 11, name: '船名', code: 'vessel_name', type: 'string', length: 100, groupCode: 'bill_of_lading', groupName: '提单', required: true, sort: 2, status: 1, createTime: '2026-06-22 14:00:00' },
    { id: 12, name: '起运港', code: 'port_of_loading', type: 'string', length: 100, groupCode: 'bill_of_lading', groupName: '提单', required: true, sort: 3, status: 1, createTime: '2026-06-22 14:00:00' },
    { id: 13, name: '目的港', code: 'port_of_discharge', type: 'string', length: 100, groupCode: 'bill_of_lading', groupName: '提单', required: false, sort: 4, status: 1, createTime: '2026-06-22 14:00:00' },
    { id: 14, name: '是否已签', code: 'is_signed', type: 'boolean', groupCode: 'contract', groupName: '合同', required: false, sort: 6, status: 1, createTime: '2026-06-23 11:00:00' },
    { id: 15, name: '税务类型', code: 'tax_type', type: 'enum', enumValues: '增值税,营业税,所得税', groupCode: 'invoice', groupName: '发票', required: false, sort: 5, status: 1, createTime: '2026-06-23 11:00:00' },
  ]
  return mockFields
}

onMounted(() => {
  // Simulate loading mock data for demo
  if (import.meta.env.DEV) {
    const mockData = generateMockData()
    data.value = mockData
    pagination.total = mockData.length
    updateStats()
  } else {
    fetchData()
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.metadata-fields {
  .page-header {
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
    }
  }

  .stats-row {
    margin-bottom: 16px;
  }

  .stat-card {
    :deep(.el-card__body) {
      padding: 16px 20px;
    }
  }

  .stat-content {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: $text-primary;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }

    .stat-icon {
      opacity: 0.65;
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
}
</style>

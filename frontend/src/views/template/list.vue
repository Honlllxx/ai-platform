<template>
  <div class="page-container template-list">
    <!-- Page header -->
    <div class="page-header">
      <h2>可视化模板配置</h2>
    </div>

    <!-- Filter bar -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="文档类型">
          <el-select v-model="query.documentType" placeholder="全部" clearable style="width: 160px;">
            <el-option label="全部" value="" />
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="模板名称" clearable style="width: 220px;" @keyup.enter="handleSearch()" />
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
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增模板</el-button>
      </div>
      <el-table :data="data" v-loading="loading" stripe border style="width: 100%;">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column label="文档类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="docTypeTagType(row.documentType)">
              {{ docTypeLabel(row.documentType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="区域数" width="80" align="center">
          <template #default="{ row }">
            {{ regionCount(row.regions) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link :icon="CopyDocument" @click="handleCopy(row)">复制</el-button>
            <el-popconfirm title="确定删除该模板？" @confirm="handleDelete(row.id)">
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

    <!-- Create Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="新增模板"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="文档类型" prop="documentType">
          <el-select v-model="form.documentType" placeholder="请选择文档类型" style="width: 100%;">
            <el-option label="合同" value="contract" />
            <el-option label="发票" value="invoice" />
            <el-option label="提单" value="bill_of_lading" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="模板描述（可选）" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, CopyDocument } from '@element-plus/icons-vue'
import { getTemplates, createTemplate, deleteTemplate, type TemplateItem } from '@/api/template'
import { useTable } from '@/composables/useTable'

const router = useRouter()

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
} = useTable<TemplateItem>({
  fetchApi: getTemplates,
  defaultQuery: { documentType: '', keyword: '' }
})

function regionCount(regions: any): number {
  if (!regions) return 0
  if (Array.isArray(regions)) return regions.length
  if (typeof regions === 'string') {
    try { return JSON.parse(regions).length } catch { return 0 }
  }
  return 0
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

function handleEdit(row: TemplateItem) {
  router.push(`/templates/${row.id}/edit`)
}

async function handleCopy(row: TemplateItem) {
  try {
    await createTemplate({
      name: `${row.name} (副本)`,
      documentType: row.documentType,
      description: row.description,
      regions: row.regions ? JSON.stringify(row.regions) : ''
    })
    ElMessage.success('模板复制成功')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTemplate(id)
    ElMessage.success('模板已删除')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

// Create dialog
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  documentType: 'contract' as string,
  description: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  documentType: [{ required: true, message: '请选择文档类型', trigger: 'change' }]
}

function openCreateDialog() {
  form.name = ''
  form.documentType = 'contract'
  form.description = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await createTemplate(form)
    ElMessage.success('模板创建成功')
    dialogVisible.value = false
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// Mock data generation for demo
function generateMockData(): TemplateItem[] {
  return [
    {
      id: 1, name: '通用合同模板', documentType: 'contract',
      description: '适用于大多数合同类型的通用识别模板',
      regions: [
        { id: 'r1', x: 100, y: 50, width: 200, height: 30, fieldCode: 'contract_no', fieldName: '合同编号', type: 'text' },
        { id: 'r2', x: 400, y: 50, width: 150, height: 30, fieldCode: 'sign_date', fieldName: '签署日期', type: 'text' },
        { id: 'r3', x: 100, y: 120, width: 250, height: 30, fieldCode: 'party_a', fieldName: '甲方名称', type: 'text' },
        { id: 'r4', x: 100, y: 180, width: 250, height: 30, fieldCode: 'party_b', fieldName: '乙方名称', type: 'text' },
        { id: 'r5', x: 100, y: 250, width: 180, height: 30, fieldCode: 'contract_amount', fieldName: '合同金额', type: 'text' },
      ],
      status: 1, version: 3,
      createTime: '2026-06-15 09:00:00',
      updateTime: '2026-06-28 14:30:00'
    },
    {
      id: 2, name: '增值税发票模板', documentType: 'invoice',
      description: '增值税专用发票和普通发票识别模板',
      regions: [
        { id: 'r1', x: 640, y: 60, width: 120, height: 30, fieldCode: 'invoice_no', fieldName: '发票号码', type: 'text' },
        { id: 'r2', x: 640, y: 100, width: 120, height: 30, fieldCode: 'invoice_date', fieldName: '开票日期', type: 'text' },
        { id: 'r3', x: 100, y: 340, width: 200, height: 30, fieldCode: 'total_amount', fieldName: '价税合计', type: 'text' },
        { id: 'r4', x: 100, y: 80, width: 300, height: 30, fieldCode: 'seller_name', fieldName: '销售方名称', type: 'text' },
      ],
      status: 1, version: 2,
      createTime: '2026-06-18 10:30:00',
      updateTime: '2026-06-27 11:15:00'
    },
    {
      id: 3, name: '海运提单模板', documentType: 'bill_of_lading',
      description: '标准海运提单（B/L）识别模板',
      regions: [
        { id: 'r1', x: 480, y: 80, width: 150, height: 30, fieldCode: 'bl_no', fieldName: '提单号', type: 'text' },
        { id: 'r2', x: 100, y: 160, width: 200, height: 30, fieldCode: 'vessel_name', fieldName: '船名', type: 'text' },
        { id: 'r3', x: 100, y: 220, width: 200, height: 30, fieldCode: 'port_of_loading', fieldName: '起运港', type: 'text' },
        { id: 'r4', x: 400, y: 220, width: 200, height: 30, fieldCode: 'port_of_discharge', fieldName: '目的港', type: 'text' },
      ],
      status: 1, version: 1,
      createTime: '2026-06-22 16:00:00',
      updateTime: '2026-06-25 09:45:00'
    },
    {
      id: 4, name: '采购合同模板', documentType: 'contract',
      description: '采购订单合同专用模板',
      regions: [],
      status: 0, version: 1,
      createTime: '2026-06-20 14:00:00',
      updateTime: '2026-06-20 14:00:00'
    },
  ]
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.template-list {
  .page-header {
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
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

<template>
  <div class="page-container permission-management">
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" v-permission="'permission:create'" @click="openCreateDialog(null)">
          新增权限
        </el-button>
        <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
      </div>

      <!-- Tree Table -->
      <el-table
        :data="treeData"
        v-loading="loading"
        stripe
        border
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="name" label="权限名称" min-width="180" />
        <el-table-column prop="code" label="权限编码" min-width="180" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160">
          <template #default="{ row }">
            {{ row.path || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template #default="{ row }">
            {{ row.icon || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" v-permission="'permission:update'" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="success" link :icon="Plus" v-if="row.type === 'menu'" v-permission="'permission:create'" @click="openCreateDialog(row)">
              添加子项
            </el-button>
            <el-popconfirm title="确定删除该权限？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete" v-permission="'permission:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑权限' : '新增权限'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="父级权限">
          <el-input :model-value="parentName" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="请输入权限编码" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%;">
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="API" value="api" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由路径" v-if="form.type === 'menu'">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="图标" v-if="form.type === 'menu'">
          <el-input v-model="form.icon" placeholder="请输入 Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getPermissionTree, createPermission, updatePermission, deletePermission, type PermissionInfo } from '@/api/permission'

const loading = ref(false)
const treeData = ref<PermissionInfo[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    treeData.value = (res?.data || res || []) as PermissionInfo[]
  } catch {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function typeTag(type: string) {
  const map: Record<string, string> = { menu: '', button: 'warning', api: 'info' }
  return map[type] || ''
}

function typeLabel(type: string) {
  const map: Record<string, string> = { menu: '菜单', button: '按钮', api: 'API' }
  return map[type] || type
}

// Form
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const parentId = ref<number | null>(null)
const parentName = ref<string>('根目录')

const form = reactive<Partial<PermissionInfo>>({
  name: '',
  code: '',
  type: 'menu',
  parentId: null,
  path: '',
  icon: '',
  sort: 0,
  status: 1
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function resetForm() {
  form.name = ''
  form.code = ''
  form.type = 'menu'
  form.parentId = parentId.value
  form.path = ''
  form.icon = ''
  form.sort = 0
  form.status = 1
}

function openCreateDialog(parent: PermissionInfo | null) {
  isEdit.value = false
  parentId.value = parent?.id ?? null
  parentName.value = parent ? parent.name : '根目录'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: PermissionInfo) {
  isEdit.value = true
  parentId.value = row.parentId
  parentName.value = row.parentId ? '父级权限' : '根目录'
  resetForm()
  form.name = row.name
  form.code = row.code
  form.type = row.type
  form.parentId = row.parentId
  form.path = row.path
  form.icon = row.icon
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updatePermission(form)
      ElMessage.success('更新成功')
    } else {
      await createPermission(form)
      ElMessage.success('创建成功')
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
    await deletePermission(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.permission-management {
  .table-toolbar {
    margin-bottom: 16px;
  }
}
</style>

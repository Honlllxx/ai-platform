<template>
  <div class="page-container role-management">
    <!-- Search Form -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="角色名称">
          <el-input v-model="query.name" placeholder="输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="query.code" placeholder="输入角色编码" clearable />
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
        <el-button type="primary" :icon="Plus" v-permission="'role:create'" @click="openCreateDialog">
          新增角色
        </el-button>
      </div>
      <el-table :data="data" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="120" />
        <el-table-column prop="code" label="角色编码" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="80" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" v-permission="'role:update'" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="warning" link :icon="Lock" v-permission="'role:assign-permission'" @click="openPermissionDialog(row)">
              权限
            </el-button>
            <el-popconfirm title="确定删除该角色？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete" v-permission="'role:delete'">删除</el-button>
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
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Permission Assignment Dialog -->
    <el-dialog
      v-model="permDialogVisible"
      title="分配权限"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="selectedPermIds"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handlePermSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Lock } from '@element-plus/icons-vue'
import { getRoles, createRole, updateRole, deleteRole, assignPermissions, type RoleInfo } from '@/api/role'
import { getPermissionTree } from '@/api/permission'
import { useTable } from '@/composables/useTable'
import type { ElTree } from 'element-plus'

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
} = useTable<RoleInfo>({
  fetchApi: getRoles,
  defaultQuery: { name: '', code: '' }
})

// Create/Edit
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number>(0)

const form = reactive({
  name: '',
  code: '',
  description: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

function resetForm() {
  form.name = ''
  form.code = ''
  form.description = ''
  currentId.value = 0
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: RoleInfo) {
  isEdit.value = true
  resetForm()
  currentId.value = row.id
  form.name = row.name
  form.code = row.code
  form.description = row.description || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateRole({ id: currentId.value, ...form })
      ElMessage.success('更新成功')
    } else {
      await createRole(form)
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
    await deleteRole(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

// Permission assignment
const permDialogVisible = ref(false)
const permSubmitLoading = ref(false)
const selectedPermIds = ref<number[]>([])
const currentRole = ref<RoleInfo | null>(null)
const permissionTree = ref<any[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()

async function openPermissionDialog(row: RoleInfo) {
  currentRole.value = row
  selectedPermIds.value = [...(row.permissionIds || [])]
  permDialogVisible.value = true
  if (permissionTree.value.length === 0) {
    try {
      const res = await getPermissionTree()
      permissionTree.value = res?.data || res || []
    } catch {
      permissionTree.value = []
    }
  }
}

async function handlePermSubmit() {
  if (!currentRole.value || !treeRef.value) return
  permSubmitLoading.value = true
  try {
    const checkedKeys = treeRef.value.getCheckedKeys() as number[]
    const halfCheckedKeys = treeRef.value.getHalfCheckedKeys() as number[]
    await assignPermissions(currentRole.value.id, [...checkedKeys, ...halfCheckedKeys])
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    permSubmitLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.role-management {
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

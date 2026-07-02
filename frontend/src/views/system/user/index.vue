<template>
  <div class="page-container user-management">
    <!-- Search Form -->
    <div class="search-form">
      <el-form :model="query" :inline="true">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="输入用户名搜索" clearable />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="query.email" placeholder="输入邮箱搜索" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
        <el-button type="primary" :icon="Plus" v-permission="'user:create'" @click="openCreateDialog">
          新增用户
        </el-button>
      </div>
      <el-table :data="data" v-loading="loading" stripe border style="width: 100%;">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag
              v-for="role in row.roleNames"
              :key="role"
              size="small"
              style="margin-right: 4px;"
            >
              {{ role }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="text-secondary">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" v-permission="'user:update'" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="warning" link :icon="SwitchButton" v-permission="'user:status'" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="info" link :icon="Avatar" v-permission="'user:assign-role'" @click="openRoleDialog(row)">
              角色
            </el-button>
            <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete" v-permission="'user:delete'">删除</el-button>
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
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="role in allRoles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Assign Role Dialog -->
    <el-dialog
      v-model="roleDialogVisible"
      title="分配角色"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ currentUser?.username }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="role in allRoles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitLoading" @click="handleRoleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Avatar, SwitchButton } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus, assignRoles, type UserInfo } from '@/api/user'
import { getAllRoles } from '@/api/role'
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
} = useTable<UserInfo>({
  fetchApi: getUsers,
  defaultQuery: { username: '', email: '', status: undefined }
})

// Form
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const currentUserId = ref<number>(0)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [] as number[]
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码不少于6位', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}

const allRoles = ref<Array<{ id: number; name: string }>>([])

function resetForm() {
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.roleIds = []
  currentUserId.value = 0
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: UserInfo) {
  isEdit.value = true
  resetForm()
  currentUserId.value = row.id
  form.username = row.username
  form.nickname = row.nickname || ''
  form.email = row.email || ''
  form.phone = row.phone || ''
  form.roleIds = row.roleIds || []
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser({ id: currentUserId.value, ...form })
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
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
    await deleteUser(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

async function handleToggleStatus(row: UserInfo) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, newStatus as 0 | 1)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // Error handled by interceptor
  }
}

// Role assignment
const roleDialogVisible = ref(false)
const roleSubmitLoading = ref(false)
const currentUser = ref<UserInfo | null>(null)
const selectedRoleIds = ref<number[]>([])

function openRoleDialog(row: UserInfo) {
  currentUser.value = row
  selectedRoleIds.value = [...(row.roleIds || [])]
  roleDialogVisible.value = true
}

async function handleRoleSubmit() {
  if (!currentUser.value) return
  roleSubmitLoading.value = true
  try {
    await assignRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    roleSubmitLoading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getAllRoles()
    allRoles.value = res?.data || res || []
  } catch {
    allRoles.value = []
  }
  fetchData()
})
</script>

<style scoped lang="scss">
.user-management {
  .table-toolbar {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .text-secondary {
    color: #909399;
  }
}
</style>

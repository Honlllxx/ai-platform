<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="isCollapse"
    :background-color="variables.sidebarBg"
    :text-color="variables.sidebarText"
    :active-text-color="variables.sidebarActiveText"
    router
    class="sidebar-menu"
  >
    <el-menu-item index="/dashboard">
      <el-icon><Odometer /></el-icon>
      <span>仪表盘</span>
    </el-menu-item>

    <el-sub-menu index="document" v-if="hasDocumentAccess">
      <template #title>
        <el-icon><Folder /></el-icon>
        <span>文档管理</span>
      </template>
      <el-menu-item index="/documents/upload" v-permission="'document:upload'">
        <el-icon><Upload /></el-icon>
        <span>文档上传</span>
      </el-menu-item>
      <el-menu-item index="/documents/list" v-permission="'document:list'">
        <el-icon><List /></el-icon>
        <span>文档列表</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="pipeline" v-if="hasPipelineAccess">
      <template #title>
        <el-icon><Connection /></el-icon>
        <span>处理流水线</span>
      </template>
      <el-menu-item index="/pipeline/list" v-permission="'pipeline:list'">
        <el-icon><SetUp /></el-icon>
        <span>流水线列表</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="business" v-if="hasBusinessAccess">
      <template #title>
        <el-icon><EditPen /></el-icon>
        <span>业务配置</span>
      </template>
      <el-menu-item index="/templates/list" v-permission="'template:list'">
        <el-icon><Crop /></el-icon>
        <span>模板配置</span>
      </el-menu-item>
      <el-sub-menu index="review" v-permission="'review:list'">
        <template #title>
          <el-icon><Checked /></el-icon>
          <span>审核工作台</span>
        </template>
        <el-menu-item index="/review/list">
          <el-icon><List /></el-icon>
          <span>审核列表</span>
        </el-menu-item>
        <el-menu-item index="/review/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>审核统计</span>
        </el-menu-item>
      </el-sub-menu>
    </el-sub-menu>

    <el-sub-menu index="metadata" v-if="hasMetadataAccess">
      <template #title>
        <el-icon><Coin /></el-icon>
        <span>元数据管理</span>
      </template>
      <el-menu-item index="/metadata/fields" v-permission="'metadata:list'">
        <el-icon><Grid /></el-icon>
        <span>字段库</span>
      </el-menu-item>
      <el-menu-item index="/metadata/export" v-permission="'metadata:export'">
        <el-icon><Download /></el-icon>
        <span>数据导出</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="prompt" v-if="hasPromptAccess">
      <template #title>
        <el-icon><Edit /></el-icon>
        <span>提示词工程</span>
      </template>
      <el-menu-item index="/prompt/versions" v-permission="'prompt:version'">
        <el-icon><CollectionTag /></el-icon>
        <span>版本管理</span>
      </el-menu-item>
      <el-menu-item index="/prompt/evaluation" v-permission="'prompt:evaluation'">
        <el-icon><TrendCharts /></el-icon>
        <span>自动化评测</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="model" v-if="hasModelAccess">
      <template #title>
        <el-icon><Cpu /></el-icon>
        <span>模型管理</span>
      </template>
      <el-menu-item index="/finetune/tasks" v-permission="'finetune:list'">
        <el-icon><Monitor /></el-icon>
        <span>微调任务</span>
      </el-menu-item>
      <el-menu-item index="/models/assets" v-permission="'model:list'">
        <el-icon><Box /></el-icon>
        <span>模型资产</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="system" v-if="hasSystemAccess">
      <template #title>
        <el-icon><Setting /></el-icon>
        <span>系统管理</span>
      </template>
      <el-menu-item index="/system/users" v-permission="'user:list'">
        <el-icon><User /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
      <el-menu-item index="/system/roles" v-permission="'role:list'">
        <el-icon><Avatar /></el-icon>
        <span>角色管理</span>
      </el-menu-item>
      <el-menu-item index="/system/permissions" v-permission="'permission:list'">
        <el-icon><Lock /></el-icon>
        <span>权限管理</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="log" v-if="hasLogAccess">
      <template #title>
        <el-icon><Document /></el-icon>
        <span>日志审计</span>
      </template>
      <el-menu-item index="/logs/operations" v-permission="'log:operation'">
        <el-icon><List /></el-icon>
        <span>操作日志</span>
      </el-menu-item>
      <el-menu-item index="/logs/agent-traces" v-permission="'log:agent'">
        <el-icon><Connection /></el-icon>
        <span>Agent 链路</span>
      </el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/settings" v-permission="'settings:view'">
      <el-icon><Tools /></el-icon>
      <span>系统设置</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

defineProps<{
  isCollapse: boolean
}>()

const activeMenu = computed(() => route.path)

const hasDocumentAccess = computed(() =>
  userStore.hasAnyPermission(['document:upload', 'document:list'])
)

const hasPipelineAccess = computed(() =>
  userStore.hasAnyPermission(['pipeline:list'])
)

const hasBusinessAccess = computed(() =>
  userStore.hasAnyPermission(['template:list', 'review:list'])
)

const hasMetadataAccess = computed(() =>
  userStore.hasAnyPermission(['metadata:list', 'metadata:export'])
)

const hasPromptAccess = computed(() =>
  userStore.hasAnyPermission(['prompt:version', 'prompt:evaluation'])
)

const hasModelAccess = computed(() =>
  userStore.hasAnyPermission(['finetune:list', 'model:list'])
)

const hasSystemAccess = computed(() =>
  userStore.hasAnyPermission(['user:list', 'role:list', 'permission:list'])
)

const hasLogAccess = computed(() =>
  userStore.hasAnyPermission(['log:operation', 'log:agent'])
)

const variables = {
  sidebarBg: '#304156',
  sidebarText: '#bfcbd9',
  sidebarActiveText: '#409eff'
}
</script>

<style scoped lang="scss">
.sidebar-menu {
  border-right: none;
  height: 100%;
  overflow-y: auto;

  &:not(.el-menu--collapse) {
    width: 220px;
  }
}
</style>

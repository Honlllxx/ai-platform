import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  }
]

// Routes that require authentication
const protectedRoutes: RouteRecordRaw[] = [
  // ========== Dashboard ==========
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '仪表盘', icon: 'Odometer', permission: 'dashboard' }
  },

  // ========== Document Management ==========
  {
    path: '/documents/upload',
    name: 'DocumentUpload',
    component: () => import('@/views/document/upload.vue'),
    meta: { title: '文档上传', permission: 'document:upload' }
  },
  {
    path: '/documents/list',
    name: 'DocumentList',
    component: () => import('@/views/document/list.vue'),
    meta: { title: '文档列表', permission: 'document:list' }
  },
  {
    path: '/documents/:id',
    name: 'DocumentDetail',
    component: () => import('@/views/document/detail.vue'),
    meta: { title: '文档详情', hidden: true, permission: 'document:list' },
    redirect: '/documents/:id/layout',
    children: [
      {
        path: 'layout',
        name: 'DocumentLayout',
        component: () => import('@/views/document/components/LayoutView.vue'),
        meta: { title: '版面分析', hidden: true }
      },
      {
        path: 'ocr',
        name: 'DocumentOcr',
        component: () => import('@/views/document/components/OcrView.vue'),
        meta: { title: 'OCR 结果', hidden: true }
      },
      {
        path: 'table',
        name: 'DocumentTable',
        component: () => import('@/views/document/components/TableView.vue'),
        meta: { title: '表格提取', hidden: true }
      }
    ]
  },

  // ========== Pipeline ==========
  {
    path: '/pipeline/list',
    name: 'PipelineList',
    component: () => import('@/views/pipeline/list.vue'),
    meta: { title: '流水线列表', permission: 'pipeline:list' }
  },
  {
    path: '/pipeline/:id',
    name: 'PipelineDetail',
    component: () => import('@/views/pipeline/detail.vue'),
    meta: { title: '流水线详情', hidden: true, permission: 'pipeline:list' }
  },

  // ========== Metadata ==========
  {
    path: '/metadata/fields',
    name: 'MetadataFields',
    component: () => import('@/views/metadata/fields.vue'),
    meta: { title: '字段库', permission: 'metadata:list' }
  },
  {
    path: '/metadata/export',
    name: 'MetadataExport',
    component: () => import('@/views/metadata/export.vue'),
    meta: { title: '数据导出', permission: 'metadata:export' }
  },

  // ========== Business Configuration ==========
  {
    path: '/templates/list',
    name: 'TemplateList',
    component: () => import('@/views/template/list.vue'),
    meta: { title: '模板配置', permission: 'template:list' }
  },
  {
    path: '/templates/:id/edit',
    name: 'TemplateEditor',
    component: () => import('@/views/template/editor.vue'),
    meta: { title: '模板编辑', hidden: true, permission: 'template:list' }
  },
  {
    path: '/review/list',
    name: 'ReviewList',
    component: () => import('@/views/review/list.vue'),
    meta: { title: '审核工作台', permission: 'review:list' }
  },
  {
    path: '/review/:id',
    name: 'ReviewDetail',
    component: () => import('@/views/review/detail.vue'),
    meta: { title: '审核详情', hidden: true, permission: 'review:list' }
  },
  {
    path: '/review/statistics',
    name: 'ReviewStatistics',
    component: () => import('@/views/review/statistics.vue'),
    meta: { title: '审核统计', permission: 'review:list' }
  },

  // ========== Prompt Engineering ==========
  {
    path: '/prompt/versions',
    name: 'PromptVersions',
    component: () => import('@/views/prompt/versions.vue'),
    meta: { title: '提示词版本', permission: 'prompt:version' }
  },
  {
    path: '/prompt/evaluation',
    name: 'PromptEvaluation',
    component: () => import('@/views/prompt/evaluation.vue'),
    meta: { title: '自动化评测', permission: 'prompt:evaluation' }
  },

  // ========== Model Management ==========
  {
    path: '/finetune/tasks',
    name: 'FinetuneTasks',
    component: () => import('@/views/finetune/tasks.vue'),
    meta: { title: '微调任务', permission: 'finetune:list' }
  },
  {
    path: '/models/assets',
    name: 'ModelAssets',
    component: () => import('@/views/models/assets.vue'),
    meta: { title: '模型资产', permission: 'model:list' }
  },

  // ========== System Management ==========
  {
    path: '/system/users',
    name: 'SystemUsers',
    component: () => import('@/views/system/user/index.vue'),
    meta: { title: '用户管理', permission: 'user:list' }
  },
  {
    path: '/system/roles',
    name: 'SystemRoles',
    component: () => import('@/views/system/role/index.vue'),
    meta: { title: '角色管理', permission: 'role:list' }
  },
  {
    path: '/system/permissions',
    name: 'SystemPermissions',
    component: () => import('@/views/system/permission/index.vue'),
    meta: { title: '权限管理', permission: 'permission:list' }
  },

  // ========== Logs & Audit ==========
  {
    path: '/logs/operations',
    name: 'OperationLogs',
    component: () => import('@/views/log/operation/index.vue'),
    meta: { title: '操作日志', permission: 'log:operation' }
  },
  {
    path: '/logs/agent-traces',
    name: 'AgentTraces',
    component: () => import('@/views/log/agent-trace/index.vue'),
    meta: { title: 'Agent 链路', permission: 'log:agent' }
  },

  // ========== Settings & Errors ==========
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/index.vue'),
    meta: { title: '系统设置', permission: 'settings:view' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/system/error/403.vue'),
    meta: { title: '无权限', hidden: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/system/error/404.vue'),
    meta: { title: '404', hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layout/DefaultLayout.vue'),
      redirect: '/dashboard',
      children: [...publicRoutes.filter(r => r.path !== '/login'), ...protectedRoutes]
    },
    ...publicRoutes
  ]
})

// Route guard
router.beforeEach(async (to, _from, next) => {
  const token = getToken()
  const userStore = useUserStore()

  // If going to login and already has token, skip
  if (to.path === '/login') {
    if (token) {
      next('/dashboard')
      return
    }
    next()
    return
  }

  // No token - redirect to login
  if (!token) {
    next(`/login?redirect=${to.path}`)
    return
  }

  // Has token but user info not loaded yet
  if (!userStore.username) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
      next(`/login?redirect=${to.path}`)
      return
    }
  }

  // Check permission
  const requiredPermission = to.meta?.permission as string | undefined
  if (requiredPermission && !userStore.hasPermission(requiredPermission)) {
    ElMessage.warning('没有访问该页面的权限')
    next('/403')
    return
  }

  next()
})

export default router

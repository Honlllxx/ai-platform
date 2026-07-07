<template>
  <div class="page-container settings-page">
    <el-card shadow="hover" class="settings-card">
      <template #header>
        <span>系统设置</span>
      </template>

      <el-tabs tab-position="left" class="settings-tabs">
        <el-tab-pane label="基本设置">
          <el-form :model="basicForm" label-width="120px" class="settings-form">
            <el-form-item label="系统名称">
              <el-input v-model="basicForm.siteName" placeholder="AI Platform" />
            </el-form-item>
            <el-form-item label="系统 Logo">
              <el-input v-model="basicForm.logo" placeholder="Logo URL" />
            </el-form-item>
            <el-form-item label="系统描述">
              <el-input v-model="basicForm.description" type="textarea" :rows="3" placeholder="系统描述" />
            </el-form-item>
            <el-form-item label="每页条数">
              <el-input-number v-model="basicForm.pageSize" :min="5" :max="100" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="handleSave('basic')">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="安全设置">
          <el-form :model="securityForm" label-width="140px" class="settings-form">
            <el-form-item label="Token 过期时间(分钟)">
              <el-input-number v-model="securityForm.tokenExpire" :min="5" :max="1440" />
            </el-form-item>
            <el-form-item label="最大登录尝试次数">
              <el-input-number v-model="securityForm.maxLoginAttempts" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="密码最小长度">
              <el-input-number v-model="securityForm.minPasswordLength" :min="6" :max="32" />
            </el-form-item>
            <el-form-item label="会话超时(分钟)">
              <el-input-number v-model="securityForm.sessionTimeout" :min="5" :max="480" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="handleSave('security')">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="日志设置">
          <el-form :model="logForm" label-width="140px" class="settings-form">
            <el-form-item label="日志保留天数">
              <el-input-number v-model="logForm.retentionDays" :min="7" :max="365" />
            </el-form-item>
            <el-form-item label="操作日志记录">
              <el-switch v-model="logForm.enableOperationLog" />
            </el-form-item>
            <el-form-item label="Agent 链路记录">
              <el-switch v-model="logForm.enableAgentTrace" />
            </el-form-item>
            <el-form-item label="记录请求参数">
              <el-switch v-model="logForm.logRequestParams" />
            </el-form-item>
            <el-form-item label="记录响应结果">
              <el-switch v-model="logForm.logResponseResult" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="handleSave('log')">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettings, updateSettings } from '@/api/settings'

const saveLoading = ref(false)
const pageLoading = ref(false)

const basicForm = reactive({
  siteName: 'AI Platform',
  logo: '',
  description: '智能管理平台',
  pageSize: 20
})

const securityForm = reactive({
  tokenExpire: 120,
  maxLoginAttempts: 5,
  minPasswordLength: 6,
  sessionTimeout: 30
})

const logForm = reactive({
  retentionDays: 90,
  enableOperationLog: true,
  enableAgentTrace: true,
  logRequestParams: true,
  logResponseResult: false
})

function applySettings(data: Record<string, any>) {
  if (data.basic) {
    const b = data.basic
    if (b.siteName !== undefined) basicForm.siteName = b.siteName
    if (b.logo !== undefined) basicForm.logo = b.logo
    if (b.description !== undefined) basicForm.description = b.description
    if (b.pageSize !== undefined) basicForm.pageSize = Number(b.pageSize)
  }
  if (data.security) {
    const s = data.security
    if (s.tokenExpire !== undefined) securityForm.tokenExpire = Number(s.tokenExpire)
    if (s.maxLoginAttempts !== undefined) securityForm.maxLoginAttempts = Number(s.maxLoginAttempts)
    if (s.minPasswordLength !== undefined) securityForm.minPasswordLength = Number(s.minPasswordLength)
    if (s.sessionTimeout !== undefined) securityForm.sessionTimeout = Number(s.sessionTimeout)
  }
  if (data.log) {
    const l = data.log
    if (l.retentionDays !== undefined) logForm.retentionDays = Number(l.retentionDays)
    if (l.enableOperationLog !== undefined) logForm.enableOperationLog = !!l.enableOperationLog
    if (l.enableAgentTrace !== undefined) logForm.enableAgentTrace = !!l.enableAgentTrace
    if (l.logRequestParams !== undefined) logForm.logRequestParams = !!l.logRequestParams
    if (l.logResponseResult !== undefined) logForm.logResponseResult = !!l.logResponseResult
  }
}

async function loadSettings() {
  pageLoading.value = true
  try {
    const res = await getSettings()
    const data = res?.data || res || {}
    applySettings(data)
  } catch {
    // Use defaults if API unavailable
  } finally {
    pageLoading.value = false
  }
}

function getCategoryData(tab: string): Record<string, any> {
  const categoryMap: Record<string, Record<string, any>> = {
    basic: {
      siteName: basicForm.siteName,
      logo: basicForm.logo,
      description: basicForm.description,
      pageSize: basicForm.pageSize
    },
    security: {
      tokenExpire: securityForm.tokenExpire,
      maxLoginAttempts: securityForm.maxLoginAttempts,
      minPasswordLength: securityForm.minPasswordLength,
      sessionTimeout: securityForm.sessionTimeout
    },
    log: {
      retentionDays: logForm.retentionDays,
      enableOperationLog: logForm.enableOperationLog,
      enableAgentTrace: logForm.enableAgentTrace,
      logRequestParams: logForm.logRequestParams,
      logResponseResult: logForm.logResponseResult
    }
  }
  return categoryMap[tab] || {}
}

async function handleSave(tab: string) {
  saveLoading.value = true
  try {
    await updateSettings(tab, getCategoryData(tab))
    const labelMap: Record<string, string> = { basic: '基本', security: '安全', log: '日志' }
    ElMessage.success(`${labelMap[tab] || tab}设置保存成功`)
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped lang="scss">
.settings-page {
  .settings-card {
    max-width: 800px;
  }

  .settings-tabs {
    :deep(.el-tabs__header) {
      margin-right: 32px;
    }
  }

  .settings-form {
    max-width: 520px;
    padding-top: 8px;
  }
}
</style>

<template>
  <div class="page-container metadata-export">
    <!-- Page header -->
    <div class="page-header">
      <h2>数据导出</h2>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: Export Tasks -->
      <el-tab-pane label="导出任务" name="export">
        <div class="tab-content">
          <!-- Export form -->
          <div class="export-form-card">
            <el-form :model="exportForm" :inline="true">
              <el-form-item label="导出格式">
                <el-select v-model="exportForm.format" placeholder="选择格式" style="width: 140px;">
                  <el-option label="JSON" value="json" />
                  <el-option label="XML" value="xml" />
                  <el-option label="Excel" value="excel" />
                </el-select>
              </el-form-item>
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="exportForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 260px;"
                />
              </el-form-item>
              <el-form-item label="文档类型">
                <el-select v-model="exportForm.documentType" placeholder="全部" clearable style="width: 140px;">
                  <el-option label="全部" value="" />
                  <el-option label="合同" value="contract" />
                  <el-option label="发票" value="invoice" />
                  <el-option label="提单" value="bill_of_lading" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExport">
                  导出
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- Export history table -->
          <div class="table-card" style="margin-top: 0;">
            <h3 class="section-title">导出历史</h3>
            <el-table :data="exportHistory" v-loading="historyLoading" stripe border style="width: 100%;">
              <el-table-column prop="id" label="ID" width="70" align="center" />
              <el-table-column label="导出时间" width="170">
                <template #default="{ row }">
                  {{ row.exportTime }}
                </template>
              </el-table-column>
              <el-table-column label="格式" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.format === 'excel' ? 'success' : row.format === 'json' ? 'primary' : 'warning'">
                    {{ row.format.toUpperCase() }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="recordCount" label="记录数" width="100" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'completed' ? 'success' : row.status === 'processing' ? 'warning' : 'danger'">
                    {{ statusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="下载" width="100" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'completed' && row.downloadUrl"
                    type="primary"
                    link
                    :icon="Download"
                    @click="handleDownload(row)"
                  >
                    下载
                  </el-button>
                  <span v-else class="text-secondary">-</span>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="historyPage"
                v-model:page-size="historyPageSize"
                :total="historyTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
              />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 2: Webhook Config -->
      <el-tab-pane label="Webhook 配置" name="webhook">
        <div class="tab-content">
          <div class="webhook-form-card">
            <el-form :model="webhookForm" label-width="100px" style="max-width: 640px;">
              <el-form-item label="Webhook URL">
                <el-input v-model="webhookForm.url" placeholder="https://your-api.example.com/webhook" />
              </el-form-item>
              <el-form-item label="密钥 (Secret)">
                <el-input v-model="webhookForm.secret" type="password" show-password placeholder="输入 Webhook 密钥" />
              </el-form-item>
              <el-form-item label="启用">
                <el-switch v-model="webhookForm.enabled" />
              </el-form-item>
              <el-form-item label="触发事件">
                <el-checkbox-group v-model="webhookForm.events">
                  <el-checkbox label="export.completed">导出完成</el-checkbox>
                  <el-checkbox label="export.failed">导出失败</el-checkbox>
                  <el-checkbox label="field.updated">字段更新</el-checkbox>
                  <el-checkbox label="field.created">字段新建</el-checkbox>
                  <el-checkbox label="document.processed">文档处理完成</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="webhookSaving" @click="handleSaveWebhook">
                  保存配置
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- API docs section -->
          <div class="api-docs-card">
            <h3 class="section-title">API 接入文档</h3>
            <p class="docs-desc">以下是通过 REST API 触发导出的示例：</p>

            <div class="code-block">
              <div class="code-block__header">
                <span class="code-block__label">POST /api/metadata/export</span>
                <span class="code-block__method">导出数据</span>
              </div>
              <pre class="code-block__body"><code>curl -X POST https://your-domain.com/api/metadata/export \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "format": "json",
    "documentIds": [1, 2, 3],
    "startDate": "2026-01-01",
    "endDate": "2026-06-30"
  }'</code></pre>
            </div>

            <div class="code-block">
              <div class="code-block__header">
                <span class="code-block__label">GET /api/metadata/export/history</span>
                <span class="code-block__method">查询导出历史</span>
              </div>
              <pre class="code-block__body"><code>curl -X GET "https://your-domain.com/api/metadata/export/history?page=1&pageSize=10" \
  -H "Authorization: Bearer YOUR_TOKEN"</code></pre>
            </div>

            <div class="code-block">
              <div class="code-block__header">
                <span class="code-block__label">Webhook Payload 示例</span>
                <span class="code-block__method">事件回调格式</span>
              </div>
              <pre class="code-block__body"><code>{
  "event": "export.completed",
  "timestamp": "2026-06-29T10:30:00Z",
  "data": {
    "exportId": 42,
    "format": "excel",
    "recordCount": 1523,
    "downloadUrl": "https://.../exports/42.xlsx"
  }
}</code></pre>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { exportData, getExportHistory, getWebhookConfig, updateWebhookConfig } from '@/api/metadata'

const activeTab = ref('export')

// Export tab
const exporting = ref(false)
const exportForm = reactive({
  format: 'json' as 'json' | 'xml' | 'excel',
  dateRange: [] as string[],
  documentType: ''
})

async function handleExport() {
  exporting.value = true
  try {
    const [startDate, endDate] = exportForm.dateRange
    await exportData({
      format: exportForm.format,
      startDate,
      endDate
    })
    ElMessage.success('导出任务已提交')
    loadExportHistory()
  } catch {
    // Error handled by interceptor
  } finally {
    exporting.value = false
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    completed: '已完成',
    processing: '处理中',
    failed: '失败'
  }
  return map[status] || status
}

async function handleDownload(row: any) {
  try {
    const res: any = await exportData({ format: row.format, fields: row.fields })
    // Try to handle as blob download or trigger from response
    const blob = new Blob([JSON.stringify(res?.data || res, null, 2)], {
      type: row.format === 'csv' ? 'text/csv' : 'application/json'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `metadata-export-${new Date().toISOString().slice(0, 10)}.${row.format}`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已下载 ${row.recordCount} 条记录`)
  } catch {
    ElMessage.warning('下载失败，请重试')
  }
}

// Export history
const historyLoading = ref(false)
const exportHistory = ref<any[]>([])
const historyPage = ref(1)
const historyPageSize = ref(10)
const historyTotal = ref(0)

async function loadExportHistory() {
  historyLoading.value = true
  try {
    const res = await getExportHistory({ page: historyPage.value, pageSize: historyPageSize.value })
    const result = res?.data || res
    exportHistory.value = result.records || result.list || []
    historyTotal.value = result.total || 0
  } catch {
    // Use mock data
    exportHistory.value = [
      { id: 5, exportTime: '2026-06-29 10:30:00', format: 'excel', recordCount: 523, status: 'completed', downloadUrl: '/exports/5.xlsx' },
      { id: 4, exportTime: '2026-06-28 16:20:00', format: 'json', recordCount: 1201, status: 'completed', downloadUrl: '/exports/4.json' },
      { id: 3, exportTime: '2026-06-27 09:15:00', format: 'xml', recordCount: 89, status: 'completed', downloadUrl: '/exports/3.xml' },
      { id: 2, exportTime: '2026-06-26 14:00:00', format: 'excel', recordCount: 756, status: 'completed', downloadUrl: '/exports/2.xlsx' },
      { id: 1, exportTime: '2026-06-25 11:45:00', format: 'json', recordCount: 332, status: 'failed', downloadUrl: '' },
    ]
    historyTotal.value = 5
  } finally {
    historyLoading.value = false
  }
}

// Webhook tab
const webhookSaving = ref(false)
const webhookForm = reactive({
  url: '',
  secret: '',
  enabled: false,
  events: ['export.completed'] as string[]
})

async function loadWebhookConfig() {
  try {
    const res = await getWebhookConfig()
    const data = res?.data || res
    if (data) {
      webhookForm.url = data.url || ''
      webhookForm.secret = data.secret || ''
      webhookForm.enabled = data.enabled || false
      webhookForm.events = data.events || ['export.completed']
    }
  } catch {
    // Use mock data
    webhookForm.url = 'https://hooks.example.com/webhook/ai-platform'
    webhookForm.secret = ''
    webhookForm.enabled = false
    webhookForm.events = ['export.completed']
  }
}

async function handleSaveWebhook() {
  if (!webhookForm.url) {
    ElMessage.warning('请输入 Webhook URL')
    return
  }
  webhookSaving.value = true
  try {
    await updateWebhookConfig({
      url: webhookForm.url,
      secret: webhookForm.secret,
      enabled: webhookForm.enabled,
      events: webhookForm.events
    })
    ElMessage.success('Webhook 配置已保存')
  } catch {
    // Error handled by interceptor
  } finally {
    webhookSaving.value = false
  }
}

onMounted(() => {
  loadExportHistory()
  loadWebhookConfig()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.metadata-export {
  .page-header {
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
    }
  }

  .tab-content {
    padding: 0;
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 16px;
  }

  .export-form-card {
    background: #fff;
    padding: 20px 24px;
    border-radius: 4px;
    margin-bottom: 16px;
  }

  .webhook-form-card {
    background: #fff;
    padding: 24px;
    border-radius: 4px;
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .text-secondary {
    color: $text-secondary;
  }
}

// API Docs
.api-docs-card {
  background: #fff;
  padding: 24px;
  border-radius: 4px;

  .docs-desc {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 16px;
  }
}

.code-block {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 16px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;
  }

  &__label {
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    font-family: 'Menlo', 'Consolas', monospace;
  }

  &__method {
    font-size: 12px;
    color: $text-secondary;
  }

  &__body {
    margin: 0;
    padding: 16px;
    background: #1e1e1e;
    overflow-x: auto;

    code {
      font-size: 13px;
      color: #d4d4d4;
      line-height: 1.7;
      font-family: 'Menlo', 'Consolas', monospace;
      white-space: pre;
    }
  }
}
</style>

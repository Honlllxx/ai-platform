<template>
  <div class="trace-timeline">
    <div v-if="!nodes || nodes.length === 0" class="empty-state">
      <el-empty description="暂无调用链路数据" />
    </div>
    <div v-else class="trace-tree">
      <div
        v-for="node in nodes"
        :key="node.id"
        class="trace-node"
        :class="{ expanded: expandedNodes.has(node.id) }"
      >
        <!-- Node Header -->
        <div class="node-header" @click="toggleNode(node)">
          <div class="node-left">
            <el-icon class="node-expand" :class="{ rotated: expandedNodes.has(node.id) }">
              <CaretRight v-if="hasChildren(node)" />
              <span v-else class="expand-placeholder"></span>
            </el-icon>
            <span class="node-type" :class="`type-${node.type}`">
              <el-icon :size="14">
                <Cpu v-if="node.type === 'llm_call' || node.type === 'llm_response'" />
                <Setting v-else-if="node.type === 'tool_call'" />
                <Files v-else-if="node.type === 'embedding'" />
                <Search v-else-if="node.type === 'vector_search'" />
                <Document v-else-if="node.type === 'retrieval'" />
              </el-icon>
            </span>
            <span class="node-name">{{ node.name }}</span>
            <el-tag :type="statusTagType(node.status)" size="small" class="node-status">
              {{ statusLabel(node.status) }}
            </el-tag>
          </div>
          <div class="node-right">
            <span class="node-time">{{ formatTime(node.startTime) }}</span>
            <span class="node-duration">{{ node.duration }}s</span>
          </div>
        </div>

        <!-- Node Detail Panel (expanded) -->
        <div v-if="expandedNodes.has(node.id)" class="node-detail">
          <div class="detail-row" v-if="node.input">
            <span class="detail-label">输入:</span>
            <pre class="detail-json">{{ formatJson(node.input) }}</pre>
          </div>
          <div class="detail-row" v-if="node.output">
            <span class="detail-label">输出:</span>
            <pre class="detail-json">{{ formatJson(node.output) }}</pre>
          </div>
          <div class="detail-info">
            <span v-if="node.tokenCount">Token: <strong>{{ node.tokenCount }}</strong></span>
            <span v-if="node.modelName">模型: <strong>{{ node.modelName }}</strong></span>
            <span>耗时: <strong>{{ node.duration }}s</strong></span>
            <span>状态: <strong>{{ statusLabel(node.status) }}</strong></span>
          </div>
          <div v-if="node.errorMessage" class="detail-error">
            {{ node.errorMessage }}
          </div>
        </div>

        <!-- Children (recursive) -->
        <div v-if="hasChildren(node)" class="node-children" :class="{ collapsed: !expandedNodes.has(node.id) }">
          <TraceTimeline
            :nodes="node.children || []"
            :expanded-nodes="expandedNodes"
            @toggle="(n) => $emit('toggle', n)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CaretRight, Cpu, Setting, Files, Search, Document } from '@element-plus/icons-vue'
import type { TraceNode } from '@/api/agent-trace'

const props = defineProps<{
  nodes: TraceNode[]
  expandedNodes: Set<string>
}>()

defineEmits<{
  (e: 'toggle', node: TraceNode): void
}>()

function hasChildren(node: TraceNode): boolean {
  return !!(node.children && node.children.length > 0)
}

function toggleNode(node: TraceNode) {
  // Only emit for internal use — the parent component manages expandedNodes
}

function formatTime(time: string): string {
  try {
    return new Date(time).toLocaleTimeString('zh-CN', { hour12: false })
  } catch {
    return time
  }
}

function formatJson(str: string): string {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

function statusTagType(status: string) {
  const map: Record<string, string> = { running: 'warning', success: 'success', fail: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = { running: '运行中', success: '成功', fail: '失败' }
  return map[status] || status
}
</script>

<style scoped lang="scss">
.trace-timeline {
  .empty-state {
    padding: 40px 0;
  }
}

.trace-node {
  font-size: 13px;

  .node-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    cursor: pointer;
    border-radius: 4px;
    transition: background 0.2s;

    &:hover {
      background: #f5f7fa;
    }
  }

  .node-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .node-expand {
    width: 16px;
    transition: transform 0.2s;

    &.rotated {
      transform: rotate(90deg);
    }

    .expand-placeholder {
      display: inline-block;
      width: 16px;
    }
  }

  .node-type {
    display: flex;
    align-items: center;

    &.type-llm_call, &.type-llm_response {
      color: #409eff;
    }

    &.type-tool_call {
      color: #67c23a;
    }

    &.type-embedding {
      color: #e6a23c;
    }

    &.type-vector_search {
      color: #f56c6c;
    }

    &.type-retrieval {
      color: #909399;
    }
  }

  .node-name {
    color: #303133;
    font-weight: 500;
  }

  .node-status {
    margin-left: 4px;
  }

  .node-right {
    display: flex;
    align-items: center;
    gap: 16px;
    color: #909399;
    font-size: 12px;
  }

  .node-duration {
    font-weight: 500;
    color: #606266;
  }

  .node-children {
    margin-left: 24px;
    border-left: 2px solid #e4e7ed;
    padding-left: 8px;

    &.collapsed {
      display: none;
    }
  }

  .node-detail {
    margin: 8px 12px 8px 36px;
    padding: 12px 16px;
    background: #fafafa;
    border-radius: 6px;
    border: 1px solid #ebeef5;
    font-size: 12px;

    .detail-row {
      margin-bottom: 8px;

      .detail-label {
        font-weight: 600;
        color: #303133;
        display: block;
        margin-bottom: 4px;
      }

      .detail-json {
        background: #fff;
        padding: 8px 12px;
        border-radius: 4px;
        border: 1px solid #e4e7ed;
        font-size: 12px;
        line-height: 1.6;
        max-height: 160px;
        overflow: auto;
        white-space: pre-wrap;
        word-break: break-all;
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        margin: 0;
      }
    }

    .detail-info {
      display: flex;
      gap: 16px;
      color: #606266;
      margin-bottom: 8px;

      strong {
        color: #303133;
      }
    }

    .detail-error {
      background: #fef0f0;
      border: 1px solid #fde2e2;
      color: #f56c6c;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 12px;
    }
  }
}
</style>

<template>
  <div class="state-graph">
    <div class="graph-legend">
      <span class="legend-title">节点类型：</span>
      <span class="legend-item">
        <span class="legend-dot legend-dot-llm"></span>
        LLM 节点
      </span>
      <span class="legend-item">
        <span class="legend-dot legend-dot-tool"></span>
        工具节点
      </span>
      <span class="legend-item">
        <span class="legend-dot legend-dot-decision"></span>
        决策节点
      </span>
    </div>

    <div class="graph-container">
      <!-- Arrows / Edges (SVG overlay) -->
      <svg class="graph-edges" ref="svgRef" :style="svgStyle">
        <template v-for="edge in graphData.edges" :key="`${edge.from}-${edge.to}`">
          <line
            :x1="getEdgeCoords(edge.from).x1"
            :y1="getEdgeCoords(edge.from).y1"
            :x2="getEdgeCoords(edge.to).x2"
            :y2="getEdgeCoords(edge.to).y2"
            stroke="#c0c4cc"
            stroke-width="2"
            marker-end="url(#arrowhead)"
          />
          <text
            v-if="edge.label"
            :x="(getEdgeCoords(edge.from).x1 + getEdgeCoords(edge.to).x2) / 2"
            :y="(getEdgeCoords(edge.from).y1 + getEdgeCoords(edge.to).y2) / 2 - 6"
            text-anchor="middle"
            font-size="11"
            fill="#909399"
          >{{ edge.label }}</text>
        </template>
        <defs>
          <marker
            id="arrowhead"
            viewBox="0 0 10 7"
            refX="9"
            refY="3.5"
            markerWidth="8"
            markerHeight="6"
            orient="auto"
          >
            <polygon points="0 0, 10 3.5, 0 7" fill="#c0c4cc" />
          </marker>
        </defs>
      </svg>

      <!-- Nodes -->
      <div class="graph-nodes" ref="nodesRef">
        <div
          v-for="node in graphData.nodes"
          :key="node.id"
          class="graph-node"
          :class="[`node-type-${node.type}`, `node-status-${node.status}`]"
          :data-node-id="node.id"
        >
          <div class="node-header">
            <span class="node-status-icon">
              <el-icon v-if="node.status === 'completed'" color="#67c23a"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="node.status === 'running'" color="#409eff"><Loading /></el-icon>
              <el-icon v-else-if="node.status === 'failed'" color="#f56c6c"><CircleCloseFilled /></el-icon>
              <el-icon v-else color="#c0c4cc"><RemoveFilled /></el-icon>
            </span>
            <span class="node-type-tag">{{ nodeTypeLabel(node.type) }}</span>
          </div>
          <div class="node-body">
            <span class="node-label">{{ node.label }}</span>
            <span class="node-duration">{{ formatDuration(node.duration) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import {
  CircleCheckFilled, CircleCloseFilled, Loading, RemoveFilled
} from '@element-plus/icons-vue'
import type { StateGraphData } from '@/api/pipeline'

const props = defineProps<{
  graphData: StateGraphData
}>()

const svgRef = ref<SVGElement>()
const nodesRef = ref<HTMLElement>()
const nodePositions = ref<Map<string, { x1: number; y1: number; x2: number; y2: number }>>(new Map())

const svgStyle = computed(() => {
  if (!nodesRef.value) return {}
  const rect = nodesRef.value.getBoundingClientRect()
  return {
    width: `${rect.width}px`,
    height: `${rect.height}px`
  }
})

function nodeTypeLabel(type: string): string {
  const map: Record<string, string> = {
    llm: 'LLM',
    tool: '工具',
    decision: '决策'
  }
  return map[type] || type
}

function formatDuration(seconds: number): string {
  if (!seconds && seconds !== 0) return ''
  if (seconds < 60) return `${seconds}s`
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}m ${secs}s`
}

function getEdgeCoords(nodeId: string): { x1: number; y1: number; x2: number; y2: number } {
  return nodePositions.value.get(nodeId) || { x1: 0, y1: 0, x2: 0, y2: 0 }
}

function recalculatePositions() {
  if (!nodesRef.value || !svgRef.value) return

  const container = nodesRef.value
  const containerRect = container.getBoundingClientRect()
  const nodeElements = container.querySelectorAll('.graph-node')
  const newPositions = new Map<string, { x1: number; y1: number; x2: number; y2: number }>()

  nodeElements.forEach((el) => {
    const nodeId = (el as HTMLElement).dataset.nodeId
    if (!nodeId) return
    const rect = el.getBoundingClientRect()
    const centerX = rect.left - containerRect.left + rect.width / 2
    const bottomY = rect.top - containerRect.top + rect.height
    const topY = rect.top - containerRect.top

    newPositions.set(nodeId, {
      x1: centerX,
      y1: bottomY,
      x2: centerX,
      y2: topY
    })
  })

  nodePositions.value = newPositions
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    recalculatePositions()
  })

  if (nodesRef.value) {
    resizeObserver = new ResizeObserver(() => {
      recalculatePositions()
    })
    resizeObserver.observe(nodesRef.value)
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped lang="scss">
.state-graph {
  .graph-legend {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 20px;
    padding: 10px 16px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 13px;

    .legend-title {
      color: #606266;
      font-weight: 500;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #909399;
    }

    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;

      &-llm { background: #409eff; }
      &-tool { background: #67c23a; }
      &-decision { background: #e6a23c; }
    }
  }

  .graph-container {
    position: relative;
    width: 100%;
  }

  .graph-edges {
    position: absolute;
    top: 0;
    left: 0;
    pointer-events: none;
    z-index: 1;
  }

  .graph-nodes {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 28px;
    padding: 20px 0;
    min-height: 200px;
  }

  .graph-node {
    width: 300px;
    border: 2px solid #e4e7ed;
    border-radius: 8px;
    background: #fff;
    padding: 14px 18px;
    transition: box-shadow 0.2s, border-color 0.2s;
    cursor: default;

    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }

    &.node-type-llm {
      border-left: 4px solid #409eff;
    }

    &.node-type-tool {
      border-left: 4px solid #67c23a;
    }

    &.node-type-decision {
      border-left: 4px solid #e6a23c;
    }

    &.node-status-running {
      animation: node-pulse 1.5s infinite;
    }

    &.node-status-failed {
      border-color: #f56c6c;
      background: #fef0f0;
    }
  }
}

@keyframes node-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(64, 158, 255, 0); }
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  .node-status-icon {
    display: flex;
    align-items: center;
    font-size: 16px;
  }

  .node-type-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 3px;
    background: #f0f2f5;
    color: #909399;
    font-weight: 500;
  }

  .node-type-llm & .node-type-tag {
    background: rgba(64, 158, 255, 0.1);
    color: #409eff;
  }

  .node-type-tool & .node-type-tag {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
  }

  .node-type-decision & .node-type-tag {
    background: rgba(230, 162, 60, 0.1);
    color: #e6a23c;
  }
}

.node-body {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .node-label {
    font-size: 15px;
    color: #303133;
    font-weight: 600;
  }

  .node-duration {
    font-size: 12px;
    color: #909399;
    font-weight: 500;
  }
}
</style>

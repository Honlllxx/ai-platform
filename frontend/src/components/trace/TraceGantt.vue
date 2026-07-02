<template>
  <div class="trace-gantt">
    <div v-if="!flatNodes || flatNodes.length === 0" class="empty-state">
      <el-empty description="暂无调用链路数据" />
    </div>
    <div v-else class="gantt-chart-wrap">
      <v-chart :option="ganttOption" style="height: 400px;" autoresize />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { CustomChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import type { TraceNode } from '@/api/agent-trace'

use([
  CanvasRenderer,
  CustomChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent
])

const props = defineProps<{
  nodes: TraceNode[]
}>()

interface FlatNode {
  id: string
  name: string
  type: string
  status: string
  startTime: number
  endTime: number
  duration: number
  depth: number
  modelName?: string
  tokenCount?: number
}

function flattenTree(nodes: TraceNode[], depth = 0): FlatNode[] {
  const result: FlatNode[] = []
  for (const node of nodes) {
    result.push({
      id: node.id,
      name: node.name,
      type: node.type,
      status: node.status,
      startTime: new Date(node.startTime).getTime(),
      endTime: new Date(node.endTime).getTime(),
      duration: node.duration,
      depth,
      modelName: node.modelName,
      tokenCount: node.tokenCount
    })
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, depth + 1))
    }
  }
  return result
}

const flatNodes = computed<FlatNode[]>(() => {
  if (!props.nodes || props.nodes.length === 0) return []
  return flattenTree(props.nodes)
})

const ganttOption = computed(() => {
  const nodes = flatNodes.value
  if (!nodes.length) return {}

  const minTime = Math.min(...nodes.map(n => n.startTime))
  const maxTime = Math.max(...nodes.map(n => n.endTime))
  const timeRange = maxTime - minTime || 1

  const rowHeight = 36
  const barHeight = 20

  const categories = [
    { name: 'LLM 调用', color: '#409eff' },
    { name: 'Tool 调用', color: '#67c23a' },
    { name: 'Embedding', color: '#e6a23c' },
    { name: '向量检索', color: '#f56c6c' },
    { name: '检索', color: '#909399' },
    { name: 'LLM 响应', color: '#9b59b6' }
  ]

  function getColor(type: string): string {
    const map: Record<string, string> = {
      llm_call: '#409eff',
      tool_call: '#67c23a',
      embedding: '#e6a23c',
      vector_search: '#f56c6c',
      retrieval: '#909399',
      llm_response: '#9b59b6'
    }
    return map[type] || '#909399'
  }

  function typeLabel(type: string): string {
    const map: Record<string, string> = {
      llm_call: 'LLM 调用',
      tool_call: 'Tool 调用',
      embedding: 'Embedding',
      vector_search: '向量检索',
      retrieval: '检索',
      llm_response: 'LLM 响应'
    }
    return map[type] || type
  }

  return {
    tooltip: {
      formatter: (params: any) => {
        const d = params.data
        if (!d) return ''
        const startStr = new Date(d[1]).toLocaleTimeString('zh-CN', { hour12: false } as Intl.DateTimeFormatOptions)
        const endStr = new Date(d[2]).toLocaleTimeString('zh-CN', { hour12: false } as Intl.DateTimeFormatOptions)
        const duration = ((d[2] - d[1]) / 1000).toFixed(2)
        return `<strong>${d[3]}</strong><br/>
          类型: ${d[5]}<br/>
          开始: ${startStr}<br/>
          结束: ${endStr}<br/>
          耗时: ${duration}s${d[6] ? `<br/>模型: ${d[6]}` : ''}${d[7] ? `<br/>Token: ${d[7]}` : ''}`
      }
    },
    grid: {
      left: 180,
      right: 30,
      top: 20,
      bottom: 60,
      containLabel: false
    },
    xAxis: {
      type: 'time',
      min: minTime,
      max: maxTime,
      axisLabel: {
        formatter: (val: number) => {
          return new Date(val).toLocaleTimeString('zh-CN', { hour12: false })
        }
      }
    },
    yAxis: {
      type: 'category',
      data: nodes.map(n => n.name),
      axisLabel: {
        width: 170,
        overflow: 'truncate',
        ellipsis: '...'
      },
      inverse: true
    },
    dataZoom: [
      {
        type: 'slider',
        start: 0,
        end: 100,
        height: 20,
        bottom: 10
      }
    ],
    series: [{
      type: 'custom',
      renderItem: (_params: any, api: any) => {
        const catIndex = api.value(0)
        const start = api.coord([api.value(1), catIndex])
        const end = api.coord([api.value(2), catIndex])
        const height = barHeight
        const y = start[1] - height / 2
        const width = Math.max(end[0] - start[0], 4)

        const color = api.value(4)

        return {
          type: 'rect',
          shape: {
            x: start[0],
            y,
            width,
            height
          },
          style: {
            fill: color,
            rx: 4,
            ry: 4
          },
          textContent: api.value(3).length > 12 ? undefined : undefined
        }
      },
      data: nodes.map(n => [
        n.name,
        n.startTime,
        n.endTime,
        n.name,
        getColor(n.type),
        typeLabel(n.type),
        n.modelName,
        n.tokenCount,
        n.status
      ]),
      encode: {
        x: [1, 2],
        y: 0
      }
    }]
  }
})
</script>

<style scoped lang="scss">
.trace-gantt {
  .empty-state {
    padding: 40px 0;
  }

  .gantt-chart-wrap {
    min-height: 200px;
  }
}
</style>

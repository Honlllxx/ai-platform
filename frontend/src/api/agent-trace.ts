import request from '@/utils/request'

export interface TraceNode {
  id: string
  parentId: string | null
  name: string
  type: 'llm_call' | 'tool_call' | 'embedding' | 'vector_search' | 'retrieval' | 'llm_response'
  status: 'running' | 'success' | 'fail'
  startTime: string
  endTime: string
  duration: number
  input?: string
  output?: string
  tokenCount?: number
  modelName?: string
  errorMessage?: string
  children?: TraceNode[]
}

export interface TraceInfo {
  id: string
  traceId: string
  sessionId: string
  userId: string
  username: string
  query: string
  status: 'running' | 'success' | 'fail'
  totalDuration: number
  totalTokens: number
  startTime: string
  endTime: string
  nodes: TraceNode[]
}

export interface TraceQuery {
  page: number
  pageSize: number
  startTime?: string
  endTime?: string
  username?: string
  status?: string
  traceId?: string
}

export function getAgentTraces(params: TraceQuery) {
  return request.get('/agent-traces', { params })
}

export function getAgentTraceById(id: string) {
  return request.get(`/agent-traces/${id}`)
}

export function getTraceTree(traceId: string) {
  return request.get(`/agent-traces/${traceId}/tree`)
}

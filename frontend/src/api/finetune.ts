import request from '@/utils/request'

export interface FinetuneTask {
  id: number
  taskName: string
  baseModel: string
  datasetName: string
  datasetSize: number
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'
  progress: number  // 0-100
  epochs: number
  currentEpoch: number
  learningRate: number
  loss?: number
  metrics?: { accuracy: number; f1: number }
  outputModelName?: string
  startTime?: string
  endTime?: string
  duration?: number
  errorMessage?: string
  createTime: string
}

export function getFinetuneTasks(params: { page: number; pageSize: number; status?: string }) {
  return request.get('/finetune/tasks', { params })
}

export function getFinetuneTask(id: number) {
  return request.get(`/finetune/tasks/${id}`)
}

export function createFinetuneTask(data: { taskName: string; baseModel: string; datasetName: string; epochs: number; learningRate: number }) {
  return request.post('/finetune/tasks', data)
}

/** Cancel (delete) a finetune task — fixed: was POST /cancel, now DELETE per Java API. */
export function cancelFinetuneTask(id: number) {
  return request.delete(`/finetune/tasks/${id}`)
}

import request from '@/utils/request'

/** Get all settings grouped by category */
export function getSettings() {
  return request.get('/settings')
}

/** Get settings for a specific category (basic | security | log) */
export function getSettingsByCategory(category: string) {
  return request.get(`/settings/${category}`)
}

/** Batch update settings for a category */
export function updateSettings(category: string, data: Record<string, any>) {
  return request.put(`/settings/${category}`, data)
}

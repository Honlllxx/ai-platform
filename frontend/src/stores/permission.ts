import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from './user'

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])

  const flatPermissions = computed(() => {
    const userStore = useUserStore()
    return userStore.permissions
  })

  function hasPermission(permission: string): boolean {
    const userStore = useUserStore()
    return userStore.hasPermission(permission)
  }

  function hasAnyPermission(permissions: string[]): boolean {
    const userStore = useUserStore()
    return userStore.hasAnyPermission(permissions)
  }

  function setRoutes(r: RouteRecordRaw[]) {
    routes.value = r
  }

  return {
    routes,
    flatPermissions,
    hasPermission,
    hasAnyPermission,
    setRoutes
  }
})

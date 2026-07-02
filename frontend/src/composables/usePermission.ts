import { computed } from 'vue'
import { usePermissionStore } from '@/stores/permission'

export function usePermission() {
  const permissionStore = usePermissionStore()

  const permissions = computed(() => permissionStore.flatPermissions)

  function has(permission: string): boolean {
    return permissionStore.hasPermission(permission)
  }

  function hasAny(permissions: string[]): boolean {
    return permissionStore.hasAnyPermission(permissions)
  }

  function check(permission: string): boolean {
    return has(permission)
  }

  return {
    permissions,
    has,
    hasAny,
    check
  }
}

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  const isLoggedIn = computed(() => !!userStore.token)
  const currentUser = computed(() => ({
    userId: userStore.userId,
    username: userStore.username,
    nickname: userStore.nickname,
    avatar: userStore.avatar,
    roles: userStore.roles,
    permissions: userStore.permissions
  }))

  function hasPermission(permission: string): boolean {
    return userStore.hasPermission(permission)
  }

  function hasAnyPermission(permissions: string[]): boolean {
    return userStore.hasAnyPermission(permissions)
  }

  function hasAllPermissions(permissions: string[]): boolean {
    return userStore.hasAllPermissions(permissions)
  }

  function isAdmin(): boolean {
    return userStore.roles.includes('admin')
  }

  async function logout() {
    await userStore.logout()
    router.push('/login')
  }

  return {
    isLoggedIn,
    currentUser,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    isAdmin,
    logout
  }
}

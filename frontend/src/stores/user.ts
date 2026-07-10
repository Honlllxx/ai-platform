import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import { getToken, setToken, removeToken, setRefreshToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userId = ref<number>(0)
  const username = ref<string>('')
  const nickname = ref<string>('')
  const avatar = ref<string>('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  async function login(uname: string, password: string, remember = false) {
    const res = await loginApi({ username: uname, password, remember })
    const data = res.data || res
    token.value = data.accessToken || data.token
    if (data.refreshToken) {
      setRefreshToken(data.refreshToken)
    }
    setToken(token.value)
    userId.value = data.userInfo.id
    username.value = data.userInfo.username
    nickname.value = data.userInfo.nickname
    avatar.value = data.userInfo.avatar || ''
    roles.value = data.userInfo.roles || []
    permissions.value = data.userInfo.permissions || []
  }

  async function fetchUserInfo() {
    try {
      const res = await getCurrentUser()
      userId.value = res.data.userId
      username.value = res.data.username
      nickname.value = res.data.nickname
      avatar.value = res.data.avatar
      roles.value = res.data.roles || []
      permissions.value = res.data.permissions || []
    } catch {
      // Token invalid - handled by interceptor
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      removeToken()
      token.value = ''
      userId.value = 0
      username.value = ''
      nickname.value = ''
      avatar.value = ''
      roles.value = []
      permissions.value = []
    }
  }

  function hasPermission(permission: string): boolean {
    if (roles.value.includes('admin')) return true
    return permissions.value.includes(permission)
  }

  function hasAnyPermission(perms: string[]): boolean {
    if (roles.value.includes('admin')) return true
    return perms.some(p => permissions.value.includes(p))
  }

  function hasAllPermissions(perms: string[]): boolean {
    if (roles.value.includes('admin')) return true
    return perms.every(p => permissions.value.includes(p))
  }

  return {
    token,
    userId,
    username,
    nickname,
    avatar,
    roles,
    permissions,
    login,
    logout,
    fetchUserInfo,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions
  }
})

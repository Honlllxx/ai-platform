import type { App, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

export function setupPermissionDirective(app: App) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
      const userStore = useUserStore()
      const required = binding.value

      if (!required) return

      let hasAuth = false
      if (Array.isArray(required)) {
        hasAuth = required.some((p: string) => userStore.hasPermission(p))
      } else if (typeof required === 'string') {
        hasAuth = userStore.hasPermission(required)
      }

      if (!hasAuth) {
        el.remove()
      }
    }
  })
}

import type { App, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const userStore = useUserStore()
  const required = binding.value

  if (!required) {
    el.style.display = ''
    return
  }

  let hasAuth = false
  if (Array.isArray(required)) {
    hasAuth = required.some((p: string) => userStore.hasPermission(p))
  } else if (typeof required === 'string') {
    hasAuth = userStore.hasPermission(required)
  }

  // Use display:none instead of el.remove() to allow re-show on permission changes
  // and avoid leaking Element Plus internal event listeners
  el.style.display = hasAuth ? '' : 'none'
}

export function setupPermissionDirective(app: App) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
      checkPermission(el, binding)
    },
    updated(el: HTMLElement, binding: DirectiveBinding) {
      checkPermission(el, binding)
    }
  })
}

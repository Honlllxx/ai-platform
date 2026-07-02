import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'
import './styles/index.scss'
import { setupPermissionDirective } from './directives/permission'

// Load mock API layer in development (controlled by VITE_USE_MOCK)
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
  const { setupMock } = await import('./mock')
  setupMock()
  console.log('[Mock] Mock API layer enabled')
}

const app = createApp(App)

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { size: 'default' })

// Setup permission directive
setupPermissionDirective(app)

app.mount('#app')

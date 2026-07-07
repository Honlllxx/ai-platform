import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      // AI-related paths → Python FastAPI :8000 (must be before the catch-all /api)
      '/api/ai': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      '/api/prompt': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      '/api/finetune': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      '/api/models': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      // All other business API → Java Spring Boot :8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts', 'vue-echarts'],
          elementPlus: ['element-plus', '@element-plus/icons-vue']
        }
      }
    }
  }
})

<template>
  <div class="default-layout">
    <div class="layout-sidebar" :class="{ collapsed: isCollapse }">
      <div class="logo">
        <span v-if="!isCollapse" class="logo-text">AI Platform</span>
        <span v-else class="logo-text-mini">AI</span>
      </div>
      <Sidebar :is-collapse="isCollapse" />
    </div>
    <div class="layout-main" :class="{ collapsed: isCollapse }">
      <HeaderBar :is-collapse="isCollapse" @toggle-collapse="isCollapse = !isCollapse" />
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from '@/components/layout/Sidebar.vue'
import HeaderBar from '@/components/layout/HeaderBar.vue'

const isCollapse = ref(false)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.default-layout {
  height: 100%;
  display: flex;
  overflow: hidden;
}

.layout-sidebar {
  width: $sidebar-width;
  background: $sidebar-bg;
  transition: width 0.3s;
  flex-shrink: 0;
  overflow: hidden;

  &.collapsed {
    width: $sidebar-collapsed-width;
  }
}

.logo {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1f2f3f;

  .logo-text {
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    white-space: nowrap;
  }

  .logo-text-mini {
    color: #fff;
    font-size: 20px;
    font-weight: 700;
  }
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  background: $bg-color;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

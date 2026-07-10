<template>
  <div class="header-bar">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="$emit('toggle-collapse')">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <!-- Notification Bell -->
      <el-popover
        placement="bottom-end"
        :width="360"
        trigger="click"
        :visible="notificationVisible"
        @show="loadNotifications"
        @hide="notificationVisible = false"
      >
        <template #reference>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
            <el-button link @click="notificationVisible = !notificationVisible" class="notification-btn">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
        </template>
        <div class="notification-popover">
          <div class="notification-header">
            <span class="notification-title">审核通知</span>
            <el-button link type="primary" size="small" @click="goToReviewList">查看全部</el-button>
          </div>
          <div class="notification-list" v-if="notifications.length > 0">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              @click="goToReview(item.reviewId)"
            >
              <div class="notification-item__icon">
                <el-icon :size="18" :color="actionColor(item.action)">
                  <Bell v-if="item.action === 'assign'" />
                  <Clock v-else-if="item.action === 'start_review'" />
                  <Select v-else-if="item.action === 'approved' || item.action === 'batch_approved'" />
                  <CloseBold v-else-if="item.action === 'rejected' || item.action === 'batch_rejected'" />
                  <Edit v-else-if="item.action === 'corrected'" />
                  <ChatLineSquare v-else-if="item.action === 'comment'" />
                  <InfoFilled v-else />
                </el-icon>
              </div>
              <div class="notification-item__content">
                <div class="notification-item__title">
                  <span class="doc-name">{{ item.documentName }}</span>
                  <el-tag size="small" :type="actionTagType(item.action)">{{ item.actionLabel }}</el-tag>
                </div>
                <div class="notification-item__time">{{ formatTime(item.timestamp || item.createTime) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无审核通知" :image-size="60" />
        </div>
      </el-popover>

      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="32" :icon="UserFilled" />
          <span class="username">{{ userStore.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人设置</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserFilled, Bell, Clock, Select, CloseBold, Edit, ChatLineSquare, InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUnreadCount, getNotifications, type NotificationItem } from '@/api/notification'

defineProps<{
  isCollapse: boolean
}>()

defineEmits<{
  (e: 'toggle-collapse'): void
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta?.title)
  return matched.map(r => ({
    path: r.path,
    title: r.meta?.title as string
  }))
})

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/settings')
  }
}

// ==================== Notification ====================

const notificationVisible = ref(false)
const unreadCount = ref(0)
const notifications = ref<NotificationItem[]>([])

async function loadNotifications() {
  try {
    const [countRes, listRes] = await Promise.all([
      getUnreadCount(),
      getNotifications(10)
    ])
    const countData = (countRes as any)?.data || countRes
    unreadCount.value = countData.count || 0
    const listData = (listRes as any)?.data || listRes
    notifications.value = listData || []
  } catch {
    // Silently fail
  }
}

function actionColor(action: string) {
  const map: Record<string, string> = {
    assign: '#409eff',
    start_review: '#e6a23c',
    approved: '#67c23a',
    batch_approved: '#67c23a',
    rejected: '#f56c6c',
    batch_rejected: '#f56c6c',
    corrected: '#409eff',
    comment: '#909399',
    status_change: '#e6a23c'
  }
  return map[action] || '#909399'
}

function actionTagType(action: string) {
  const map: Record<string, string> = {
    assign: 'primary',
    start_review: 'warning',
    approved: 'success',
    batch_approved: 'success',
    rejected: 'danger',
    batch_rejected: 'danger',
    corrected: 'primary',
    comment: 'info',
    status_change: 'warning'
  }
  return map[action] || 'info'
}

function goToReview(reviewId: number) {
  notificationVisible.value = false
  router.push(`/review/${reviewId}`)
}

function goToReviewList() {
  notificationVisible.value = false
  router.push('/review/list')
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.header-bar {
  height: $header-height;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 100;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: $text-regular;

  &:hover {
    color: $primary-color;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

// Notification badge
.notification-badge {
  :deep(.el-badge__content) {
    font-size: 10px;
  }
}

.notification-btn {
  padding: 4px;
  color: $text-regular;

  &:hover {
    color: $primary-color;
  }
}

.notification-popover {
  max-height: 420px;
  display: flex;
  flex-direction: column;

  .notification-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 10px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 8px;

    .notification-title {
      font-size: 15px;
      font-weight: 600;
      color: $text-primary;
    }
  }

  .notification-list {
    overflow-y: auto;
    max-height: 340px;
  }

  .notification-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 10px 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover {
      background: #f5f7fa;
    }

    &__icon {
      flex-shrink: 0;
      margin-top: 2px;
    }

    &__content {
      flex: 1;
      min-width: 0;
    }

    &__title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;

      .doc-name {
        font-size: 13px;
        font-weight: 500;
        color: $text-primary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 180px;
      }
    }

    &__time {
      font-size: 12px;
      color: $text-placeholder;
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: $text-regular;

  .username {
    font-size: 14px;
  }

  &:hover {
    color: $primary-color;
  }
}
</style>

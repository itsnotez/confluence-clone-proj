<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/spaces" class="logo-link">사내 지식관리 시스템</router-link>
    </div>
    <div class="header-center">
      <DxTextBox
        v-model:value="searchQuery"
        placeholder="검색..."
        :width="320"
        @enter-key="handleSearch"
      />
    </div>
    <div class="header-right">
      <router-link v-if="auth.user?.role === 'SITE_ADMIN'" to="/admin" class="admin-link">관리자</router-link>
      <span v-if="auth.user" class="user-name">{{ auth.user.name }}</span>

      <!-- 알림 벨 아이콘 -->
      <div class="notif-wrapper" ref="notifWrapperRef">
        <button class="notif-bell" @click="toggleDropdown" aria-label="알림">
          🔔
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
        </button>

        <!-- 알림 드롭다운 패널 -->
        <div v-if="showDropdown" class="notif-dropdown">
          <div class="notif-header">
            <span class="notif-title">알림</span>
            <button class="mark-all-btn" @click="handleMarkAllRead">모두 읽음</button>
          </div>
          <div class="notif-list">
            <div
              v-if="notifications.length === 0"
              class="notif-empty"
            >
              알림이 없습니다.
            </div>
            <div
              v-for="n in notifications"
              :key="n.id"
              class="notif-item"
              :class="{ unread: !n.isRead }"
              @click="handleNotifClick(n)"
            >
              <div class="notif-item-title">{{ n.title }}</div>
              <div class="notif-item-message">{{ n.message }}</div>
            </div>
          </div>
        </div>
      </div>

      <DxButton
        text="로그아웃"
        type="normal"
        styling-mode="outlined"
        @click="handleLogout"
      />
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { storeToRefs } from 'pinia'
import { DxTextBox } from 'devextreme-vue/text-box'
import { DxButton } from 'devextreme-vue/button'

const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()
const { unreadCount, notifications } = storeToRefs(notificationStore)

const searchQuery = ref('')
const showDropdown = ref(false)
const notifWrapperRef = ref(null)

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value.trim() } })
  }
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    notificationStore.fetchNotifications()
  }
}

async function handleNotifClick(n) {
  if (!n.isRead) {
    await notificationStore.markRead(n.id)
  }
  showDropdown.value = false
  if (n.linkUrl) {
    router.push(n.linkUrl)
  }
}

async function handleMarkAllRead() {
  await notificationStore.markAllRead()
}

// 드롭다운 외부 클릭 시 닫기
function handleClickOutside(event) {
  if (notifWrapperRef.value && !notifWrapperRef.value.contains(event.target)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  height: 56px;
  background: #1976d2;
  padding: 0 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  flex: 0 0 auto;
  margin-right: 24px;
}
.logo-link {
  color: white;
  text-decoration: none;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}
.header-right {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 24px;
}
.admin-link {
  color: white;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 10px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  text-decoration: none;
}
.admin-link:hover {
  background: rgba(255, 255, 255, 0.15);
}
.user-name {
  color: white;
  font-size: 14px;
}

/* 알림 벨 */
.notif-wrapper {
  position: relative;
}
.notif-bell {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 20px;
  position: relative;
  padding: 4px 8px;
  color: white;
  display: flex;
  align-items: center;
}
.notif-bell:hover {
  opacity: 0.8;
}
.badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #f44336;
  color: white;
  font-size: 10px;
  font-weight: 700;
  border-radius: 50%;
  min-width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
  line-height: 1;
}

/* 드롭다운 패널 */
.notif-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 320px;
  max-height: 400px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  z-index: 200;
  display: flex;
  flex-direction: column;
}
.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}
.notif-title {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}
.mark-all-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #1976d2;
  font-size: 12px;
  padding: 4px 8px;
}
.mark-all-btn:hover {
  text-decoration: underline;
}
.notif-list {
  overflow-y: auto;
  flex: 1;
}
.notif-empty {
  padding: 24px 16px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.notif-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.1s;
}
.notif-item:hover {
  background: #f5f5f5;
}
.notif-item.unread {
  background: #e8f0fe;
}
.notif-item.unread:hover {
  background: #dce8fd;
}
.notif-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.notif-item-message {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

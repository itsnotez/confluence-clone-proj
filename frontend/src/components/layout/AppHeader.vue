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
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
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
  height: var(--header-height, 56px);
  background: #FFFFFF;
  padding: 0 16px;
  border-bottom: 1px solid #E6E8EA;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  flex: 0 0 auto;
  margin-right: 24px;
}
.logo-link {
  color: #1E2124;
  text-decoration: none;
  font-size: 17px;
  font-weight: 700;
  white-space: nowrap;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.logo-link:hover {
  color: #256EF4;
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
  color: #1E2124;
  font-size: 15px;
  font-weight: 400;
  padding: 6px 12px;
  border: 1px solid #58616A;
  border-radius: 6px;
  text-decoration: none;
  background: transparent;
}
.admin-link:hover {
  background: #F4F5F6;
  color: #1E2124;
}
.user-name {
  color: #464C53;
  font-size: 15px;
  font-weight: 400;
}

/* 알림 벨 */
.notif-wrapper {
  position: relative;
}
.notif-bell {
  background: transparent;
  border: 1px solid #58616A;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  padding: 7px 8px;
  color: #1E2124;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.notif-bell:hover {
  background: #F4F5F6;
}
.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #DE3412;
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
  background: #FFFFFF;
  border: 1px solid #B1B8BE;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
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
  border-bottom: 1px solid #E6E8EA;
  flex-shrink: 0;
  background: #F4F5F6;
}
.notif-title {
  font-weight: 700;
  font-size: 15px;
  color: #1E2124;
}
.mark-all-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #256EF4;
  font-size: 13px;
  padding: 4px 8px;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
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
  color: #6D7882;
  font-size: 15px;
}
.notif-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #E6E8EA;
  transition: background 0.1s;
}
.notif-item:hover {
  background: #F4F5F6;
}
.notif-item.unread {
  background: #ECF2FE;
}
.notif-item.unread:hover {
  background: #dce8fd;
}
.notif-item-title {
  font-size: 15px;
  font-weight: 700;
  color: #1E2124;
  margin-bottom: 4px;
}
.notif-item-message {
  font-size: 13px;
  color: #6D7882;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

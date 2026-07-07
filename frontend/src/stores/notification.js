import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  let pollTimer = null

  async function fetchUnreadCount() {
    try {
      const { data } = await notificationApi.getUnreadCount()
      unreadCount.value = data.data
    } catch (e) {
      // 폴링 실패 무시
    }
  }

  async function fetchNotifications() {
    const { data } = await notificationApi.getNotifications({ page: 0, size: 20 })
    notifications.value = data.data.content
  }

  async function markRead(id) {
    await notificationApi.markRead(id)
    notifications.value = notifications.value.map(n => n.id === id ? { ...n, isRead: true } : n)
    if (unreadCount.value > 0) unreadCount.value--
  }

  async function markAllRead() {
    await notificationApi.markAllRead()
    unreadCount.value = 0
    notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
  }

  function startPolling() {
    fetchUnreadCount()
    if (pollTimer) clearInterval(pollTimer) // 중복 등록 방지
    pollTimer = setInterval(fetchUnreadCount, 30000)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  return { unreadCount, notifications, fetchUnreadCount, fetchNotifications, markRead, markAllRead, startPolling, stopPolling }
})

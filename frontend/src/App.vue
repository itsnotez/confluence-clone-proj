<template>
  <RouterView />
</template>

<script setup>
import { watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const auth = useAuthStore()
const notif = useNotificationStore()

onMounted(() => {
  if (auth.isLoggedIn) {
    notif.startPolling()
  }
})

onUnmounted(() => {
  notif.stopPolling()
})

// 로그인/로그아웃 시 폴링 시작/중지
watch(() => auth.isLoggedIn, (v) => {
  if (v) {
    notif.startPolling()
  } else {
    notif.stopPolling()
  }
})
</script>

<style>
*, *::before, *::after {
  box-sizing: border-box;
}
body {
  margin: 0;
  padding: 0;
  font-family: 'Noto Sans KR', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #fff;
  color: #333;
}
a {
  text-decoration: none;
  color: inherit;
}
</style>

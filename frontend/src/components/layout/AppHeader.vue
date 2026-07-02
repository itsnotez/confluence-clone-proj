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
      <span v-if="auth.user" class="user-name">{{ auth.user.name }}</span>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DxTextBox } from 'devextreme-vue/text-box'
import { DxButton } from 'devextreme-vue/button'

const router = useRouter()
const auth = useAuthStore()
const searchQuery = ref('')

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value.trim() } })
  }
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
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
.user-name {
  color: white;
  font-size: 14px;
}
</style>

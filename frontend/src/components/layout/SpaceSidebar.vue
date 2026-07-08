<template>
  <aside class="space-sidebar">
    <div class="sidebar-header">
      <RouterLink :to="`/spaces/${spaceKey}`" class="space-name-link">
        {{ spaceStore.currentSpace?.name || '스페이스' }}
      </RouterLink>
      <button class="mailbox-btn" @click="goMailBox" title="메일함">
        <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
          <rect x="1" y="3" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.3"/>
          <path d="M1 5.5 L8 9.5 L15 5.5" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>
        </svg>
        메일함
      </button>
    </div>
    <ContentTree :space-key="spaceKey" />
    <div class="sidebar-footer">
      <button class="new-page-btn" @click="goNewPage">+ 새 페이지 만들기</button>
    </div>
  </aside>
</template>

<script setup>
import { useRouter, RouterLink } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import ContentTree from './ContentTree.vue'

const props = defineProps({
  spaceKey: {
    type: String,
    required: true
  }
})

const router = useRouter()
const spaceStore = useSpaceStore()

function goNewPage() {
  router.push(`/spaces/${props.spaceKey}/contents/new`)
}

function goMailBox() {
  router.push({ name: 'MailBox', params: { spaceKey: props.spaceKey } })
}
</script>

<style scoped>
.space-sidebar {
  width: 250px;
  min-width: 250px;
  background: #FFFFFF;
  border-right: 1px solid #E6E8EA;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.sidebar-header {
  padding: 10px 12px;
  border-bottom: 1px solid #E6E8EA;
  background: #F4F5F6;
  display: flex;
  align-items: center;
  gap: 8px;
}
.space-name-link {
  font-weight: 700;
  font-size: 15px;
  color: #1E2124;
  text-decoration: none;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.space-name-link:hover {
  color: #256EF4;
}
.mailbox-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: transparent;
  color: #1E2124;
  border: 1px solid #58616A;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  flex-shrink: 0;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.mailbox-btn:hover {
  background: #F4F5F6;
}
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #E6E8EA;
  margin-top: auto;
}
.new-page-btn {
  width: 100%;
  padding: 10px 12px;
  background: #256EF4;
  color: white;
  border: 1px solid #256EF4;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 400;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.new-page-btn:hover {
  background: #0B50D0;
  border-color: #0B50D0;
}
</style>

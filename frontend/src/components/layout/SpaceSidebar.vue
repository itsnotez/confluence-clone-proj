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
  background: #f5f5f5;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
}
.sidebar-header {
  padding: 10px 12px;
  border-bottom: 1px solid #e0e0e0;
  background: #eeeeee;
  display: flex;
  align-items: center;
  gap: 8px;
}
.space-name-link {
  font-weight: 600;
  font-size: 14px;
  color: #1565c0;
  text-decoration: none;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.space-name-link:hover {
  text-decoration: underline;
}
.mailbox-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  background: #fff;
  color: #1976d2;
  border: 1px solid #1976d2;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}
.mailbox-btn:hover {
  background: #e3f2fd;
}
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #e0e0e0;
  margin-top: auto;
}
.new-page-btn {
  width: 100%;
  padding: 8px 12px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.new-page-btn:hover {
  background: #1565c0;
}
</style>

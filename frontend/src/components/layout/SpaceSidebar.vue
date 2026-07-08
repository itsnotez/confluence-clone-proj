<template>
  <aside class="space-sidebar">
    <div class="sidebar-header">
      <RouterLink :to="`/spaces/${spaceKey}`" class="space-name-link">
        {{ spaceStore.currentSpace?.name || '스페이스' }}
      </RouterLink>
    </div>
    <ContentTree :space-key="spaceKey" />
    <div class="sidebar-footer">
      <button class="new-page-btn" @click="goNewPage">+ 새 페이지 만들기</button>
      <button class="mailbox-btn" @click="goMailBox">메일함</button>
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
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  background: #eeeeee;
}
.space-name-link {
  font-weight: 600;
  font-size: 15px;
  color: #1565c0;
  text-decoration: none;
  display: block;
}
.space-name-link:hover {
  text-decoration: underline;
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
.mailbox-btn {
  width: 100%;
  padding: 8px 12px;
  background: #fff;
  color: #1976d2;
  border: 1px solid #1976d2;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  margin-top: 8px;
}
.mailbox-btn:hover {
  background: #e3f2fd;
}
</style>

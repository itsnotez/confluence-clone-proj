<template>
  <div class="content-page">
    <AppHeader />
    <div class="content-layout">
      <SpaceSidebar :space-key="spaceKey" />
      <main class="content-main">
        <div v-if="loading" class="loading-area">
          <DxLoadIndicator :visible="true" />
          <span>로딩 중...</span>
        </div>
        <div v-else-if="contentStore.currentContent" class="content-body">
          <div class="content-toolbar">
            <div class="content-meta">
              <span class="content-status" :class="contentStore.currentContent.status?.toLowerCase()">
                {{ contentStore.currentContent.status === 'PUBLISHED' ? '게시됨' : '임시저장' }}
              </span>
              <span v-if="contentStore.currentContent.createdBy" class="meta-author">
                작성자: {{ contentStore.currentContent.createdBy.name || contentStore.currentContent.createdBy }}
              </span>
              <span v-if="contentStore.currentContent.createdAt" class="meta-date">
                {{ formatDate(contentStore.currentContent.createdAt) }}
              </span>
            </div>
            <div class="content-actions">
              <DxButton
                text="버전 기록"
                type="normal"
                styling-mode="outlined"
                @click="showVersionHistory = true"
              />
              <DxButton
                text="편집"
                type="default"
                @click="goEdit"
              />
            </div>
          </div>
          <h1 class="content-title">{{ contentStore.currentContent.title }}</h1>
          <TipTapEditor
            :model-value="contentStore.currentContent.body"
            :readonly="true"
          />
        </div>
        <div v-else class="empty-state">
          <p>콘텐츠를 불러올 수 없습니다.</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useContentStore } from '@/stores/content'
import AppHeader from '@/components/layout/AppHeader.vue'
import SpaceSidebar from '@/components/layout/SpaceSidebar.vue'
import TipTapEditor from '@/components/content/TipTapEditor.vue'
import { DxButton } from 'devextreme-vue/button'
import { DxLoadIndicator } from 'devextreme-vue/load-indicator'

const route = useRoute()
const router = useRouter()
const contentStore = useContentStore()

const spaceKey = route.params.spaceKey
const contentId = route.params.contentId
const loading = ref(true)
const showVersionHistory = ref(false)

onMounted(async () => {
  await contentStore.fetchContent(contentId)
  loading.value = false
})

function goEdit() {
  router.push(`/spaces/${spaceKey}/contents/${contentId}/edit`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric'
  })
}
</script>

<style scoped>
.content-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.content-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.content-main {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
  background: white;
}
.loading-area {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
  margin-top: 60px;
}
.content-body {
  max-width: 900px;
  margin: 0 auto;
}
.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.content-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #666;
}
.content-status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.content-status.published {
  background: #e8f5e9;
  color: #2e7d32;
}
.content-status.draft {
  background: #fff8e1;
  color: #f57f17;
}
.content-actions {
  display: flex;
  gap: 8px;
}
.content-title {
  font-size: 2em;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 24px 0;
  line-height: 1.3;
}
.empty-state {
  text-align: center;
  color: #999;
  margin-top: 80px;
  font-size: 15px;
}
</style>

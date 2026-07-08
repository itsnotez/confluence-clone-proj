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
            <button class="back-btn" @click="router.back()">← 뒤로</button>
            <div class="content-meta" style="flex:1">
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
          <AttachmentPanel :content-id="contentStore.currentContent?.id" :readonly="true" />
          <div class="label-panel">
            <p class="label-title">라벨</p>
            <div v-if="labels.length === 0" class="label-empty">라벨 없음</div>
            <div v-else class="label-chips">
              <span
                v-for="lb in labels"
                :key="lb.id"
                class="label-chip"
                :style="lb.color ? { background: lb.color + '22', borderColor: lb.color } : {}"
              >
                {{ lb.name }}
                <button class="chip-remove" @click="removeLabel(lb.id)">×</button>
              </span>
            </div>
            <div class="label-add">
              <DxSelectBox
                :items="spaceLabels"
                display-expr="name"
                value-expr="id"
                v-model:value="selectedLabelId"
                placeholder="라벨 선택"
                :width="200"
              />
              <DxButton text="추가" type="default" @click="addLabel" />
            </div>
          </div>
          <CommentPanel :content-id="contentStore.currentContent?.id" />
        </div>
        <div v-else class="empty-state">
          <p>콘텐츠를 불러올 수 없습니다.</p>
        </div>
        <VersionHistoryPanel
          v-model:visible="showVersionHistory"
          :content-id="contentStore.currentContent?.id"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useContentStore } from '@/stores/content'
import AppHeader from '@/components/layout/AppHeader.vue'
import SpaceSidebar from '@/components/layout/SpaceSidebar.vue'
import TipTapEditor from '@/components/content/TipTapEditor.vue'
import VersionHistoryPanel from '@/components/content/VersionHistoryPanel.vue'
import CommentPanel from '@/components/content/CommentPanel.vue'
import AttachmentPanel from '@/components/content/AttachmentPanel.vue'
import { DxButton } from 'devextreme-vue/button'
import { DxLoadIndicator } from 'devextreme-vue/load-indicator'
import { DxSelectBox } from 'devextreme-vue/select-box'
import { labelApi } from '@/api/label'

const route = useRoute()
const router = useRouter()
const contentStore = useContentStore()

const spaceKey = route.params.spaceKey
const contentId = computed(() => route.params.contentId)
const loading = ref(true)
const showVersionHistory = ref(false)

// Label state
const labels = ref([])
const spaceLabels = ref([])
const selectedLabelId = ref(null)

const cid = computed(() => contentStore.currentContent?.id)
const sid = computed(() => contentStore.currentContent?.spaceId)

async function loadLabels() {
  if (!cid.value) return
  try {
    const res = await labelApi.getByContent(cid.value)
    labels.value = res.data.data || []
  } catch (err) {
    console.error('라벨 조회 실패:', err)
  }
}

async function loadSpaceLabels() {
  if (!sid.value) return
  try {
    const res = await labelApi.listBySpace(sid.value)
    spaceLabels.value = res.data.data || []
  } catch (err) {
    console.error('스페이스 라벨 조회 실패:', err)
  }
}

async function addLabel() {
  if (!selectedLabelId.value) return
  try {
    await labelApi.add(cid.value, selectedLabelId.value)
    selectedLabelId.value = null
    await loadLabels()
  } catch (err) {
    console.error('라벨 추가 실패:', err)
  }
}

async function removeLabel(labelId) {
  try {
    await labelApi.remove(cid.value, labelId)
    await loadLabels()
  } catch (err) {
    console.error('라벨 제거 실패:', err)
  }
}

async function loadContent() {
  loading.value = true
  await contentStore.fetchContent(contentId.value)
  loading.value = false
  await loadLabels()
  await loadSpaceLabels()
}

onMounted(loadContent)
watch(contentId, loadContent)

function goEdit() {
  router.push(`/spaces/${spaceKey}/contents/${contentId.value}/edit`)
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
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
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
  background: #FFFFFF;
}
.loading-area {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #6D7882;
  margin-top: 60px;
  font-size: 15px;
}
.content-body {
  max-width: 900px;
  margin: 0 auto;
}
.content-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.back-btn {
  background: none;
  border: none;
  color: #256EF4;
  font-size: 15px;
  cursor: pointer;
  padding: 4px 0;
  white-space: nowrap;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.back-btn:hover { color: #0B50D0; text-decoration: underline; }
.content-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #6D7882;
}
.content-status {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 400;
}
.content-status.published {
  background: #e6f4ea;
  color: #228738;
  border: 1px solid #b7dfbf;
}
.content-status.draft {
  background: #fff8e1;
  color: #a07000;
  border: 1px solid #ffe082;
}
.content-actions {
  display: flex;
  gap: 8px;
}
.content-title {
  font-size: 24px;
  font-weight: 700;
  color: #1E2124;
  margin: 0 0 24px 0;
  line-height: 1.3;
}
.empty-state {
  text-align: center;
  color: #6D7882;
  margin-top: 80px;
  font-size: 15px;
}
.label-panel {
  margin-top: 24px;
  border-top: 1px solid #E6E8EA;
  padding-top: 16px;
}
.label-title {
  font-size: 15px;
  font-weight: 700;
  color: #1E2124;
  margin: 0 0 8px 0;
}
.label-empty {
  font-size: 15px;
  color: #6D7882;
  margin-bottom: 12px;
}
.label-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.label-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 12px;
  background: #ECF2FE;
  border: 1px solid #256EF4;
  font-size: 13px;
  color: #0B50D0;
}
.chip-remove {
  cursor: pointer;
  color: #6D7882;
  background: none;
  border: none;
  font-size: 14px;
  line-height: 1;
  padding: 0 0 0 2px;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.chip-remove:hover {
  color: #DE3412;
}
.label-add {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

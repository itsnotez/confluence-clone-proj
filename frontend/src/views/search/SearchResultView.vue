<template>
  <div class="search-page">
    <AppHeader />
    <main class="search-main">
      <div v-if="searchStore.loading" class="loading-area">
        <DxLoadIndicator :visible="true" />
        <span>검색 중...</span>
      </div>
      <div v-else>
        <h2 class="search-heading">검색 결과: "{{ searchStore.query }}"</h2>
        <p class="result-count">{{ searchStore.results.length }}건</p>
        <div v-if="searchStore.results.length === 0" class="empty-state">
          검색 결과가 없습니다.
        </div>
        <ul v-else class="search-list">
          <li
            v-for="item in searchStore.results"
            :key="item.id"
            class="search-item"
            @click="goToContent(item)"
          >
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <span
                class="status-badge"
                :class="item.status?.toLowerCase()"
              >
                {{ item.status === 'PUBLISHED' ? '게시됨' : '임시저장' }}
              </span>
              <span class="item-date">{{ formatDate(item.updatedAt) }}</span>
            </div>
          </li>
        </ul>
      </div>
    </main>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSearchStore } from '@/stores/search'
import AppHeader from '@/components/layout/AppHeader.vue'
import { DxLoadIndicator } from 'devextreme-vue/load-indicator'

const route = useRoute()
const router = useRouter()
const searchStore = useSearchStore()

watch(
  () => route.query.q,
  (q) => searchStore.doSearch(q),
  { immediate: true }
)

function goToContent(item) {
  router.push(`/spaces/${item.spaceId}/contents/${item.id}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.search-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.search-main {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  background: white;
}
.loading-area {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #666;
  margin-top: 60px;
}
.search-heading {
  font-size: 1.4em;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}
.result-count {
  font-size: 13px;
  color: #888;
  margin: 0 0 20px 0;
}
.empty-state {
  text-align: center;
  color: #999;
  margin-top: 60px;
  font-size: 15px;
}
.search-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-width: 800px;
}
.search-item {
  padding: 14px 12px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background 0.15s;
}
.search-item:hover {
  background: #f5f7ff;
}
.item-title {
  font-size: 15px;
  font-weight: 500;
  color: #1976d2;
  margin-bottom: 6px;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #888;
}
.status-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}
.status-badge.published {
  background: #e8f5e9;
  color: #2e7d32;
}
.status-badge.draft {
  background: #fff8e1;
  color: #f57f17;
}
.item-date {
  color: #aaa;
}
</style>

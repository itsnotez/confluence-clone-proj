<template>
  <div class="search-page">
    <AppHeader />
    <main class="search-main">
      <div class="search-nav">
        <button class="back-btn" @click="router.back()">← 뒤로</button>
      </div>
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
  router.push(`/spaces/${item.spaceKey}/contents/${item.id}`)
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
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.search-main {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  background: #FFFFFF;
}
.search-nav {
  margin-bottom: 16px;
}
.back-btn {
  background: none;
  border: none;
  color: #256EF4;
  font-size: 15px;
  cursor: pointer;
  padding: 4px 0;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.back-btn:hover { color: #0B50D0; text-decoration: underline; }
.loading-area {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #6D7882;
  margin-top: 60px;
  font-size: 15px;
}
.search-heading {
  font-size: 24px;
  font-weight: 700;
  color: #1E2124;
  margin: 0 0 4px 0;
}
.result-count {
  font-size: 15px;
  color: #6D7882;
  margin: 0 0 24px 0;
}
.empty-state {
  text-align: center;
  color: #6D7882;
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
  padding: 16px 12px;
  border-bottom: 1px solid #E6E8EA;
  cursor: pointer;
  transition: background 0.15s;
}
.search-item:hover {
  background: #ECF2FE;
}
.item-title {
  font-size: 17px;
  font-weight: 700;
  color: #256EF4;
  margin-bottom: 6px;
}
.search-item:hover .item-title {
  color: #0B50D0;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #6D7882;
}
.status-badge {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 400;
}
.status-badge.published {
  background: #e6f4ea;
  color: #228738;
  border: 1px solid #b7dfbf;
}
.status-badge.draft {
  background: #fff8e1;
  color: #a07000;
  border: 1px solid #ffe082;
}
.item-date {
  color: #6D7882;
}
</style>

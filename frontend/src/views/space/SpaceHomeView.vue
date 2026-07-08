<template>
  <div class="space-home">
    <AppHeader />
    <div class="space-body">
      <SpaceSidebar :space-key="spaceKey" />
      <main class="main-content">
        <template v-if="spaceStore.currentSpace">
          <div class="space-header">
            <div class="space-title-row">
              <h2>{{ spaceStore.currentSpace.name }}</h2>
              <RouterLink
                v-if="auth.user?.role === 'SITE_ADMIN'"
                :to="`/spaces/${spaceKey}/permissions`"
                class="perm-link"
              >권한 설정</RouterLink>
            </div>
            <p v-if="spaceStore.currentSpace.description" class="space-desc">
              {{ spaceStore.currentSpace.description }}
            </p>
          </div>

          <div class="home-body">
            <!-- 좌측: 페이지 목차 -->
            <div class="toc-section">
              <h3 class="section-heading">
                <svg width="15" height="15" viewBox="0 0 16 16" fill="none" style="vertical-align:-2px;margin-right:6px">
                  <rect x="1" y="2" width="14" height="12" rx="2" stroke="currentColor" stroke-width="1.3"/>
                  <line x1="4" y1="5.5" x2="12" y2="5.5" stroke="currentColor" stroke-width="1.2"/>
                  <line x1="4" y1="8" x2="12" y2="8" stroke="currentColor" stroke-width="1.2"/>
                  <line x1="4" y1="10.5" x2="9" y2="10.5" stroke="currentColor" stroke-width="1.2"/>
                </svg>
                페이지 목차
                <span class="page-count">{{ totalPageCount }}페이지</span>
              </h3>
              <div v-if="sortedTree.length === 0" class="empty-hint">
                아직 페이지가 없습니다.
                <RouterLink v-if="canWrite" :to="`/spaces/${spaceKey}/contents/new`" class="empty-link">첫 페이지 만들기</RouterLink>
              </div>
              <div v-else class="toc-tree">
                <TocNode
                  v-for="node in sortedTree"
                  :key="node.id"
                  :node="node"
                  :depth="0"
                  :space-key="spaceKey"
                />
              </div>
            </div>

            <!-- 우측: 최근 게시물 -->
            <div class="recent-section">
              <h3 class="section-heading">
                <svg width="15" height="15" viewBox="0 0 16 16" fill="none" style="vertical-align:-2px;margin-right:6px">
                  <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.3"/>
                  <line x1="8" y1="4.5" x2="8" y2="8.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                  <line x1="8" y1="8.2" x2="10.5" y2="10.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
                최근 게시물
              </h3>
              <div v-if="recentPages.length === 0" class="empty-hint">게시물이 없습니다.</div>
              <div v-else class="recent-list">
                <div
                  v-for="page in recentPages"
                  :key="page.id"
                  class="recent-item"
                  @click="goToContent(page.id)"
                >
                  <div class="recent-item-title">
                    <svg width="12" height="12" viewBox="0 0 14 14" fill="none" style="flex-shrink:0;margin-top:1px">
                      <rect x="2" y="1" width="10" height="12" rx="1" stroke="#888" stroke-width="1.2"/>
                      <line x1="4" y1="4.5" x2="10" y2="4.5" stroke="#888" stroke-width="1"/>
                      <line x1="4" y1="6.5" x2="10" y2="6.5" stroke="#888" stroke-width="1"/>
                      <line x1="4" y1="8.5" x2="8"  y2="8.5" stroke="#888" stroke-width="1"/>
                    </svg>
                    <span>{{ page.title || '(제목 없음)' }}</span>
                  </div>
                  <div class="recent-item-meta">
                    <span v-if="page.createdBy?.name" class="meta-author">{{ page.createdBy.name }}</span>
                    <span class="meta-date">{{ formatDate(page.updatedAt) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="loading">스페이스 불러오는 중...</div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, defineComponent, h } from 'vue'
import { useRoute, RouterLink, useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/layout/AppHeader.vue'
import SpaceSidebar from '@/components/layout/SpaceSidebar.vue'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const auth = useAuthStore()

const canWrite = computed(() => {
  if (auth.user?.role === 'SITE_ADMIN') return true
  const p = spaceStore.mySpacePermission
  return p === 'WRITE' || p === 'SPACE_ADMIN'
})

const spaceKey = computed(() => route.params.spaceKey)

const sortedTree = computed(() =>
  [...(spaceStore.contentTree || [])].sort((a, b) => a.position - b.position)
)

// Flatten all nodes in the tree
function flattenTree(nodes) {
  const result = []
  for (const node of nodes) {
    result.push(node)
    if (node.children?.length) result.push(...flattenTree(node.children))
  }
  return result
}

const totalPageCount = computed(() => flattenTree(spaceStore.contentTree || []).length)

const recentPages = computed(() => {
  const flat = flattenTree(spaceStore.contentTree || [])
  return flat
    .filter(n => n.updatedAt)
    .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt))
    .slice(0, 10)
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now - d
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return '방금 전'
  if (diffMin < 60) return `${diffMin}분 전`
  if (diffHour < 24) return `${diffHour}시간 전`
  if (diffDay < 7) return `${diffDay}일 전`
  return d.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
}

// Recursive TOC node component
const TocNode = defineComponent({
  name: 'TocNode',
  props: {
    node: { type: Object, required: true },
    depth: { type: Number, default: 0 },
    spaceKey: { type: String, required: true }
  },
  setup(props) {
    const sorted = computed(() =>
      [...(props.node.children || [])].sort((a, b) => a.position - b.position)
    )
    return () => {
      const indent = props.depth * 20
      const rowEl = h('div', {
        class: 'toc-row',
        style: { paddingLeft: (indent + 8) + 'px' },
        onClick: () => router.push(`/spaces/${props.spaceKey}/contents/${props.node.id}`)
      }, [
        h('span', { class: 'toc-chevron' },
          sorted.value.length > 0
            ? h('svg', { width: 10, height: 10, viewBox: '0 0 10 10', fill: 'none' },
                h('path', { d: 'M2 3 L5 7 L8 3', stroke: '#888', 'stroke-width': '1.5', fill: 'none', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }))
            : h('span', { style: 'display:inline-block;width:10px' })
        ),
        h('svg', { class: 'toc-page-icon', width: 13, height: 13, viewBox: '0 0 14 14', fill: 'none' }, [
          h('rect', { x: 2, y: 1, width: 10, height: 12, rx: 1, stroke: '#888', 'stroke-width': '1.2' }),
          h('line', { x1: 4, y1: 4.5, x2: 10, y2: 4.5, stroke: '#888', 'stroke-width': 1 }),
          h('line', { x1: 4, y1: 6.5, x2: 10, y2: 6.5, stroke: '#888', 'stroke-width': 1 }),
          h('line', { x1: 4, y1: 8.5, x2: 8, y2: 8.5, stroke: '#888', 'stroke-width': 1 })
        ]),
        h('span', { class: 'toc-title' }, props.node.title || '(제목 없음)')
      ])

      const children = sorted.value.map(child =>
        h(TocNode, {
          key: child.id,
          node: child,
          depth: props.depth + 1,
          spaceKey: props.spaceKey
        })
      )

      return h('div', { class: 'toc-node' }, [rowEl, ...children])
    }
  }
})

function goToContent(id) {
  router.push(`/spaces/${spaceKey.value}/contents/${id}`)
}

async function loadSpace() {
  const key = spaceKey.value
  if (key) {
    await spaceStore.fetchSpace(key)
    await spaceStore.fetchContentTree(key)
  }
}

onMounted(loadSpace)
watch(spaceKey, loadSpace)
</script>

<style scoped>
.space-home {
  min-height: 100vh;
  background: #FFFFFF;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.space-body {
  display: flex;
  height: calc(100vh - 56px);
}
.main-content {
  flex: 1;
  padding: 32px 36px;
  overflow-y: auto;
  min-width: 0;
}
.loading {
  color: #6D7882;
  font-size: 15px;
  padding-top: 40px;
}

/* Header */
.space-header {
  border-bottom: 1px solid #E6E8EA;
  padding-bottom: 18px;
  margin-bottom: 24px;
}
.space-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 6px;
}
.space-title-row h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1E2124;
}
.space-desc {
  color: #464C53;
  font-size: 15px;
  margin: 0;
}
.perm-link {
  font-size: 13px;
  color: #256EF4;
  text-decoration: none;
  border: 1px solid #256EF4;
  padding: 4px 12px;
  border-radius: 6px;
  white-space: nowrap;
  background: transparent;
}
.perm-link:hover {
  background: #ECF2FE;
  color: #0B50D0;
  border-color: #0B50D0;
}

/* 2-column layout */
.home-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

/* Section shared */
.section-heading {
  font-size: 17px;
  font-weight: 700;
  color: #1E2124;
  margin: 0 0 14px;
  display: flex;
  align-items: center;
}
.page-count {
  margin-left: auto;
  font-size: 13px;
  font-weight: 400;
  color: #6D7882;
  background: #F4F5F6;
  padding: 2px 8px;
  border-radius: 10px;
}
.empty-hint {
  font-size: 15px;
  color: #6D7882;
  padding: 16px 0;
}
.empty-link {
  margin-left: 6px;
  color: #256EF4;
  font-size: 15px;
}
.empty-link:hover { color: #0B50D0; }

/* TOC */
.toc-section {
  min-width: 0;
}
.toc-tree {
  border: 1px solid #B1B8BE;
  border-radius: 8px;
  overflow: hidden;
  background: #FFFFFF;
}

/* Recent list */
.recent-section {
  min-width: 0;
}
.recent-list {
  border: 1px solid #B1B8BE;
  border-radius: 8px;
  overflow: hidden;
  background: #FFFFFF;
}
.recent-item {
  padding: 10px 14px;
  border-bottom: 1px solid #E6E8EA;
  cursor: pointer;
  transition: background 0.1s;
}
.recent-item:last-child { border-bottom: none; }
.recent-item:hover { background: #ECF2FE; }
.recent-item-title {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 15px;
  color: #1E2124;
  font-weight: 400;
  margin-bottom: 4px;
  overflow: hidden;
}
.recent-item-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-item:hover .recent-item-title span { color: #256EF4; }
.recent-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6D7882;
}
.meta-author {
  color: #464C53;
  font-weight: 400;
}
</style>

<style>
/* Global styles for TocNode (render function component) */
.toc-node { user-select: none; }
.toc-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  padding-bottom: 8px;
  padding-right: 16px;
  cursor: pointer;
  font-size: 15px;
  color: #1E2124;
  border-bottom: 1px solid #E6E8EA;
  transition: background 0.1s;
  font-family: "Pretendard GOV", "Pretendard", -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Noto Sans KR", sans-serif;
}
.toc-row:hover { background: #ECF2FE; color: #256EF4; }
.toc-row:hover .toc-page-icon rect,
.toc-row:hover .toc-page-icon line { stroke: #256EF4; }
.toc-chevron {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  flex-shrink: 0;
  color: #6D7882;
}
.toc-page-icon { flex-shrink: 0; }
.toc-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toc-node:last-child > .toc-row { border-bottom: none; }
</style>

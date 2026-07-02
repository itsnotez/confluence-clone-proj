<template>
  <div class="space-home">
    <AppHeader />
    <div class="space-body">
      <SpaceSidebar :space-key="spaceKey" />
      <main class="main-content">
        <div v-if="spaceStore.currentSpace" class="space-info">
          <h2>{{ spaceStore.currentSpace.name }}</h2>
          <p v-if="spaceStore.currentSpace.description" class="space-desc">
            {{ spaceStore.currentSpace.description }}
          </p>
        </div>
        <div v-else class="space-info">
          <h2>스페이스 로딩 중...</h2>
        </div>
        <div class="recent-section">
          <h3>최근 콘텐츠</h3>
          <DxList
            :data-source="recentItems"
            :height="'auto'"
            key-expr="id"
            display-expr="title"
            :no-data-text="'콘텐츠가 없습니다.'"
            @item-click="onContentClick"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import AppHeader from '@/components/layout/AppHeader.vue'
import SpaceSidebar from '@/components/layout/SpaceSidebar.vue'
import { DxList } from 'devextreme-vue/list'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()

const spaceKey = computed(() => route.params.spaceKey)

const recentItems = computed(() => {
  return (spaceStore.contentTree || []).slice(0, 10)
})

async function loadSpace() {
  const key = spaceKey.value
  if (key) {
    await spaceStore.fetchSpace(key)
    await spaceStore.fetchContentTree(key)
  }
}

onMounted(loadSpace)
watch(spaceKey, loadSpace)

function onContentClick(e) {
  const item = e.itemData
  if (item && item.id) {
    router.push(`/spaces/${spaceKey.value}/contents/${item.id}`)
  }
}
</script>

<style scoped>
.space-home {
  min-height: 100vh;
  background: #fff;
}
.space-body {
  display: flex;
  height: calc(100vh - 56px);
}
.main-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}
.space-info h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #222;
}
.space-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 24px;
}
.recent-section h3 {
  font-size: 16px;
  color: #444;
  margin-bottom: 12px;
}
</style>

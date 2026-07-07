<template>
  <DxPopup
    :visible="visible"
    title="버전 기록"
    :width="420"
    :height="'80%'"
    :show-close-button="true"
    :drag-enabled="true"
    :position="panelPosition"
    @hidden="onHidden"
  >
    <div class="version-panel">
      <div v-if="loading" class="panel-loading">
        <DxLoadIndicator :visible="true" />
        <span>로딩 중...</span>
      </div>
      <div v-else-if="versions.length === 0" class="panel-empty">
        버전 기록이 없습니다.
      </div>
      <ul v-else class="version-list">
        <li
          v-for="ver in versions"
          :key="ver.versionNo"
          class="version-item"
        >
          <div class="version-header">
            <span class="version-no">v{{ ver.versionNo }}</span>
            <span class="version-date">{{ formatDate(ver.createdAt) }}</span>
          </div>
          <div class="version-author">
            {{ ver.createdBy?.name || ver.createdBy || '알 수 없음' }}
          </div>
        </li>
      </ul>
    </div>
  </DxPopup>
</template>

<script setup>
import { ref, watch } from 'vue'
import { DxPopup } from 'devextreme-vue/popup'
import { DxLoadIndicator } from 'devextreme-vue/load-indicator'
import contentApi from '@/api/content'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  contentId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:visible'])

const versions = ref([])
const loading = ref(false)

const panelPosition = {
  at: 'right center',
  my: 'right center',
  of: window
}

async function loadVersions() {
  if (!props.contentId) return
  loading.value = true
  try {
    const res = await contentApi.getVersions(props.contentId)
    versions.value = res.data.data || []
  } catch (err) {
    console.error('버전 조회 실패:', err)
    versions.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) loadVersions()
})

function onHidden() {
  emit('update:visible', false)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>

<style scoped>
.version-panel {
  padding: 8px 0;
}
.panel-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  color: #666;
}
.panel-empty {
  text-align: center;
  color: #999;
  padding: 40px 16px;
  font-size: 14px;
}
.version-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.version-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.version-item:hover {
  background: #f5f5f5;
}
.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.version-no {
  font-weight: 600;
  color: #1976d2;
  font-size: 14px;
}
.version-date {
  font-size: 12px;
  color: #999;
}
.version-author {
  font-size: 13px;
  color: #555;
}
</style>

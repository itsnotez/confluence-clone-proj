<template>
  <div v-if="!readonly || loading || attachments.length > 0" class="attachment-panel">
    <h3 class="panel-title">첨부파일</h3>
    <input type="file" ref="fileInputRef" style="display:none" @change="onFileChange" />
    <div v-if="!readonly" class="panel-toolbar">
      <DxButton text="파일 업로드" type="default" styling-mode="outlined" :disabled="uploading" @click="triggerUpload" />
      <span v-if="uploading" class="uploading-text">업로드 중...</span>
    </div>
    <div v-if="loading" class="loading-area">
      <span>로딩 중...</span>
    </div>
    <div v-else class="attachment-list">
      <p v-if="attachments.length === 0" class="empty-state">첨부파일이 없습니다.</p>
      <div v-for="att in attachments" :key="att.id" class="attachment-item">
        <div class="attachment-info">
          <span class="file-name">{{ att.fileName }}</span>
          <span class="file-meta">{{ formatSize(att.fileSize) }} · {{ formatDate(att.createdAt) }}</span>
        </div>
        <DxButton text="다운로드" type="normal" styling-mode="outlined" @click="openDownload(att.id)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { attachmentApi } from '@/api/attachment'
import { DxButton } from 'devextreme-vue/button'

const props = defineProps({
  contentId: { type: Number, default: null },
  readonly: { type: Boolean, default: false }
})

const attachments = ref([])
const loading = ref(false)
const uploading = ref(false)
const fileInputRef = ref(null)

async function loadAttachments() {
  if (!props.contentId) return
  loading.value = true
  try {
    const res = await attachmentApi.list(props.contentId)
    attachments.value = res.data.data || []
  } catch (err) {
    console.error('첨부파일 조회 실패:', err)
  } finally {
    loading.value = false
  }
}

async function onFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    await attachmentApi.upload(props.contentId, file)
    await loadAttachments()
  } catch (err) {
    console.error('업로드 실패:', err)
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

function triggerUpload() {
  fileInputRef.value?.click()
}

function openDownload(id) {
  window.open(attachmentApi.download(id), '_blank')
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let n = bytes
  while (n >= 1024 && i < units.length - 1) { n /= 1024; i++ }
  return `${n.toFixed(1)} ${units[i]}`
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric'
  })
}

onMounted(() => loadAttachments())
watch(() => props.contentId, () => loadAttachments())
</script>

<style scoped>
.attachment-panel {
  margin-top: 32px;
  border-top: 1px solid #eee;
  padding-top: 20px;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #333;
}
.panel-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.uploading-text {
  font-size: 13px;
  color: #1976d2;
}
.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.attachment-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.file-meta {
  font-size: 12px;
  color: #999;
}
.empty-state {
  color: #999;
  font-size: 14px;
  padding: 16px 0;
}
.loading-area {
  color: #666;
  padding: 16px 0;
}
</style>

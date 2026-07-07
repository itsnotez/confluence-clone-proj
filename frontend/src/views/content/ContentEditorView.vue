<template>
  <div class="editor-page">
    <AppHeader />
    <div class="editor-toolbar">
      <DxTextBox
        v-model:value="title"
        placeholder="제목을 입력하세요"
        :width="500"
        class="title-input"
      />
      <div class="editor-actions">
        <DxButton
          text="임시저장"
          type="normal"
          styling-mode="outlined"
          :disabled="saving"
          @click="handleSave"
        />
        <DxButton
          text="게시"
          type="default"
          :disabled="saving"
          @click="handlePublish"
        />
      </div>
    </div>
    <div class="editor-body">
      <TipTapEditor v-model="body" :readonly="false" />
      <div class="attachment-section">
        <AttachmentPanel v-if="currentContentId" :content-id="currentContentId" />
        <div v-else class="attachment-pending">
          첨부파일은 저장 후 등록할 수 있습니다.
        </div>
      </div>
    </div>
    <DxToast
      v-model:visible="toastVisible"
      :message="toastMessage"
      :type="toastType"
      :display-time="2500"
      position="bottom center"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useContentStore } from '@/stores/content'
import AppHeader from '@/components/layout/AppHeader.vue'
import TipTapEditor from '@/components/content/TipTapEditor.vue'
import AttachmentPanel from '@/components/content/AttachmentPanel.vue'
import { DxTextBox } from 'devextreme-vue/text-box'
import { DxButton } from 'devextreme-vue/button'
import { DxToast } from 'devextreme-vue/toast'

const route = useRoute()
const router = useRouter()
const contentStore = useContentStore()

const spaceKey = route.params.spaceKey
const routeContentId = route.params.contentId
const isNew = !routeContentId || routeContentId === 'new'
const parentId = route.query.parentId ? Number(route.query.parentId) : null

const title = ref('')
const body = ref('')
const saving = ref(false)
const currentContentId = ref(isNew ? null : routeContentId)

const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref('success')

function showToast(message, type = 'success') {
  toastMessage.value = message
  toastType.value = type
  toastVisible.value = true
}

onMounted(async () => {
  if (!isNew) {
    await contentStore.fetchContent(routeContentId)
    if (contentStore.currentContent) {
      title.value = contentStore.currentContent.title || ''
      body.value = contentStore.currentContent.body || ''
    }
  }
})

async function handleSave() {
  if (!title.value.trim()) {
    showToast('제목을 입력해주세요.', 'error')
    return
  }
  saving.value = true
  try {
    if (isNew || !currentContentId.value) {
      const created = await contentStore.createContent(spaceKey, {
        title: title.value,
        body: body.value,
        type: 'PAGE',
        ...(parentId !== null && { parentId })
      })
      currentContentId.value = created.id
      router.replace(`/spaces/${spaceKey}/contents/${created.id}/edit`)
    } else {
      await contentStore.saveContent(currentContentId.value, body.value)
    }
    showToast('저장되었습니다.')
  } catch {
    showToast('저장에 실패했습니다.', 'error')
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  if (!title.value.trim()) {
    showToast('제목을 입력해주세요.', 'error')
    return
  }
  saving.value = true
  try {
    let targetId = currentContentId.value
    if (isNew || !targetId) {
      const created = await contentStore.createContent(spaceKey, {
        title: title.value,
        body: body.value,
        type: 'PAGE',
        ...(parentId !== null && { parentId })
      })
      targetId = created.id
      currentContentId.value = targetId
    }
    await contentStore.publishContent(targetId, body.value)
    showToast('게시되었습니다.')
    setTimeout(() => {
      router.push(`/spaces/${spaceKey}/contents/${targetId}`)
    }, 1000)
  } catch {
    showToast('게시에 실패했습니다.', 'error')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: white;
}
.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
  gap: 16px;
}
.title-input {
  flex: 1;
}
.editor-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.editor-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.attachment-section {
  max-width: 860px;
  margin: 0 auto;
}
.attachment-pending {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  font-size: 13px;
  color: #aaa;
}
</style>

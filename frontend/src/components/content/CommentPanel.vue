<template>
  <div class="comment-panel">
    <h3 class="panel-title">댓글</h3>
    <div v-if="loading" class="loading-area">
      <span>로딩 중...</span>
    </div>
    <div v-else class="comment-list">
      <p v-if="comments.length === 0" class="empty-state">댓글이 없습니다.</p>
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <span class="comment-author">{{ comment.createdBy?.name }}</span>
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
        </div>
        <p class="comment-body">{{ comment.body }}</p>
        <div class="comment-actions">
          <DxButton text="답글" type="normal" styling-mode="text" @click="setReply(comment)" />
          <DxButton v-if="isOwn(comment)" text="삭제" type="danger" styling-mode="text" @click="removeComment(comment.id)" />
        </div>
        <div v-if="comment.children?.length" class="replies">
          <div v-for="reply in comment.children" :key="reply.id" class="reply-item">
            <div class="comment-header">
              <span class="comment-author">{{ reply.createdBy?.name }}</span>
              <span class="comment-date">{{ formatDate(reply.createdAt) }}</span>
            </div>
            <p class="comment-body">{{ reply.body }}</p>
            <div class="comment-actions">
              <DxButton v-if="isOwn(reply)" text="삭제" type="danger" styling-mode="text" @click="removeComment(reply.id)" />
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="comment-form">
      <div v-if="replyTargetId" class="reply-indicator">
        {{ replyTargetLabel }}에게 답글
        <DxButton text="취소" type="normal" styling-mode="text" @click="clearReply" />
      </div>
      <DxTextArea v-model:value="newBody" placeholder="댓글을 입력하세요..." :min-height="80" />
      <DxButton text="작성" type="default" @click="submitComment" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { commentApi } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'
import { DxButton } from 'devextreme-vue/button'
import { DxTextArea } from 'devextreme-vue/text-area'

const props = defineProps({
  contentId: { type: Number, default: null }
})

const authStore = useAuthStore()
const comments = ref([])
const loading = ref(false)
const newBody = ref('')
const replyTargetId = ref(null)
const replyTargetLabel = ref('')

async function loadComments() {
  if (!props.contentId) return
  loading.value = true
  try {
    const res = await commentApi.list(props.contentId)
    comments.value = res.data.data || []
  } catch (err) {
    console.error('댓글 조회 실패:', err)
  } finally {
    loading.value = false
  }
}

function setReply(comment) {
  replyTargetId.value = comment.id
  replyTargetLabel.value = comment.createdBy?.name || '?'
}

function clearReply() {
  replyTargetId.value = null
  replyTargetLabel.value = ''
}

async function submitComment() {
  if (!newBody.value.trim()) return
  const data = { body: newBody.value.trim() }
  if (replyTargetId.value) data.parentCommentId = replyTargetId.value
  try {
    await commentApi.create(props.contentId, data)
    newBody.value = ''
    clearReply()
    await loadComments()
  } catch (err) {
    console.error('댓글 작성 실패:', err)
  }
}

async function removeComment(id) {
  try {
    await commentApi.remove(id)
    await loadComments()
  } catch (err) {
    console.error('댓글 삭제 실패:', err)
  }
}

function isOwn(comment) {
  return authStore.user?.id === comment.createdBy?.id
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric'
  })
}

onMounted(() => loadComments())
watch(() => props.contentId, () => loadComments())
</script>

<style scoped>
.comment-panel {
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
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.comment-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
  color: #666;
}
.comment-author {
  font-weight: 500;
  color: #333;
}
.comment-body {
  font-size: 14px;
  color: #444;
  margin: 4px 0;
}
.replies {
  margin-left: 24px;
  border-left: 2px solid #e8e8e8;
  padding-left: 12px;
}
.reply-item {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.comment-form {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reply-indicator {
  font-size: 13px;
  color: #1976d2;
  display: flex;
  align-items: center;
  gap: 8px;
}
.empty-state {
  color: #999;
  font-size: 14px;
  padding: 16px 0;
}
.comment-actions {
  display: flex;
  gap: 4px;
}
.loading-area {
  color: #666;
  padding: 16px 0;
}
</style>

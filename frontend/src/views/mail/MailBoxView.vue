<template>
  <div class="mailbox-page">
    <AppHeader />
    <div class="mailbox-body">
      <SpaceSidebar :space-key="spaceKey" />
      <div class="mailbox-content">
        <div class="mailbox-container">
          <!-- 좌측: 계정 선택 + 메시지 목록 -->
          <div class="message-list-panel">
            <div class="panel-header">
              <h2>메일함</h2>
              <DxSelectBox
                :items="mailStore.accounts"
                display-expr="emailAddress"
                value-expr="id"
                v-model:value="selectedAccountId"
                placeholder="메일 계정 선택"
                style="flex:1;max-width:360px"
              />
            </div>

            <div v-if="mailStore.loading" style="padding:20px;text-align:center;color:#666">
              불러오는 중...
            </div>
            <div v-else-if="mailStore.accounts.length === 0" style="padding:20px;color:#999">
              연결된 메일 계정이 없습니다.
            </div>
            <DxDataGrid
              v-else
              :data-source="mailStore.messages"
              :show-borders="true"
              :hover-state-enabled="true"
              :row-alternation-enabled="true"
              @row-click="onRowClick"
              height="calc(100vh - 220px)"
            >
              <DxSelection mode="single" />
              <DxColumn data-field="subject" caption="제목" />
              <DxColumn data-field="fromAddress" caption="발신자" :width="200" />
              <DxColumn data-field="receivedAt" caption="수신일" data-type="datetime" :width="160" />
              <DxColumn data-field="status" caption="상태" :width="90" cell-template="statusTemplate" />
              <template #statusTemplate="{ data }">
                <span :class="['status-badge', data.value === 'CONVERTED' ? 'status-converted' : 'status-new']">
                  {{ data.value === 'CONVERTED' ? '변환됨' : '미변환' }}
                </span>
              </template>
              <DxPaging :page-size="20" />
            </DxDataGrid>
          </div>

          <!-- 우측: 미리보기 패널 -->
          <div v-if="showPreview && mailStore.selectedMessage" class="preview-panel">
            <div class="preview-header">
              <h3 class="preview-title">{{ mailStore.selectedMessage.subject }}</h3>
              <DxButton icon="close" styling-mode="text" @click="showPreview = false" />
            </div>
            <div class="preview-meta">
              <p><strong>발신:</strong> {{ mailStore.selectedMessage.fromAddress }}</p>
              <p><strong>수신:</strong> {{ formatDate(mailStore.selectedMessage.receivedAt) }}</p>
            </div>
            <hr style="border:none;border-top:1px solid #e0e0e0;margin:12px 0" />
            <div class="preview-body">{{ mailStore.selectedMessage.bodyPreview }}</div>
            <hr style="border:none;border-top:1px solid #e0e0e0;margin:12px 0" />
            <div class="preview-actions">
              <DxButton
                v-if="!mailStore.selectedMessage.linkedContentId"
                text="Wiki 페이지로 변환"
                type="default"
                styling-mode="contained"
                :disabled="converting"
                @click="handleConvert(mailStore.selectedMessage)"
              />
              <p v-else class="converted-notice">
                변환 완료 (페이지 ID: {{ mailStore.selectedMessage.linkedContentId }})
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMailStore } from '@/stores/mail'
import AppHeader from '@/components/layout/AppHeader.vue'
import SpaceSidebar from '@/components/layout/SpaceSidebar.vue'
import { DxDataGrid, DxColumn, DxPaging, DxSelection } from 'devextreme-vue/data-grid'
import { DxButton } from 'devextreme-vue/button'
import { DxSelectBox } from 'devextreme-vue/select-box'
import notify from 'devextreme/ui/notify'

const route = useRoute()
const mailStore = useMailStore()

const spaceKey = computed(() => route.params.spaceKey)
const selectedAccountId = ref(null)
const showPreview = ref(false)
const converting = ref(false)

onMounted(async () => {
  await mailStore.fetchAccounts(spaceKey.value)
  if (mailStore.accounts.length > 0) {
    selectedAccountId.value = mailStore.accounts[0].id
  }
})

watch(selectedAccountId, async (id) => {
  if (id) {
    showPreview.value = false
    await mailStore.fetchMessages(spaceKey.value, id)
  }
})

function onRowClick({ data }) {
  mailStore.selectMessage(data)
  showPreview.value = true
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('ko-KR')
}

async function handleConvert(msg) {
  converting.value = true
  try {
    const result = await mailStore.convertToPage(spaceKey.value, selectedAccountId.value, msg.id)
    notify({ message: result.message || '페이지 변환 성공', type: 'success', displayTime: 3000 })
  } catch (e) {
    const errMsg = e.response?.status === 409 ? '이미 변환된 메일입니다' : '변환 실패'
    notify({ message: errMsg, type: 'error', displayTime: 3000 })
  } finally {
    converting.value = false
  }
}
</script>

<style scoped>
.mailbox-page {
  min-height: 100vh;
  background: #fafafa;
}
.mailbox-body {
  display: flex;
  height: calc(100vh - 56px);
}
.mailbox-content {
  flex: 1;
  overflow: hidden;
  padding: 20px;
}
.mailbox-container {
  display: flex;
  gap: 16px;
  height: 100%;
}
.message-list-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.panel-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
  white-space: nowrap;
}
.preview-panel {
  width: 400px;
  min-width: 360px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
  background: #fff;
}
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.preview-title {
  margin: 0;
  font-size: 15px;
  line-height: 1.4;
  flex: 1;
  word-break: break-all;
}
.preview-meta p {
  margin: 4px 0;
  font-size: 12px;
  color: #666;
}
.preview-body {
  font-size: 13px;
  white-space: pre-wrap;
  color: #444;
  max-height: 300px;
  overflow-y: auto;
}
.preview-actions {
  margin-top: 12px;
}
.converted-notice {
  color: #2e7d32;
  font-size: 13px;
  margin: 0;
}
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.status-new {
  background: #e3f2fd;
  color: #1565c0;
}
.status-converted {
  background: #e8f5e9;
  color: #2e7d32;
}
</style>

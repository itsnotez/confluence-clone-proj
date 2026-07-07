<template>
  <div class="admin-page">
    <AppHeader />
    <div class="admin-header">
      <h1>관리자 대시보드</h1>
    </div>

    <div class="admin-content">
      <DxTabPanel :animation-enabled="false">
        <DxItem title="대시보드">
          <template #default>
            <div class="tab-content">
              <div v-if="loading" class="loading-message">로딩 중...</div>
              <div v-else-if="stats">
                <!-- 통계 카드 4개 -->
                <div class="stats-grid">
                  <div class="stat-card">
                    <div class="stat-label">활성 사용자</div>
                    <div class="stat-value">{{ stats.activeUsers }}</div>
                  </div>
                  <div class="stat-card">
                    <div class="stat-label">Space 수</div>
                    <div class="stat-value">{{ stats.totalSpaces }}</div>
                  </div>
                  <div class="stat-card">
                    <div class="stat-label">콘텐츠 수</div>
                    <div class="stat-value">{{ stats.totalContents }}</div>
                  </div>
                  <div class="stat-card">
                    <div class="stat-label">스토리지 사용량</div>
                    <div class="stat-value">{{ formatBytes(stats.storageUsedBytes) }}</div>
                  </div>
                </div>

                <!-- 메일 서버 상태 파이 차트 -->
                <div class="chart-section">
                  <h3 class="chart-title">메일 서버 상태</h3>
                  <DxPieChart
                    :data-source="mailStatusData"
                    palette="Bright"
                    :size="{ width: 400, height: 300 }"
                  >
                    <DxSeries
                      argument-field="status"
                      value-field="count"
                      :label="{ visible: true, connector: { enabled: true } }"
                    />
                  </DxPieChart>
                </div>
              </div>
              <div v-else class="no-data">데이터를 불러올 수 없습니다.</div>
            </div>
          </template>
        </DxItem>

        <DxItem title="유저 관리">
          <template #default>
            <div class="tab-content">
              <div class="user-toolbar">
                <DxButton text="+ 사용자 추가" type="default" @click="openCreateDialog" />
              </div>
              <DxDataGrid
                :data-source="users"
                :show-borders="true"
                :hover-state-enabled="true"
                :row-alternation-enabled="true"
                height="calc(100vh - 260px)"
              >
                <DxSearchPanel :visible="true" placeholder="검색..." />
                <DxColumn data-field="loginId" caption="로그인 ID" :width="140" />
                <DxColumn data-field="name" caption="이름" :width="120" />
                <DxColumn data-field="email" caption="이메일" />
                <DxColumn
                  caption="역할"
                  :calculate-cell-value="(row) => row.role === 'SITE_ADMIN' ? '관리자' : '일반'"
                  :width="80"
                  alignment="center"
                />
                <DxColumn
                  caption="상태"
                  :calculate-cell-value="(row) => row.status === 'ACTIVE' ? '활성' : '비활성'"
                  :width="80"
                  alignment="center"
                  cell-template="statusCell"
                />
                <DxColumn caption="생성일" data-field="createdAt" data-type="date" :width="120" />
                <DxColumn caption="관리" cell-template="actionCell" :width="160" alignment="center" />
                <template #statusCell="{ data }">
                  <span :class="['status-badge', data.value === '활성' ? 'active' : 'inactive']">
                    {{ data.value }}
                  </span>
                </template>
                <template #actionCell="{ data }">
                  <div class="action-btns">
                    <DxButton text="수정" type="normal" styling-mode="outlined" @click="openEditDialog(data.data)" />
                    <DxButton
                      :text="data.data.status === 'ACTIVE' ? '비활성화' : '활성화'"
                      type="normal"
                      styling-mode="outlined"
                      @click="toggleStatus(data.data)"
                    />
                  </div>
                </template>
                <DxPaging :page-size="20" />
              </DxDataGrid>
            </div>
          </template>
        </DxItem>

        <DxItem title="메일 계정 관리">
          <template #default>
            <div class="tab-content">
              <div class="mail-toolbar">
                <div class="mail-space-selector">
                  <label>스페이스 선택</label>
                  <DxSelectBox
                    v-model:value="mailSelectedSpaceKey"
                    :items="spaceList"
                    display-expr="name"
                    value-expr="spaceKey"
                    placeholder="스페이스를 선택하세요"
                    :width="260"
                    @value-changed="onSpaceSelected"
                  />
                </div>
                <DxButton
                  text="+ 계정 추가"
                  type="default"
                  :disabled="!mailSelectedSpaceKey"
                  @click="openMailAccountDialog"
                />
              </div>

              <div v-if="!mailSelectedSpaceKey" class="no-data">스페이스를 선택하면 메일 계정 목록이 표시됩니다.</div>
              <div v-else-if="mailAccountsLoading" class="loading-message">로딩 중...</div>
              <div v-else>
                <DxDataGrid
                  :data-source="mailAccounts"
                  :show-borders="true"
                  :hover-state-enabled="true"
                  :row-alternation-enabled="true"
                  height="calc(100vh - 320px)"
                  :no-data-text="'등록된 메일 계정이 없습니다.'"
                >
                  <DxColumn data-field="emailAddress" caption="이메일 주소" />
                  <DxColumn data-field="imapHost" caption="IMAP 호스트" :width="180" />
                  <DxColumn data-field="imapPort" caption="포트" :width="70" alignment="center" />
                  <DxColumn
                    caption="SSL"
                    :width="60"
                    alignment="center"
                    :calculate-cell-value="(row) => row.imapSsl ? '✓' : '✗'"
                  />
                  <DxColumn
                    data-field="syncStatus"
                    caption="동기화 상태"
                    :width="110"
                    alignment="center"
                    cell-template="syncStatusCell"
                  />
                  <DxColumn caption="오류 내용" data-field="lastErrorMessage" cell-template="errorMsgCell" />
                  <DxColumn data-field="lastSyncedAt" caption="마지막 동기화" data-type="datetime" :width="170" />
                  <DxColumn caption="관리" :width="160" alignment="center" cell-template="mailActionCell" />
                  <template #syncStatusCell="{ data }">
                    <span :class="['status-badge', syncBadgeClass(data.value)]">
                      {{ syncStatusLabel(data.value) }}
                    </span>
                  </template>
                  <template #errorMsgCell="{ data }">
                    <span v-if="data.value" class="error-msg" :title="data.value">{{ data.value }}</span>
                    <span v-else class="no-error">-</span>
                  </template>
                  <template #mailActionCell="{ data }">
                    <div class="action-btns">
                      <DxButton
                        text="동기화"
                        type="normal"
                        styling-mode="outlined"
                        :disabled="syncingIds.has(data.data.id)"
                        @click="triggerSync(data.data)"
                      />
                      <DxButton
                        text="삭제"
                        type="danger"
                        styling-mode="outlined"
                        @click="deleteMailAccount(data.data)"
                      />
                    </div>
                  </template>
                  <DxPaging :page-size="20" />
                </DxDataGrid>
              </div>
            </div>
          </template>
        </DxItem>

        <DxItem title="감사로그">
          <template #default>
            <div class="tab-content">
              <DxDataGrid
                :data-source="auditLogs"
                :show-borders="true"
                :hover-state-enabled="true"
                :row-alternation-enabled="true"
                height="calc(100vh - 220px)"
              >
                <DxSearchPanel :visible="true" placeholder="검색..." />
                <DxFilterRow :visible="true" />
                <DxColumn data-field="actorId" caption="사용자 ID" :width="100" alignment="center" />
                <DxColumn
                  caption="액션"
                  :calculate-cell-value="(row) => ACTION_LABELS[row.actionType] || row.actionType"
                  :width="160"
                />
                <DxColumn
                  caption="대상"
                  :calculate-cell-value="(row) => formatTarget(row)"
                  :width="160"
                />
                <DxColumn
                  caption="설명"
                  :calculate-cell-value="(row) => parseDetail(row.detail)"
                />
                <DxColumn data-field="createdAt" caption="시각" data-type="datetime" :width="180" />
                <DxPaging :page-size="20" />
              </DxDataGrid>
            </div>
          </template>
        </DxItem>
      </DxTabPanel>
    </div>

    <!-- 사용자 추가/수정 다이얼로그 -->
    <DxPopup
      v-model:visible="dialogVisible"
      :title="editingUser ? '사용자 수정' : '사용자 추가'"
      :width="440"
      :height="'auto'"
      :show-close-button="true"
    >
      <div class="dialog-form">
        <div v-if="!editingUser" class="form-row">
          <label>로그인 ID</label>
          <DxTextBox v-model:value="form.loginId" placeholder="예: hong.gildong" />
        </div>
        <div class="form-row">
          <label>이름</label>
          <DxTextBox v-model:value="form.name" placeholder="홍길동" />
        </div>
        <div class="form-row">
          <label>이메일</label>
          <DxTextBox v-model:value="form.email" placeholder="user@company.com" />
        </div>
        <div class="form-row">
          <label>비밀번호{{ editingUser ? ' (변경 시에만 입력)' : '' }}</label>
          <DxTextBox v-model:value="form.password" mode="password" :placeholder="editingUser ? '변경할 경우만 입력 (8자 이상)' : '8자 이상'" />
        </div>
        <div class="form-row">
          <label>역할</label>
          <DxSelectBox
            v-model:value="form.role"
            :items="roleOptions"
            display-expr="label"
            value-expr="value"
          />
        </div>
        <div class="form-error" v-if="formError">{{ formError }}</div>
        <div class="form-actions">
          <DxButton text="취소" type="normal" styling-mode="outlined" @click="dialogVisible = false" />
          <DxButton :text="editingUser ? '저장' : '추가'" type="default" :disabled="submitting" @click="submitForm" />
        </div>
      </div>
    </DxPopup>
  </div>

  <!-- 메일 계정 추가 다이얼로그 -->
  <DxPopup
    v-model:visible="mailAccountDialogVisible"
    title="메일 계정 추가"
    :width="460"
    :height="'auto'"
    :show-close-button="true"
  >
    <div class="dialog-form">
      <div class="form-row">
        <label>이메일 주소</label>
        <DxTextBox v-model:value="mailForm.emailAddress" placeholder="example@company.com" />
      </div>
      <div class="form-row">
        <label>비밀번호</label>
        <DxTextBox v-model:value="mailForm.password" mode="password" placeholder="앱 비밀번호 또는 계정 비밀번호" />
      </div>
      <div class="form-row">
        <label>IMAP 호스트</label>
        <DxTextBox v-model:value="mailForm.imapHost" placeholder="imap.gmail.com" />
      </div>
      <div class="form-row form-row-inline">
        <div class="form-row" style="flex:1">
          <label>IMAP 포트</label>
          <DxTextBox v-model:value="mailForm.imapPort" placeholder="993" />
        </div>
        <div class="form-row form-row-check">
          <label>SSL 사용</label>
          <DxCheckBox v-model:value="mailForm.imapSsl" />
        </div>
      </div>
      <div class="form-row">
        <label>SMTP 호스트 <span class="optional">(선택)</span></label>
        <DxTextBox v-model:value="mailForm.smtpHost" placeholder="smtp.gmail.com" />
      </div>
      <div class="form-row">
        <label>SMTP 포트 <span class="optional">(선택)</span></label>
        <DxTextBox v-model:value="mailForm.smtpPort" placeholder="587" />
      </div>
      <div class="form-error" v-if="mailFormError">{{ mailFormError }}</div>
      <div class="form-actions">
        <DxButton text="취소" type="normal" styling-mode="outlined" @click="mailAccountDialogVisible = false" />
        <DxButton text="추가" type="default" :disabled="mailFormSubmitting" @click="submitMailAccount" />
      </div>
    </div>
  </DxPopup>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import { adminApi } from '@/api/admin'
import { userApi } from '@/api/user'
import { mailAccountApi } from '@/api/mail'
import spaceApi from '@/api/space'
import { DxTabPanel, DxItem } from 'devextreme-vue/tab-panel'
import { DxPieChart, DxSeries } from 'devextreme-vue/pie-chart'
import { DxDataGrid, DxColumn, DxPaging, DxFilterRow, DxSearchPanel } from 'devextreme-vue/data-grid'
import { DxPopup } from 'devextreme-vue/popup'
import { DxTextBox } from 'devextreme-vue/text-box'
import { DxSelectBox } from 'devextreme-vue/select-box'
import { DxCheckBox } from 'devextreme-vue/check-box'
import { DxButton } from 'devextreme-vue/button'

const ACTION_LABELS = {
  ADMIN_ACCESS: '관리자 접속',
  SPACE_CREATE: '스페이스 생성',
  SPACE_DELETE: '스페이스 삭제',
  CONTENT_DELETE: '콘텐츠 삭제',
  PERMISSION_CHANGE: '권한 변경',
  MAIL_ACCOUNT_CREATE: '메일계정 생성',
  MAIL_ACCOUNT_DELETE: '메일계정 삭제',
}

const TARGET_LABELS = {
  ADMIN: '관리',
  SPACE: '스페이스',
  CONTENT: '콘텐츠',
  PERMISSION: '권한',
  MAIL_ACCOUNT: '메일계정',
}

function formatTarget(row) {
  const type = TARGET_LABELS[row.targetType] || row.targetType
  return row.targetId ? `${type} #${row.targetId}` : type
}

function parseDetail(detail) {
  if (!detail) return '-'
  try {
    const obj = JSON.parse(detail)
    if (obj.endpoint) return obj.endpoint
    if (obj.spaceName) return obj.spaceName
    if (obj.spaceKey) return `[${obj.spaceKey}]`
    if (obj.contentTitle) return obj.contentTitle
    const first = Object.values(obj)[0]
    return first != null ? String(first) : '-'
  } catch {
    return detail
  }
}

const stats = ref(null)
const auditLogs = ref([])
const auditLogTotal = ref(0)
const loading = ref(false)

// 유저 관리
const users = ref([])
const dialogVisible = ref(false)
const editingUser = ref(null)
const submitting = ref(false)
const formError = ref('')
const roleOptions = [
  { label: '일반', value: 'MEMBER' },
  { label: '관리자', value: 'SITE_ADMIN' },
]
const emptyForm = () => ({ loginId: '', name: '', email: '', password: '', role: 'MEMBER' })
const form = ref(emptyForm())

async function loadUsers() {
  try {
    const res = await userApi.list()
    users.value = res.data.data || []
  } catch (e) {
    console.error('유저 목록 조회 실패:', e)
  }
}

function openCreateDialog() {
  editingUser.value = null
  form.value = emptyForm()
  formError.value = ''
  dialogVisible.value = true
}

function openEditDialog(user) {
  editingUser.value = user
  form.value = { loginId: user.loginId, name: user.name, email: user.email, password: '', role: user.role }
  formError.value = ''
  dialogVisible.value = true
}

async function submitForm() {
  formError.value = ''
  if (!form.value.name.trim() || !form.value.email.trim()) {
    formError.value = '이름과 이메일은 필수입니다.'
    return
  }
  if (!editingUser.value && (!form.value.loginId.trim() || form.value.password.length < 8)) {
    formError.value = '로그인 ID와 비밀번호(8자 이상)를 입력하세요.'
    return
  }
  if (editingUser.value && form.value.password && form.value.password.length < 8) {
    formError.value = '비밀번호는 8자 이상이어야 합니다.'
    return
  }
  submitting.value = true
  try {
    if (editingUser.value) {
      const updateData = { name: form.value.name, email: form.value.email, role: form.value.role }
      if (form.value.password) updateData.password = form.value.password
      await userApi.update(editingUser.value.id, updateData)
    } else {
      await userApi.create(form.value)
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (e) {
    formError.value = e.response?.data?.message || '저장에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(user) {
  if (user.status === 'ACTIVE') {
    if (!confirm(`'${user.name}' 계정을 비활성화하시겠습니까?`)) return
    await userApi.deactivate(user.id)
  } else {
    // 재활성화: role 유지하면서 PUT으로 status 변경은 별도 API 없으므로 임시 처리
    await userApi.update(user.id, { name: user.name, email: user.email, role: user.role })
  }
  await loadUsers()
}

// 메일 계정 관리
const spaceList = ref([])
const mailSelectedSpaceKey = ref(null)
const mailAccounts = ref([])
const mailAccountsLoading = ref(false)
const mailAccountDialogVisible = ref(false)
const mailFormSubmitting = ref(false)
const mailFormError = ref('')
const emptyMailForm = () => ({
  emailAddress: '',
  password: '',
  imapHost: '',
  imapPort: '993',
  imapSsl: true,
  smtpHost: '',
  smtpPort: '587'
})
const mailForm = ref(emptyMailForm())

async function loadSpaces() {
  try {
    const res = await spaceApi.getSpaces()
    spaceList.value = res.data.data || []
  } catch (e) {
    console.error('스페이스 목록 조회 실패:', e)
  }
}

async function onSpaceSelected() {
  if (!mailSelectedSpaceKey.value) return
  mailAccountsLoading.value = true
  try {
    const res = await mailAccountApi.getAccounts(mailSelectedSpaceKey.value)
    mailAccounts.value = res.data.data || []
  } catch (e) {
    console.error('메일 계정 조회 실패:', e)
    mailAccounts.value = []
  } finally {
    mailAccountsLoading.value = false
  }
}

function openMailAccountDialog() {
  mailForm.value = emptyMailForm()
  mailFormError.value = ''
  mailAccountDialogVisible.value = true
}

async function submitMailAccount() {
  mailFormError.value = ''
  const f = mailForm.value
  if (!f.emailAddress.trim() || !f.password.trim() || !f.imapHost.trim()) {
    mailFormError.value = '이메일 주소, 비밀번호, IMAP 호스트는 필수입니다.'
    return
  }
  mailFormSubmitting.value = true
  try {
    await mailAccountApi.createAccount(mailSelectedSpaceKey.value, {
      emailAddress: f.emailAddress,
      password: f.password,
      imapHost: f.imapHost,
      imapPort: Number(f.imapPort) || 993,
      imapSsl: f.imapSsl,
      smtpHost: f.smtpHost || null,
      smtpPort: Number(f.smtpPort) || 587
    })
    mailAccountDialogVisible.value = false
    await onSpaceSelected()
  } catch (e) {
    mailFormError.value = e.response?.data?.message || '계정 추가에 실패했습니다.'
  } finally {
    mailFormSubmitting.value = false
  }
}

const syncingIds = ref(new Set())

function syncStatusLabel(status) {
  const map = { ACTIVE: '정상', ERROR: '오류', DISABLED: '비활성', PENDING: '대기' }
  return map[status] || status
}

function syncBadgeClass(status) {
  if (status === 'ACTIVE') return 'active'
  if (status === 'ERROR' || status === 'DISABLED') return 'inactive'
  return 'pending'
}

async function triggerSync(account) {
  syncingIds.value = new Set([...syncingIds.value, account.id])
  try {
    await mailAccountApi.syncAccount(mailSelectedSpaceKey.value, account.id)
    await onSpaceSelected()
  } catch (e) {
    alert(e.response?.data?.message || '동기화 요청에 실패했습니다.')
  } finally {
    const next = new Set(syncingIds.value)
    next.delete(account.id)
    syncingIds.value = next
  }
}

async function deleteMailAccount(account) {
  if (!confirm(`'${account.emailAddress}' 계정을 삭제하시겠습니까?`)) return
  try {
    await mailAccountApi.deleteAccount(mailSelectedSpaceKey.value, account.id)
    await onSpaceSelected()
  } catch (e) {
    alert(e.response?.data?.message || '삭제에 실패했습니다.')
  }
}

const mailStatusData = computed(() => {
  if (!stats.value) return []
  return [
    { status: '정상', count: stats.value.mailAccountsOk },
    { status: '오류', count: stats.value.mailAccountsFailed }
  ]
})

function formatBytes(bytes) {
  if (bytes == null) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(async () => {
  loading.value = true
  try {
    const [statsRes, logsRes] = await Promise.all([
      adminApi.getStats(),
      adminApi.getAuditLogs({ page: 0, size: 20 })
    ])
    stats.value = statsRes.data.data
    auditLogs.value = logsRes.data.data.content
    auditLogTotal.value = logsRes.data.data.totalElements
  } catch (e) {
    console.error('관리자 데이터 로딩 실패:', e)
  } finally {
    loading.value = false
  }
  await Promise.all([loadUsers(), loadSpaces()])
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #fafafa;
  padding: 24px;
}

.admin-header {
  margin-bottom: 24px;
}

.admin-header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.admin-content {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.tab-content {
  padding: 16px 0;
}

.loading-message {
  padding: 40px;
  text-align: center;
  color: #666;
}

.no-data {
  padding: 40px;
  text-align: center;
  color: #999;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: #f5f7fa;
  border: 1px solid #e0e4ea;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a73e8;
}

.chart-section {
  margin-top: 16px;
}

.chart-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #333;
}
.user-toolbar {
  margin-bottom: 12px;
}
.action-btns {
  display: flex;
  gap: 6px;
  justify-content: center;
}
.status-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.status-badge.active {
  background: #e8f5e9;
  color: #2e7d32;
}
.status-badge.inactive {
  background: #fce4ec;
  color: #c62828;
}
.dialog-form {
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-row label {
  font-size: 13px;
  font-weight: 500;
  color: #555;
}
.form-error {
  color: #d32f2f;
  font-size: 13px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}
.mail-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
}
.mail-space-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mail-space-selector label {
  font-size: 13px;
  font-weight: 500;
  color: #555;
  white-space: nowrap;
}
.form-row-inline {
  display: flex;
  gap: 16px;
  flex-direction: row !important;
  align-items: flex-end;
}
.form-row-check {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 80px;
}
.optional {
  font-size: 11px;
  color: #999;
  font-weight: 400;
}
.status-badge.pending {
  background: #fff8e1;
  color: #f57f17;
}
.error-msg {
  font-size: 12px;
  color: #c62828;
  display: block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: help;
}
.no-error {
  color: #bbb;
  font-size: 12px;
}
</style>

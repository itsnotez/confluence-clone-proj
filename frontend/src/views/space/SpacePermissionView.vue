<template>
  <div class="permission-page">
    <AppHeader />
    <div class="page-content">
      <div class="page-header">
        <h2>{{ spaceName }} — 권한 관리</h2>
        <DxButton
          text="권한 추가"
          type="default"
          styling-mode="contained"
          @click="showAddDialog = true"
        />
      </div>

      <DxDataGrid
        :data-source="permissions"
        :show-borders="true"
        :hover-state-enabled="true"
        :row-alternation-enabled="true"
        key-expr="id"
      >
        <DxColumn data-field="subjectType" caption="대상 유형" :width="120" />
        <DxColumn data-field="subjectId" caption="대상 ID" :width="100" />
        <DxColumn data-field="permissionLevel" caption="권한 레벨" :width="140" />
        <DxColumn data-field="createdAt" caption="생성일" :width="160" cell-template="dateTemplate" />
        <DxColumn caption="삭제" :width="80" cell-template="deleteTemplate" />

        <template #dateTemplate="{ data }">
          {{ formatDate(data.value) }}
        </template>
        <template #deleteTemplate="{ data }">
          <DxButton
            text="삭제"
            type="danger"
            styling-mode="outlined"
            @click="handleRevoke(data.data)"
          />
        </template>
      </DxDataGrid>

      <!-- 권한 추가 다이얼로그 -->
      <DxPopup
        v-model:visible="showAddDialog"
        title="권한 추가"
        :width="420"
        :height="360"
        :drag-enabled="true"
        :show-close-button="true"
      >
        <div class="dialog-body">
          <div class="form-row">
            <label>대상 유형</label>
            <DxSelectBox
              v-model:value="addForm.subjectType"
              :items="subjectTypeOptions"
              @value-changed="onSubjectTypeChanged"
            />
          </div>
          <div class="form-row">
            <label>대상 ID</label>
            <DxNumberBox
              v-model:value="addForm.subjectId"
              :disabled="addForm.subjectType === 'ALL'"
              :min="1"
              placeholder="사용자 또는 그룹 ID"
            />
          </div>
          <div class="form-row">
            <label>권한 레벨</label>
            <DxSelectBox
              v-model:value="addForm.permissionLevel"
              :items="permissionLevelOptions"
            />
          </div>
          <div class="dialog-actions">
            <DxButton text="취소" type="normal" styling-mode="outlined" @click="closeDialog" />
            <DxButton text="저장" type="default" styling-mode="contained" @click="handleGrant" />
          </div>
        </div>
      </DxPopup>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import permissionApi from '@/api/permission'
import spaceApi from '@/api/space'
import { DxDataGrid, DxColumn } from 'devextreme-vue/data-grid'
import { DxPopup } from 'devextreme-vue/popup'
import { DxButton } from 'devextreme-vue/button'
import { DxSelectBox } from 'devextreme-vue/select-box'
import { DxNumberBox } from 'devextreme-vue/number-box'

const route = useRoute()
const spaceKey = route.params.spaceKey

const permissions = ref([])
const spaceName = ref(spaceKey)
const showAddDialog = ref(false)

const subjectTypeOptions = ['USER', 'GROUP', 'ALL']
const permissionLevelOptions = ['SPACE_ADMIN', 'WRITE', 'READ', 'NONE']

const addForm = reactive({
  subjectType: 'USER',
  subjectId: null,
  permissionLevel: 'READ'
})

onMounted(async () => {
  try {
    const spaceRes = await spaceApi.getSpace(spaceKey)
    spaceName.value = spaceRes.data?.name || spaceKey
  } catch {}
  await loadPermissions()
})

async function loadPermissions() {
  try {
    const res = await permissionApi.getSpacePermissions(spaceKey)
    permissions.value = res.data || []
  } catch (err) {
    console.error('권한 조회 실패:', err)
    permissions.value = []
  }
}

function onSubjectTypeChanged(e) {
  addForm.subjectType = e.value
  if (e.value === 'ALL') {
    addForm.subjectId = null
  }
}

async function handleGrant() {
  if (!addForm.subjectType || !addForm.permissionLevel) return
  if (addForm.subjectType !== 'ALL' && !addForm.subjectId) return

  try {
    await permissionApi.grantPermission(spaceKey, { ...addForm })
    await loadPermissions()
    closeDialog()
  } catch (err) {
    console.error('권한 추가 실패:', err)
  }
}

async function handleRevoke(row) {
  if (!confirm(`권한을 삭제하시겠습니까?`)) return
  try {
    await permissionApi.revokePermission(spaceKey, row.subjectType, row.subjectId)
    await loadPermissions()
  } catch (err) {
    console.error('권한 삭제 실패:', err)
  }
}

function closeDialog() {
  showAddDialog.value = false
  addForm.subjectType = 'USER'
  addForm.subjectId = null
  addForm.permissionLevel = 'READ'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'short', day: 'numeric'
  })
}
</script>

<style scoped>
.permission-page {
  min-height: 100vh;
  background: #fafafa;
}
.page-content {
  padding: 24px;
  max-width: 1100px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: #333;
}
.dialog-body {
  padding: 16px;
}
.form-row {
  margin-bottom: 14px;
}
.form-row label {
  display: block;
  font-size: 13px;
  color: #555;
  margin-bottom: 4px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}
</style>

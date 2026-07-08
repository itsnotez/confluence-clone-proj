<template>
  <div class="space-list-page">
    <AppHeader />
    <div class="page-content">
      <div class="page-header">
        <h2>스페이스 목록</h2>
        <DxButton
          v-if="auth.user?.role === 'SITE_ADMIN'"
          text="새 Space 만들기"
          type="default"
          styling-mode="contained"
          @click="showCreateDialog = true"
        />
      </div>

      <DxDataGrid
        :data-source="spaceStore.spaces"
        :show-borders="true"
        :hover-state-enabled="true"
        :row-alternation-enabled="true"
        key-expr="spaceKey"
        :selected-row-keys="selectedKeys"
        @row-click="onRowClick"
      >
        <DxSelection mode="single" />
        <DxColumn data-field="spaceKey" caption="키" :width="120" />
        <DxColumn data-field="name" caption="Space 이름" />
        <DxColumn data-field="type" caption="유형" :width="100" />
        <DxColumn data-field="description" caption="설명" />
        <DxColumn caption="즐겨찾기" :width="100" cell-template="favoriteTemplate" />
        <template #favoriteTemplate="{ data }">
          <DxButton
            :text="data.data.favorited ? '★' : '☆'"
            :type="data.data.favorited ? 'default' : 'normal'"
            styling-mode="text"
            @click.stop="handleToggleFavorite(data.data.spaceKey)"
          />
        </template>
      </DxDataGrid>

      <!-- 생성 다이얼로그 -->
      <DxPopup
        v-model:visible="showCreateDialog"
        title="새 Space 만들기"
        :width="480"
        :height="420"
        :drag-enabled="true"
        :show-close-button="true"
      >
        <div class="dialog-body">
          <DxForm
            :form-data="createForm"
            :col-count="1"
            label-location="top"
          >
            <DxSimpleItem
              data-field="spaceKey"
              :label="{ text: 'Space 키 (영문/숫자)' }"
              :editor-options="{ onValueChanged: e => createForm.spaceKey = e.value }"
            >
              <DxRequiredRule message="Space 키를 입력하세요." />
            </DxSimpleItem>
            <DxSimpleItem
              data-field="name"
              :label="{ text: 'Space 이름' }"
              :editor-options="{ onValueChanged: e => createForm.name = e.value }"
            >
              <DxRequiredRule message="Space 이름을 입력하세요." />
            </DxSimpleItem>
            <DxSimpleItem
              data-field="type"
              editor-type="dxSelectBox"
              :label="{ text: '유형' }"
              :editor-options="{
                items: ['PUBLIC', 'PRIVATE'],
                value: createForm.type,
                onValueChanged: e => createForm.type = e.value
              }"
            />
            <DxSimpleItem
              data-field="description"
              editor-type="dxTextArea"
              :label="{ text: '설명' }"
              :editor-options="{
                height: 70,
                onValueChanged: e => createForm.description = e.value
              }"
            />
          </DxForm>
          <div class="dialog-actions">
            <DxButton text="취소" type="normal" styling-mode="outlined" @click="closeDialog" />
            <DxButton text="저장" type="default" styling-mode="contained" @click="handleCreate" />
          </div>
        </div>
      </DxPopup>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/layout/AppHeader.vue'
import { DxDataGrid, DxColumn, DxSelection } from 'devextreme-vue/data-grid'
import { DxPopup } from 'devextreme-vue/popup'
import { DxForm, DxSimpleItem, DxRequiredRule } from 'devextreme-vue/form'
import { DxButton } from 'devextreme-vue/button'

const router = useRouter()
const spaceStore = useSpaceStore()
const auth = useAuthStore()
const showCreateDialog = ref(false)
const selectedKeys = ref([])

const createForm = reactive({
  spaceKey: '',
  name: '',
  type: 'PUBLIC',
  description: ''
})

onMounted(() => {
  spaceStore.fetchSpaces()
})

function onRowClick(e) {
  selectedKeys.value = [e.data.spaceKey]
  router.push(`/spaces/${e.data.spaceKey}`)
}

function handleToggleFavorite(spaceKey) {
  spaceStore.toggleFavorite(spaceKey)
}

async function handleCreate() {
  if (!createForm.spaceKey || !createForm.name) return
  try {
    await spaceStore.createSpace({ ...createForm })
    closeDialog()
  } catch (err) {
    console.error('createSpace error:', err)
  }
}

function closeDialog() {
  showCreateDialog.value = false
  createForm.spaceKey = ''
  createForm.name = ''
  createForm.type = 'PUBLIC'
  createForm.description = ''
}
</script>

<style scoped>
.space-list-page {
  min-height: 100vh;
  background: #F4F5F6;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.page-content {
  padding: 32px;
  max-width: 1100px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1E2124;
}
.dialog-body {
  padding: 16px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

:deep(.dx-datagrid .dx-row) {
  cursor: pointer;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
  font-size: 15px;
  color: #1E2124;
}
:deep(.dx-datagrid .dx-state-hover td) {
  background-color: #ECF2FE !important;
  color: #0B50D0;
}
:deep(.dx-datagrid .dx-selection td),
:deep(.dx-datagrid .dx-selection.dx-row:not(.dx-row-lines) td) {
  background-color: #256EF4 !important;
  color: #fff !important;
}
:deep(.dx-datagrid .dx-selection td .dx-button-text) {
  color: #fff;
}
:deep(.dx-datagrid-headers .dx-datagrid-text-content) {
  font-weight: 700;
  color: #1E2124;
  font-size: 15px;
}
</style>

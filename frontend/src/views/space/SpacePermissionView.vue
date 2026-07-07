<template>
  <div class="permission-page">
    <AppHeader />
    <div class="page-content">
      <div class="manage-card">
        <!-- Card Header -->
        <div class="card-header">
          <h2 class="card-title">Manage Access — {{ spaceName }}</h2>
          <button class="close-btn" @click="goBack">×</button>
        </div>

        <!-- Add Users Section -->
        <div class="card-section add-section">
          <div class="section-label">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
            <span>사용자 추가</span>
          </div>
          <div class="chip-input-box" @click="focusUserInput">
            <span v-for="u in stagedUsers" :key="u.id" class="user-chip">
              {{ u.email.toUpperCase() }}
              <button class="chip-x" @click.stop="removeStaged(u.id)">×</button>
            </span>
            <input
              ref="userInputRef"
              v-model="userSearch"
              placeholder="이메일 또는 이름으로 검색..."
              class="chip-text-input"
              @input="onSearchInput"
              @keydown.escape="searchResults = []"
              @blur="onSearchBlur"
            />
          </div>
          <div v-if="searchResults.length > 0" class="search-dropdown">
            <div
              v-for="u in searchResults"
              :key="u.id"
              class="search-item"
              @mousedown.prevent="stageUser(u)"
            >
              <span class="si-name">{{ u.name }}</span>
              <span class="si-email">{{ u.email }}</span>
            </div>
          </div>
        </div>

        <!-- Users with access Section -->
        <div class="card-section access-section">
          <div class="section-label">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            <span>접근 중인 사용자</span>
          </div>

          <div class="access-table">
            <!-- Table Header -->
            <div class="table-row table-head-row">
              <div class="col-filter">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#aaa" stroke-width="2.5">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input v-model="filterText" placeholder="이름으로 필터..." class="filter-input" />
              </div>
              <div class="col-perm">읽기</div>
              <div class="col-perm">쓰기</div>
              <div class="col-perm">관리자</div>
              <div class="col-del"></div>
            </div>

            <!-- No data -->
            <div v-if="filteredRows.length === 0" class="empty-row">
              {{ loading ? '로딩 중...' : '접근 권한이 있는 사용자가 없습니다.' }}
            </div>

            <!-- Permission Rows -->
            <div
              v-for="row in filteredRows"
              :key="row.key"
              class="table-row perm-row"
            >
              <div class="col-info">
                <span class="row-name">{{ row.name }}</span>
                <span class="row-email">{{ row.email }}</span>
              </div>
              <div class="col-perm">
                <label class="cb-wrap">
                  <input type="checkbox" :checked="row.read" @change="togglePerm(row, 'READ')" />
                  <span class="cb-custom" :class="{ checked: row.read }"></span>
                </label>
              </div>
              <div class="col-perm">
                <label class="cb-wrap">
                  <input type="checkbox" :checked="row.write" @change="togglePerm(row, 'WRITE')" />
                  <span class="cb-custom" :class="{ checked: row.write }"></span>
                </label>
              </div>
              <div class="col-perm">
                <label class="cb-wrap">
                  <input type="checkbox" :checked="row.admin" @change="togglePerm(row, 'SPACE_ADMIN')" />
                  <span class="cb-custom" :class="{ checked: row.admin }"></span>
                </label>
              </div>
              <div class="col-del">
                <button class="delete-row-btn" @click="removeRow(row)" title="권한 삭제">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#aaa" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                    <path d="M10 11v6"/>
                    <path d="M14 11v6"/>
                    <path d="M9 6V4h6v2"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="card-footer">
          <div v-if="saveError" class="save-error">{{ saveError }}</div>
          <div class="footer-actions">
            <button class="btn-cancel" @click="goBack">취소</button>
            <button class="btn-save" :disabled="saving" @click="saveAll">
              {{ saving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import permissionApi from '@/api/permission'
import spaceApi from '@/api/space'
import { userApi } from '@/api/user'

const route = useRoute()
const router = useRouter()
const spaceKey = route.params.spaceKey

const spaceName = ref(spaceKey)
const loading = ref(false)
const saving = ref(false)
const saveError = ref('')

// All users from system
const allUsers = ref([])

// Rows: working list of user permissions
const rows = ref([])

// Staged users for adding (chip input)
const stagedUsers = ref([])
const userSearch = ref('')
const searchResults = ref([])
const userInputRef = ref(null)

// Filter text for the access table
const filterText = ref('')

const filteredRows = computed(() => {
  const q = filterText.value.trim().toLowerCase()
  if (!q) return rows.value.filter(r => !r.deleted)
  return rows.value.filter(r => !r.deleted && (r.name.toLowerCase().includes(q) || r.email.toLowerCase().includes(q)))
})

function levelToFlags(level) {
  return {
    read: level !== 'NONE' && !!level,
    write: level === 'WRITE' || level === 'SPACE_ADMIN',
    admin: level === 'SPACE_ADMIN',
  }
}

function flagsToLevel(row, toggledCol) {
  if (toggledCol === 'READ') {
    return row.read ? 'NONE' : 'READ'
  }
  if (toggledCol === 'WRITE') {
    return row.write ? 'READ' : 'WRITE'
  }
  if (toggledCol === 'SPACE_ADMIN') {
    return row.admin ? 'WRITE' : 'SPACE_ADMIN'
  }
  return row.level
}

function togglePerm(row, col) {
  const newLevel = flagsToLevel(row, col)
  row.level = newLevel
  const flags = levelToFlags(newLevel)
  row.read = flags.read
  row.write = flags.write
  row.admin = flags.admin
  row.changed = true
}

function removeRow(row) {
  row.deleted = true
}

// User search
function onSearchInput() {
  const q = userSearch.value.trim().toLowerCase()
  if (!q) {
    searchResults.value = []
    return
  }
  const stagedIds = new Set(stagedUsers.value.map(u => u.id))
  const existingIds = new Set(rows.value.map(r => r.key))
  searchResults.value = allUsers.value
    .filter(u =>
      !stagedIds.has(u.id) &&
      !existingIds.has(u.id) &&
      (u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
    )
    .slice(0, 6)
}

function onSearchBlur() {
  setTimeout(() => {
    searchResults.value = []
  }, 150)
}

function focusUserInput() {
  userInputRef.value?.focus()
}

function stageUser(u) {
  if (!stagedUsers.value.find(s => s.id === u.id)) {
    stagedUsers.value.push(u)
  }
  userSearch.value = ''
  searchResults.value = []
  userInputRef.value?.focus()
}

function removeStaged(id) {
  stagedUsers.value = stagedUsers.value.filter(u => u.id !== id)
}

function goBack() {
  router.back()
}

async function loadData() {
  loading.value = true
  try {
    const [spaceRes, usersRes, permsRes] = await Promise.all([
      spaceApi.getSpace(spaceKey),
      userApi.list(),
      permissionApi.getSpacePermissions(spaceKey),
    ])
    spaceName.value = spaceRes.data?.data?.name || spaceRes.data?.name || spaceKey
    allUsers.value = usersRes.data?.data || []

    const rawPerms = permsRes.data?.data || permsRes.data || []
    const userMap = Object.fromEntries(allUsers.value.map(u => [u.id, u]))

    rows.value = rawPerms
      .filter(p => p.subjectType === 'USER')
      .map(p => {
        const user = userMap[p.subjectId] || {}
        const flags = levelToFlags(p.permissionLevel)
        return {
          key: p.subjectId,
          subjectId: p.subjectId,
          subjectType: 'USER',
          name: user.name || `User #${p.subjectId}`,
          email: user.email || '',
          level: p.permissionLevel,
          ...flags,
          changed: false,
          deleted: false,
          isNew: false,
        }
      })
  } catch (err) {
    console.error('데이터 로딩 실패:', err)
  } finally {
    loading.value = false
  }
}

async function saveAll() {
  saving.value = true
  saveError.value = ''
  try {
    // 1. Staged users → add as READ
    for (const u of stagedUsers.value) {
      await permissionApi.grantPermission(spaceKey, {
        subjectType: 'USER',
        subjectId: u.id,
        permissionLevel: 'READ',
      })
    }

    // 2. Changed rows → upsert
    for (const row of rows.value) {
      if (row.deleted) {
        await permissionApi.revokePermission(spaceKey, row.subjectType, row.subjectId)
      } else if (row.changed) {
        if (row.level === 'NONE' || !row.level) {
          await permissionApi.revokePermission(spaceKey, row.subjectType, row.subjectId)
        } else {
          await permissionApi.grantPermission(spaceKey, {
            subjectType: row.subjectType,
            subjectId: row.subjectId,
            permissionLevel: row.level,
          })
        }
      }
    }

    await loadData()
    stagedUsers.value = []
  } catch (err) {
    saveError.value = err.response?.data?.message || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.permission-page {
  min-height: 100vh;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;
}

.page-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 24px;
}

.manage-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
  width: 100%;
  max-width: 760px;
  overflow: hidden;
}

/* Card Header */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px 16px;
  border-bottom: 1px solid #e8eaed;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 22px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 4px;
}
.close-btn:hover {
  background: #f0f0f0;
  color: #555;
}

/* Sections */
.card-section {
  padding: 20px 28px;
  border-bottom: 1px solid #e8eaed;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
  margin-bottom: 12px;
}

/* Chip Input */
.chip-input-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  background: #fff;
  cursor: text;
  min-height: 44px;
}

.chip-input-box:focus-within {
  border-color: #4a90d9;
  box-shadow: 0 0 0 3px rgba(74, 144, 217, 0.15);
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #2c5f8a;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 3px;
  letter-spacing: 0.3px;
}

.chip-x {
  background: none;
  border: none;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0 0 0 2px;
}
.chip-x:hover {
  color: #fff;
}

.chip-text-input {
  flex: 1;
  min-width: 160px;
  border: none;
  outline: none;
  font-size: 13px;
  color: #333;
  background: transparent;
}

/* Search Dropdown */
.search-dropdown {
  margin-top: 4px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  overflow: hidden;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
}
.search-item:hover {
  background: #f0f4ff;
}

.si-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
.si-email {
  font-size: 12px;
  color: #888;
}

/* Access Table */
.access-table {
  border: 1px solid #d0d7de;
  border-radius: 6px;
  overflow: hidden;
}

.table-row {
  display: grid;
  grid-template-columns: 1fr 72px 72px 72px 40px;
  align-items: center;
  border-bottom: 1px solid #e8eaed;
}
.table-row:last-child {
  border-bottom: none;
}

.table-head-row {
  background: #f8f9fa;
  border-bottom: 1px solid #d0d7de;
}

.col-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
}

.filter-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 13px;
  color: #333;
  background: transparent;
}
.filter-input::placeholder {
  color: #aaa;
}

.col-perm {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  padding: 8px 0;
}

.col-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
}

.row-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
.row-email {
  font-size: 12px;
  color: #888;
}

.perm-row:hover {
  background: #fafbfc;
}

/* Custom Checkbox */
.cb-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.cb-wrap input[type="checkbox"] {
  display: none;
}

.cb-custom {
  width: 18px;
  height: 18px;
  border: 2px solid #c0c8d0;
  border-radius: 3px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.cb-custom.checked {
  background: #3d6b9e;
  border-color: #3d6b9e;
}

.cb-custom.checked::after {
  content: '';
  width: 10px;
  height: 6px;
  border-left: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(-45deg) translateY(-1px);
}

.cb-wrap:hover .cb-custom:not(.checked) {
  border-color: #8aaccc;
}

.col-del {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.delete-row-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  opacity: 0.5;
}
.delete-row-btn:hover {
  opacity: 1;
  background: #fee;
}
.delete-row-btn:hover svg {
  stroke: #e53e3e;
}

.empty-row {
  padding: 24px;
  text-align: center;
  color: #aaa;
  font-size: 13px;
}

/* Card Footer */
.card-footer {
  padding: 16px 28px;
  background: #f8f9fa;
  border-top: 1px solid #e8eaed;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.save-error {
  font-size: 13px;
  color: #e53e3e;
  margin-bottom: 10px;
  text-align: right;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #d0d7de;
  border-radius: 5px;
  background: #fff;
  color: #444;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-cancel:hover {
  background: #f5f5f5;
}

.btn-save {
  padding: 8px 24px;
  border: none;
  border-radius: 5px;
  background: #3d6b9e;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.btn-save:hover:not(:disabled) {
  background: #2c5580;
}
.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

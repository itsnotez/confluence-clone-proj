<template>
  <div class="content-tree">
    <div v-if="sortedRoots.length === 0" class="empty-tree">페이지가 없습니다.</div>
    <TreeNode
      v-for="node in sortedRoots"
      :key="node.id"
      :node="node"
      :depth="0"
      :selected-id="currentContentId"
      :space-key="spaceKey"
      @navigate="navigateTo"
      @new-child="handleNewChild"
      @move="openMoveModal"
      @delete="handleDelete"
    />

    <!-- Move modal -->
    <Teleport to="body">
      <div v-if="moveModal.visible" class="modal-overlay" @click.self="closeMoveModal">
        <div class="modal-box">
          <div class="modal-header">
            <span>페이지 이동: <strong>{{ moveModal.node?.title }}</strong></span>
            <button class="modal-close" @click="closeMoveModal">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-hint">이동할 위치의 상위 페이지를 선택하세요</p>
            <div class="move-tree">
              <div
                class="move-option"
                :class="{ selected: moveModal.selectedParentId === null }"
                @click="moveModal.selectedParentId = null"
              >
                <span class="move-option-icon">🏠</span> 루트 (최상위)
              </div>
              <MoveTreeOption
                v-for="n in sortedRoots"
                :key="n.id"
                :node="n"
                :depth="1"
                :excluded-id="moveModal.node?.id"
                :selected-id="moveModal.selectedParentId"
                @select="moveModal.selectedParentId = $event"
              />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="closeMoveModal">취소</button>
            <button class="btn-confirm" :disabled="moveModal.loading" @click="confirmMove">
              {{ moveModal.loading ? '이동 중...' : '이동' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, ref, defineComponent, h, resolveComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import contentApi from '@/api/content'
import TreeNode from './TreeNode.vue'

// Recursive component for the move modal picker
const MoveTreeOption = defineComponent({
  name: 'MoveTreeOption',
  props: {
    node: Object,
    depth: { type: Number, default: 1 },
    excludedId: [Number, String],
    selectedId: { default: null }
  },
  emits: ['select'],
  setup(props, { emit }) {
    return () => {
      if (Number(props.node.id) === Number(props.excludedId)) return null
      const isSelected = props.selectedId != null && Number(props.selectedId) === props.node.id
      return h('div', null, [
        h('div', {
          class: ['move-option', isSelected ? 'selected' : ''],
          style: { paddingLeft: (props.depth * 16 + 8) + 'px' },
          onClick: () => emit('select', props.node.id)
        }, [
          h('span', { class: 'move-option-icon' }, '📄'),
          ' ' + (props.node.title || '(제목 없음)')
        ]),
        ...(props.node.children || []).map(child =>
          h(MoveTreeOption, {
            node: child,
            depth: props.depth + 1,
            excludedId: props.excludedId,
            selectedId: props.selectedId,
            onSelect: (id) => emit('select', id)
          })
        )
      ])
    }
  }
})

const props = defineProps({
  spaceKey: { type: String, required: true }
})

const router = useRouter()
const route = useRoute()
const spaceStore = useSpaceStore()

const currentContentId = computed(() => {
  const id = route.params.contentId
  return id ? Number(id) : null
})

const sortedRoots = computed(() =>
  [...(spaceStore.contentTree || [])].sort((a, b) => a.position - b.position)
)

const moveModal = ref({
  visible: false,
  node: null,
  selectedParentId: null,
  loading: false
})

function navigateTo(id) {
  router.push(`/spaces/${props.spaceKey}/contents/${id}`)
}

function handleNewChild(parentId) {
  router.push({ path: `/spaces/${props.spaceKey}/contents/new`, query: { parentId } })
}

async function handleDelete(node) {
  if (!confirm(`"${node.title}" 페이지를 삭제하시겠습니까?`)) return
  try {
    await contentApi.deleteContent(node.id)
    await spaceStore.fetchContentTree(props.spaceKey)
    if (currentContentId.value === node.id) {
      router.push(`/spaces/${props.spaceKey}`)
    }
  } catch (e) {
    alert('삭제에 실패했습니다.')
  }
}

function openMoveModal(node) {
  moveModal.value = {
    visible: true,
    node,
    selectedParentId: node.parentId ?? null,
    loading: false
  }
}

function closeMoveModal() {
  moveModal.value.visible = false
}

async function confirmMove() {
  const { node, selectedParentId } = moveModal.value
  if (!node) return
  moveModal.value.loading = true
  try {
    const siblings = selectedParentId == null
      ? spaceStore.contentTree
      : findChildren(spaceStore.contentTree, selectedParentId)
    const position = siblings.filter(s => s.id !== node.id).length
    await contentApi.moveContent(node.id, { parentId: selectedParentId, position })
    await spaceStore.fetchContentTree(props.spaceKey)
    closeMoveModal()
  } catch (e) {
    alert(e?.response?.data?.message || '이동에 실패했습니다.')
  } finally {
    moveModal.value.loading = false
  }
}

function findChildren(tree, parentId) {
  for (const node of tree) {
    if (node.id === parentId) return node.children || []
    if (node.children?.length) {
      const found = findChildren(node.children, parentId)
      if (found.length || node.id === parentId) return found
    }
  }
  return []
}

async function loadTree() {
  if (props.spaceKey) {
    await spaceStore.fetchContentTree(props.spaceKey)
  }
}

onMounted(loadTree)
watch(() => props.spaceKey, loadTree)
</script>

<style scoped>
.content-tree {
  flex: 1;
  overflow-y: auto;
  padding: 6px 4px;
}

.empty-tree {
  padding: 16px;
  color: #999;
  font-size: 13px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-box {
  background: #fff;
  border-radius: 8px;
  width: 420px;
  max-width: 95vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  padding: 2px 6px;
  border-radius: 4px;
}
.modal-close:hover { background: #eee; }

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 18px;
}

.modal-hint {
  font-size: 12px;
  color: #666;
  margin: 0 0 10px;
}

.move-tree {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}

.move-option-icon { font-size: 12px; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 18px;
  border-top: 1px solid #e0e0e0;
}

.btn-cancel {
  padding: 7px 16px;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.btn-cancel:hover { background: #f5f5f5; }

.btn-confirm {
  padding: 7px 16px;
  background: #1976d2;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.btn-confirm:hover:not(:disabled) { background: #1565c0; }
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

<style>
.tree-context-menu {
  position: absolute;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  z-index: 10000;
  min-width: 150px;
  padding: 4px 0;
}
.tree-context-menu .menu-item {
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  color: #333;
}
.tree-context-menu .menu-item:hover { background: #f0f0f0; }
.tree-context-menu .menu-item-danger { color: #d32f2f; }
.tree-context-menu .menu-item-danger:hover { background: #fff0f0; }

.move-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #333;
  user-select: none;
}
.move-option:hover { background: #f0f4ff; }
.move-option.selected {
  background: #1976d2;
  color: #fff;
  font-weight: 600;
}
.move-option.selected .move-option-icon { filter: brightness(10); }
</style>

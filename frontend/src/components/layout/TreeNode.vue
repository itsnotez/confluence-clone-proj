<template>
  <div class="tree-node">
    <div
      class="tree-node-row"
      :class="{ 'is-selected': isSelected }"
      :style="{ paddingLeft: (depth * 16 + 4) + 'px' }"
      @click="handleClick"
      @contextmenu.prevent="canWrite && showMenu($event)"
    >
      <!-- expand/collapse chevron -->
      <span
        class="chevron"
        :class="{ expanded: isExpanded }"
        @click.stop="toggleExpand"
      >
        <svg v-if="hasChildren || node.children?.length > 0" width="10" height="10" viewBox="0 0 10 10">
          <path d="M2 3 L5 7 L8 3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span v-else style="display:inline-block;width:10px;"/>
      </span>

      <!-- page icon -->
      <span class="node-icon">
        <svg width="13" height="13" viewBox="0 0 14 14" fill="none">
          <rect x="2" y="1" width="10" height="12" rx="1" stroke="#888" stroke-width="1.2"/>
          <line x1="4" y1="4.5" x2="10" y2="4.5" stroke="#888" stroke-width="1"/>
          <line x1="4" y1="6.5" x2="10" y2="6.5" stroke="#888" stroke-width="1"/>
          <line x1="4" y1="8.5" x2="8"  y2="8.5" stroke="#888" stroke-width="1"/>
        </svg>
      </span>

      <!-- title -->
      <span class="node-title">{{ node.title || '(제목 없음)' }}</span>

      <!-- hover menu button -->
      <span v-if="canWrite" class="more-btn" @click.stop="showMenu($event)" title="더 보기">⋮</span>
    </div>

    <!-- context menu -->
    <Teleport to="body">
      <div
        v-if="menuVisible && canWrite"
        class="tree-context-menu"
        :style="{ top: menuY + 'px', left: menuX + 'px' }"
        @click.stop
      >
        <div class="menu-item" @click="onNewChild">새 하위 페이지</div>
        <div class="menu-item" @click="onMove">이동</div>
        <div class="menu-item menu-item-danger" @click="onDelete">삭제</div>
      </div>
    </Teleport>

    <!-- children -->
    <div v-if="isExpanded && node.children && node.children.length > 0" class="tree-children">
      <TreeNode
        v-for="child in sortedChildren"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :selected-id="selectedId"
        :space-key="spaceKey"
        @navigate="$emit('navigate', $event)"
        @new-child="$emit('new-child', $event)"
        @move="$emit('move', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useSpaceStore } from '@/stores/space'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  selectedId: { type: [Number, String, null], default: null },
  spaceKey: { type: String, required: true }
})

const spaceStore = useSpaceStore()
const auth = useAuthStore()

const canWrite = computed(() => {
  if (auth.user?.role === 'SITE_ADMIN') return true
  const p = spaceStore.mySpacePermission
  return p === 'WRITE' || p === 'SPACE_ADMIN'
})

const emit = defineEmits(['navigate', 'new-child', 'move', 'delete'])

const isExpanded = ref(true)
const menuVisible = ref(false)
const menuX = ref(0)
const menuY = ref(0)

const isSelected = computed(() => props.selectedId != null && Number(props.selectedId) === props.node.id)
const hasChildren = computed(() => props.node.children && props.node.children.length > 0)
const sortedChildren = computed(() => {
  if (!props.node.children) return []
  return [...props.node.children].sort((a, b) => a.position - b.position)
})

function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

function handleClick() {
  emit('navigate', props.node.id)
}

function showMenu(e) {
  const rect = e.target.closest ? e.target.closest('.tree-node-row')?.getBoundingClientRect() : null
  if (rect) {
    menuX.value = rect.right - 4
    menuY.value = rect.top + window.scrollY
  } else {
    menuX.value = e.clientX
    menuY.value = e.clientY + window.scrollY
  }
  menuVisible.value = true
}

function closeMenu() {
  menuVisible.value = false
}

function onNewChild() {
  closeMenu()
  emit('new-child', props.node.id)
}

function onMove() {
  closeMenu()
  emit('move', props.node)
}

function onDelete() {
  closeMenu()
  emit('delete', props.node)
}

onMounted(() => {
  document.addEventListener('click', closeMenu)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenu)
})
</script>

<style scoped>
.tree-node {
  user-select: none;
}

.tree-node-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 3px;
  padding-bottom: 3px;
  padding-right: 8px;
  cursor: pointer;
  border-radius: 4px;
  position: relative;
  color: #333;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
}

.tree-node-row:hover {
  background: #e8e8e8;
}

.tree-node-row.is-selected {
  background: #d0e4f7;
  color: #1565c0;
  font-weight: 500;
}

.tree-node-row.is-selected .node-icon svg rect,
.tree-node-row.is-selected .node-icon svg line {
  stroke: #1565c0;
}

.chevron {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: #888;
  transition: transform 0.15s;
}

.chevron:not(.expanded) svg {
  transform: rotate(-90deg);
}

.node-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.more-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 3px;
  font-size: 14px;
  line-height: 1;
  color: #555;
  flex-shrink: 0;
}

.more-btn:hover {
  background: #ccc;
}

.tree-node-row:hover .more-btn {
  display: flex;
}
</style>

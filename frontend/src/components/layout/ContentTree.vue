<template>
  <div class="content-tree">
    <DxTreeView
      :items="treeItems"
      :select-by-click="true"
      :expand-all-enabled="true"
      key-expr="id"
      display-expr="text"
      items-expr="items"
      @item-click="onItemClick"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { DxTreeView } from 'devextreme-vue/tree-view'

const props = defineProps({
  spaceKey: {
    type: String,
    required: true
  }
})

const router = useRouter()
const spaceStore = useSpaceStore()

function mapNode(node) {
  return {
    id: node.id,
    text: node.title || node.name || '(제목 없음)',
    expanded: true,
    icon: node.type === 'FOLDER' ? 'folder' : node.type === 'BLOG' ? 'comment' : 'file',
    items: node.children && node.children.length > 0 ? node.children.map(mapNode) : undefined
  }
}

const treeItems = computed(() => {
  return (spaceStore.contentTree || []).map(mapNode)
})

function onItemClick(e) {
  const item = e.itemData
  if (item && item.id) {
    router.push(`/spaces/${props.spaceKey}/contents/${item.id}`)
  }
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
  max-height: calc(100vh - 56px - 80px);
  overflow-y: auto;
  width: 250px;
  padding: 8px 0;
}
</style>

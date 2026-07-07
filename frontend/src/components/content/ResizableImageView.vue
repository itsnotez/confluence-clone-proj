<template>
  <node-view-wrapper as="span" class="resizable-image-wrapper" :class="{ selected }">
    <img
      :src="node.attrs.src"
      :alt="node.attrs.alt || ''"
      :style="imgStyle"
      ref="imgRef"
      draggable="false"
    />
    <span
      v-if="editor.isEditable"
      class="resize-handle"
      @mousedown.prevent="startResize"
    />
  </node-view-wrapper>
</template>

<script setup>
import { computed, ref } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'

const props = defineProps({
  node: Object,
  updateAttributes: Function,
  selected: Boolean,
  editor: Object,
})

const imgRef = ref(null)

const imgStyle = computed(() => {
  const w = props.node.attrs.width
  return w
    ? `width: ${w}; max-width: 100%; height: auto; display: inline-block; vertical-align: bottom;`
    : 'max-width: 100%; height: auto; display: inline-block; vertical-align: bottom;'
})

function startResize(e) {
  const startX = e.clientX
  const startWidth = imgRef.value?.offsetWidth ?? 200

  function onMouseMove(ev) {
    const newWidth = Math.max(50, startWidth + (ev.clientX - startX))
    props.updateAttributes({ width: `${newWidth}px` })
  }

  function onMouseUp() {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}
</script>

<style scoped>
.resizable-image-wrapper {
  display: inline-block;
  position: relative;
  line-height: 0;
  user-select: none;
}
.resizable-image-wrapper.selected img {
  outline: 2px solid #1976d2;
  outline-offset: 1px;
}
.resize-handle {
  position: absolute;
  bottom: 3px;
  right: 3px;
  width: 12px;
  height: 12px;
  background: #1976d2;
  border: 2px solid white;
  border-radius: 2px;
  cursor: se-resize;
  opacity: 0;
  transition: opacity 0.15s;
}
.resizable-image-wrapper:hover .resize-handle,
.resizable-image-wrapper.selected .resize-handle {
  opacity: 1;
}
</style>

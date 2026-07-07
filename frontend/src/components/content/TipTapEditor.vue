<template>
  <div class="tiptap-editor">
    <div v-if="!readonly" class="toolbar">
      <button class="toolbar-btn" :class="{ active: editor?.isActive('bold') }" @click="editor?.chain().focus().toggleBold().run()" title="굵게">B</button>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('italic') }" @click="editor?.chain().focus().toggleItalic().run()" title="기울임"><i>I</i></button>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('underline') }" @click="editor?.chain().focus().toggleUnderline().run()" title="밑줄">U</button>
      <span class="toolbar-sep">|</span>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('heading', { level: 1 }) }" @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()" title="제목 1">H1</button>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('heading', { level: 2 }) }" @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()" title="제목 2">H2</button>
      <span class="toolbar-sep">|</span>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('bulletList') }" @click="editor?.chain().focus().toggleBulletList().run()" title="글머리 기호">&#8226; 목록</button>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('orderedList') }" @click="editor?.chain().focus().toggleOrderedList().run()" title="번호 목록">1. 목록</button>
      <span class="toolbar-sep">|</span>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('code') }" @click="editor?.chain().focus().toggleCode().run()" title="인라인 코드">code</button>
      <button class="toolbar-btn" :class="{ active: editor?.isActive('codeBlock') }" @click="editor?.chain().focus().toggleCodeBlock().run()" title="코드 블록">{ }</button>
      <span class="toolbar-sep">|</span>
      <button class="toolbar-btn" @click="setLink" title="링크">링크</button>
      <button class="toolbar-btn" @click="addImage" title="이미지">이미지</button>
    </div>
    <editor-content
      :editor="editor"
      class="editor-content"
      :class="{ 'drag-over': isDragOver }"
      @dragover.prevent="isDragOver = true"
      @dragleave="isDragOver = false"
      @drop.prevent="handleDrop"
    />
  </div>
</template>

<script setup>
import { onBeforeUnmount, watch, ref } from 'vue'
import { useEditor, EditorContent, VueNodeViewRenderer } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import { Table, TableRow, TableCell, TableHeader } from '@tiptap/extension-table'
import ResizableImageView from './ResizableImageView.vue'

const ResizableImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: el => el.style.width || el.getAttribute('width') || null,
        renderHTML: attrs => attrs.width ? { style: `width: ${attrs.width}; max-width: 100%; height: auto;` } : {},
      },
    }
  },
  addNodeView() {
    return VueNodeViewRenderer(ResizableImageView)
  },
})

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

function parseContent(val) {
  if (!val) return ''
  try {
    return JSON.parse(val)
  } catch {
    return val
  }
}

const editor = useEditor({
  editable: !props.readonly,
  extensions: [
    StarterKit,
    ResizableImage,
    Link.configure({ openOnClick: false }),
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader
  ],
  content: parseContent(props.modelValue),
  onUpdate: ({ editor }) => {
    emit('update:modelValue', JSON.stringify(editor.getJSON()))
  }
})

watch(() => props.modelValue, (newVal) => {
  if (!editor.value) return
  const current = JSON.stringify(editor.value.getJSON())
  if (newVal !== current) {
    editor.value.commands.setContent(parseContent(newVal), false)
  }
})

watch(() => props.readonly, (val) => {
  if (editor.value) {
    editor.value.setEditable(!val)
  }
})

function setLink() {
  const url = window.prompt('URL 입력:')
  if (url) {
    editor.value?.chain().focus().setLink({ href: url }).run()
  }
}

function addImage() {
  const url = window.prompt('이미지 URL 입력:')
  if (url) {
    editor.value?.chain().focus().setImage({ src: url }).run()
  }
}

const isDragOver = ref(false)

function handleDrop(event) {
  isDragOver.value = false
  const files = Array.from(event.dataTransfer?.files ?? []).filter(f => f.type.startsWith('image/'))
  if (!files.length) return
  files.forEach(file => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const src = e.target?.result
      if (src) {
        editor.value?.chain().focus().setImage({ src: String(src) }).run()
      }
    }
    reader.readAsDataURL(file)
  })
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.tiptap-editor {
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
  flex-wrap: wrap;
}
.toolbar-btn {
  padding: 4px 8px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 3px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
  min-width: 28px;
}
.toolbar-btn:hover {
  background: #e0e0e0;
  border-color: #ccc;
}
.toolbar-btn.active {
  background: #1976d2;
  color: white;
  border-color: #1565c0;
}
.toolbar-sep {
  color: #ccc;
  padding: 0 4px;
}
.editor-content {
  min-height: 400px;
  padding: 16px;
  background: white;
  transition: background 0.15s;
}
.editor-content.drag-over {
  background: #e8f0fe;
  outline: 2px dashed #1976d2;
  outline-offset: -2px;
}
.editor-content :deep(.ProseMirror) {
  min-height: 370px;
  outline: none;
}
.editor-content :deep(.ProseMirror p) {
  margin: 0.5em 0;
}
.editor-content :deep(.ProseMirror h1) {
  font-size: 1.8em;
  font-weight: 700;
  margin: 0.8em 0 0.4em;
}
.editor-content :deep(.ProseMirror h2) {
  font-size: 1.4em;
  font-weight: 600;
  margin: 0.7em 0 0.3em;
}
.editor-content :deep(.ProseMirror code) {
  background: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: monospace;
}
.editor-content :deep(.ProseMirror pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: 4px;
  overflow-x: auto;
}
.editor-content :deep(.ProseMirror table) {
  border-collapse: collapse;
  width: 100%;
}
.editor-content :deep(.ProseMirror td),
.editor-content :deep(.ProseMirror th) {
  border: 1px solid #ddd;
  padding: 6px 10px;
}
.editor-content :deep(.ProseMirror th) {
  background: #f5f5f5;
  font-weight: 600;
}
</style>

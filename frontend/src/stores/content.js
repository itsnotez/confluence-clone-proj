import { defineStore } from 'pinia'
import { ref } from 'vue'
import contentApi from '@/api/content'

export const useContentStore = defineStore('content', () => {
  const currentContent = ref(null)
  const versions = ref([])
  const isEditing = ref(false)

  async function fetchContent(id) {
    try {
      const { data } = await contentApi.getContent(id)
      currentContent.value = data.data
    } catch (err) {
      console.error('fetchContent error:', err)
    }
  }

  async function saveContent(id, body) {
    try {
      const { data } = await contentApi.updateContent(id, { body })
      return data.data
    } catch (err) {
      console.error('saveContent error:', err)
      throw err
    }
  }

  async function publishContent(id, body) {
    try {
      const { data } = await contentApi.publishContent(id, { body })
      currentContent.value = data.data
      return data.data
    } catch (err) {
      console.error('publishContent error:', err)
      throw err
    }
  }

  async function createContent(spaceKey, data) {
    try {
      const { data: res } = await contentApi.createContent(spaceKey, data)
      return res.data
    } catch (err) {
      console.error('createContent error:', err)
      throw err
    }
  }

  async function fetchVersions(id) {
    try {
      const { data } = await contentApi.getVersions(id)
      versions.value = data.data || []
    } catch (err) {
      console.error('fetchVersions error:', err)
      versions.value = []
    }
  }

  return {
    currentContent,
    versions,
    isEditing,
    fetchContent,
    saveContent,
    publishContent,
    createContent,
    fetchVersions
  }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'
import spaceApi from '@/api/space'

export const useSpaceStore = defineStore('space', () => {
  const spaces = ref([])
  const currentSpace = ref(null)
  const contentTree = ref([])
  const mySpacePermission = ref(null)

  async function fetchSpaces() {
    try {
      const { data } = await spaceApi.getSpaces()
      spaces.value = data.data || []
    } catch (err) {
      console.error('fetchSpaces error:', err)
    }
  }

  async function fetchSpace(spaceKey) {
    try {
      const { data } = await spaceApi.getSpace(spaceKey)
      currentSpace.value = data.data
      mySpacePermission.value = data.data?.myPermission ?? null
    } catch (err) {
      console.error('fetchSpace error:', err)
    }
  }

  async function createSpace(formData) {
    await spaceApi.createSpace(formData)
    await fetchSpaces()
  }

  async function deleteSpace(spaceKey) {
    await spaceApi.deleteSpace(spaceKey)
    await fetchSpaces()
  }

  async function toggleFavorite(spaceKey) {
    try {
      await spaceApi.toggleFavorite(spaceKey)
      const space = spaces.value.find(s => s.spaceKey === spaceKey)
      if (space) {
        space.favorited = !space.favorited
      }
    } catch (err) {
      console.error('toggleFavorite error:', err)
    }
  }

  async function fetchContentTree(spaceKey) {
    try {
      const { data } = await spaceApi.getContentTree(spaceKey)
      contentTree.value = data.data || []
    } catch (err) {
      console.error('fetchContentTree error:', err)
      contentTree.value = []
    }
  }

  return {
    spaces,
    currentSpace,
    contentTree,
    mySpacePermission,
    fetchSpaces,
    fetchSpace,
    createSpace,
    deleteSpace,
    toggleFavorite,
    fetchContentTree
  }
})

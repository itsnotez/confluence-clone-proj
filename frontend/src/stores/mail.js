import { defineStore } from 'pinia'
import { ref } from 'vue'
import { mailAccountApi, mailMessageApi } from '@/api/mail'

export const useMailStore = defineStore('mail', () => {
  const accounts = ref([])
  const messages = ref([])
  const selectedMessage = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchAccounts(spaceKey) {
    loading.value = true
    error.value = null
    try {
      const { data } = await mailAccountApi.getAccounts(spaceKey)
      accounts.value = data.data || []
    } catch (err) {
      console.error('fetchAccounts error:', err)
      error.value = err
    } finally {
      loading.value = false
    }
  }

  async function fetchMessages(spaceKey, accountId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await mailMessageApi.getMessages(spaceKey, accountId)
      messages.value = data.data || []
    } catch (err) {
      console.error('fetchMessages error:', err)
      error.value = err
    } finally {
      loading.value = false
    }
  }

  async function convertToPage(spaceKey, accountId, msgId) {
    const { data } = await mailMessageApi.convertToPage(spaceKey, accountId, msgId)
    const idx = messages.value.findIndex(m => m.id === msgId)
    if (idx >= 0) messages.value[idx].linkedContentId = data.data.contentId
    return data.data
  }

  function selectMessage(msg) {
    selectedMessage.value = msg
  }

  return {
    accounts,
    messages,
    selectedMessage,
    loading,
    error,
    fetchAccounts,
    fetchMessages,
    convertToPage,
    selectMessage
  }
})

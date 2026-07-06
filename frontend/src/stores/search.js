import { defineStore } from 'pinia'
import { ref } from 'vue'
import { searchApi } from '@/api/search'

export const useSearchStore = defineStore('search', () => {
  const query = ref('')
  const results = ref([])
  const loading = ref(false)

  async function doSearch(q) {
    if (!q || !q.trim()) {
      results.value = []
      return
    }
    query.value = q
    loading.value = true
    try {
      const res = await searchApi.search(q)
      results.value = res.data.data || []
    } catch (err) {
      console.error('검색 실패:', err)
      results.value = []
    } finally {
      loading.value = false
    }
  }

  return { query, results, loading, doSearch }
})

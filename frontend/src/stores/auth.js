import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const accessToken = ref(localStorage.getItem('accessToken'))
  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(loginId, password) {
    const { data } = await api.post('/auth/login', { loginId, password })
    accessToken.value = data.data.accessToken
    localStorage.setItem('accessToken', data.data.accessToken)
    localStorage.setItem('refreshToken', data.data.refreshToken)
    user.value = data.data.user
  }

  function logout() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async function fetchMe() {
    const { data } = await api.get('/users/me')
    user.value = data.data
  }

  return { user, accessToken, isLoggedIn, login, logout, fetchMe }
})

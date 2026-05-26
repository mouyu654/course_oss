import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getMe } from '@/api/auth.js'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('access_token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const displayName = ref(localStorage.getItem('display_name') || '')
  const userId = ref(localStorage.getItem('user_id') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')
  const isAcademic = computed(() => role.value === 'academic')
  const isDirector = computed(() => role.value === 'director')
  const isTeacher = computed(() => role.value === 'teacher')

  function setAuth(data) {
    token.value = data.access_token
    role.value = data.role
    displayName.value = data.display_name
    userId.value = data.id || ''
    localStorage.setItem('access_token', data.access_token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('display_name', data.display_name)
    localStorage.setItem('user_id', data.id || '')
  }

  function clearAuth() {
    token.value = ''
    role.value = ''
    displayName.value = ''
    userId.value = ''
    localStorage.removeItem('access_token')
    localStorage.removeItem('role')
    localStorage.removeItem('display_name')
    localStorage.removeItem('user_id')
  }

  async function login(username, password) {
    const res = await loginApi(username, password)
    setAuth(res.data)
    return res.data
  }

  async function fetchMe() {
    try {
      const res = await getMe()
      setAuth(res.data)
      return res.data
    } catch {
      clearAuth()
      throw new Error('获取用户信息失败')
    }
  }

  function logout() {
    clearAuth()
  }

  return {
    token,
    role,
    displayName,
    userId,
    isLoggedIn,
    isAdmin,
    isAcademic,
    isDirector,
    isTeacher,
    login,
    logout,
    fetchMe,
    setAuth,
    clearAuth,
  }
})

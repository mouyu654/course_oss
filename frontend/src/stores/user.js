import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.roleCode || '')
  const realName = computed(() => userInfo.value?.realName || '')
  const userId = computed(() => userInfo.value?.id || null)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  // 角色判断
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isAcademic = computed(() => role.value === 'ACADEMIC')
  const isDirector = computed(() => role.value === 'DIRECTOR')
  const isTeacher = computed(() => role.value === 'TEACHER')

  return {
    token, userInfo, isLoggedIn, role, realName, userId,
    isAdmin, isAcademic, isDirector, isTeacher,
    setToken, setUserInfo, logout
  }
})

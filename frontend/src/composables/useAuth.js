import { useUserStore } from '@/stores/user'
import { login as loginApi, changePassword as changePwdApi } from '@/api/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'

export function useAuth() {
  const userStore = useUserStore()
  const router = useRouter()
  const loading = ref(false)

  async function login(username, password) {
    loading.value = true
    try {
      const res = await loginApi({ username, password })
      const { token, userId, username: uname, realName, roleCode } = res.data
      userStore.setToken(token)
      userStore.setUserInfo({ id: userId, username: uname, realName, roleCode })
      ElMessage.success('登录成功')
      // 根据角色跳转到不同首页
      const defaultRoute = getDefaultRoute(roleCode)
      router.push(defaultRoute)
      return true
    } catch (e) {
      return false
    } finally {
      loading.value = false
    }
  }

  async function changePassword(oldPassword, newPassword) {
    try {
      await changePwdApi({ oldPassword, newPassword })
      ElMessage.success('密码修改成功，请重新登录')
      logout()
      return true
    } catch {
      return false
    }
  }

  function logout() {
    userStore.logout()
    router.push('/login')
  }

  return { login, logout, changePassword, loading }
}

function getDefaultRoute(roleCode) {
  const map = {
    ADMIN: '/admin/users',
    ACADEMIC: '/academic/courses',
    DIRECTOR: '/director/grad-req',
    TEACHER: '/teacher/objectives'
  }
  return map[roleCode] || '/login'
}

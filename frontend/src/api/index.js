import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { status } = error.response || {}
    switch (status) {
      case 401:
        localStorage.removeItem('access_token')
        localStorage.removeItem('role')
        localStorage.removeItem('display_name')
        localStorage.removeItem('user_id')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        break
      case 403:
        ElMessage.error('无权限执行此操作')
        break
      case 400:
        ElMessage.error(error.response.data?.detail || '请求参数有误')
        break
      case 409:
        ElMessage.warning(error.response.data?.detail || '存在关联数据，无法删除')
        break
      case 500:
        ElMessage.error('服务器内部错误，请稍后重试')
        break
      default:
        break
    }
    return Promise.reject(error)
  },
)

export default api

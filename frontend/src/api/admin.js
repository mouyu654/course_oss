import api from './index.js'

// 用户管理
export function getUsers() {
  return api.get('/admin/users')
}

export function createUser(data) {
  return api.post('/admin/users', data)
}

export function updateUser(id, data) {
  return api.patch(`/admin/users/${id}`, data)
}

// 全局数据字典
export function getConfig(category) {
  return api.get('/admin/config', { params: { category } })
}

export function createConfig(data) {
  return api.post('/admin/config', data)
}

export function updateConfig(id, data) {
  return api.put(`/admin/config/${id}`, data)
}

export function deleteConfig(id) {
  return api.delete(`/admin/config/${id}`)
}

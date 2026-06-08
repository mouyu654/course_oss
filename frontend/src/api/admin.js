import request from '@/utils/request'

// ===== 用户管理 =====
export function getUsers(params) {
  return request({ url: '/admin/users', method: 'get', params })
}
export function createUser(data) {
  return request({ url: '/admin/users', method: 'post', data })
}
export function updateUser(id, data) {
  return request({ url: `/admin/users/${id}`, method: 'put', data })
}
export function deleteUser(id) {
  return request({ url: `/admin/users/${id}`, method: 'delete' })
}
export function toggleUserStatus(id) {
  return request({ url: `/admin/users/${id}/toggle-status`, method: 'put' })
}
export function getRoles() {
  return request({ url: '/admin/users/roles', method: 'get' })
}

// ===== 数据字典 =====
export function getSemesters() {
  return request({ url: '/dict/semesters', method: 'get' })
}
export function createSemester(data) {
  return request({ url: '/dict/semesters', method: 'post', data })
}
export function updateSemester(id, data) {
  return request({ url: `/dict/semesters/${id}`, method: 'put', data })
}
export function deleteSemester(id) {
  return request({ url: `/dict/semesters/${id}`, method: 'delete' })
}
export function getColleges() {
  return request({ url: '/dict/colleges', method: 'get' })
}
export function createCollege(data) {
  return request({ url: '/dict/colleges', method: 'post', data })
}
export function updateCollege(id, data) {
  return request({ url: `/dict/colleges/${id}`, method: 'put', data })
}
export function deleteCollege(id) {
  return request({ url: `/dict/colleges/${id}`, method: 'delete' })
}
export function getMajors() {
  return request({ url: '/dict/majors', method: 'get' })
}
export function createMajor(data) {
  return request({ url: '/dict/majors', method: 'post', data })
}
export function updateMajor(id, data) {
  return request({ url: `/dict/majors/${id}`, method: 'put', data })
}
export function deleteMajor(id) {
  return request({ url: `/dict/majors/${id}`, method: 'delete' })
}

// ===== 行政班级 =====
export function getAdminClasses(params) {
  return request({ url: '/dict/admin-classes', method: 'get', params })
}
export function createAdminClass(data) {
  return request({ url: '/dict/admin-classes', method: 'post', data })
}
export function updateAdminClass(id, data) {
  return request({ url: `/dict/admin-classes/${id}`, method: 'put', data })
}
export function deleteAdminClass(id) {
  return request({ url: `/dict/admin-classes/${id}`, method: 'delete' })
}
export function getAdminClassStudents(id) {
  return request({ url: `/dict/admin-classes/${id}/students`, method: 'get' })
}
export function addStudentToAdminClass(classId, studentId) {
  return request({ url: `/dict/admin-classes/${classId}/students/${studentId}`, method: 'post' })
}
export function removeStudentFromAdminClass(classId, studentId) {
  return request({ url: `/dict/admin-classes/${classId}/students/${studentId}`, method: 'delete' })
}

// ===== 成绩解锁 =====
export function getAllScores(params) {
  return request({ url: '/admin/scores', method: 'get', params })
}
export function unlockScore(sheetId) {
  return request({ url: `/admin/scores/${sheetId}/unlock`, method: 'post' })
}
export function getScoreDetails(classId) {
  return request({ url: `/admin/scores/${classId}/details`, method: 'get' })
}
export function getUnlockRequests() {
  return request({ url: '/admin/unlock-requests', method: 'get' })
}
export function approveUnlockRequest(id) {
  return request({ url: `/admin/unlock-requests/${id}/approve`, method: 'post' })
}
export function unlockApprovedRequest(id) {
  return request({ url: `/admin/unlock-requests/${id}/unlock`, method: 'post' })
}
export function rejectUnlockRequest(id) {
  return request({ url: `/admin/unlock-requests/${id}/reject`, method: 'post' })
}

import request from '@/utils/request'

// ===== 教师课程列表（课程切换器） =====
export function getMyClasses() {
  return request({ url: '/teaching-classes/my-classes', method: 'get' })
}

// ===== 课程目标 =====
export function getObjectives(classId) {
  return request({ url: `/classes/${classId}/objectives`, method: 'get' })
}
export function createObjective(classId, data) {
  return request({ url: `/classes/${classId}/objectives`, method: 'post', data })
}
export function updateObjective(classId, id, data) {
  return request({ url: `/classes/${classId}/objectives/${id}`, method: 'put', data })
}
export function deleteObjective(classId, id) {
  return request({ url: `/classes/${classId}/objectives/${id}`, method: 'delete' })
}
export function downloadObjectiveTemplate(classId) {
  return request({ url: `/classes/${classId}/objectives/import-template`, method: 'get', responseType: 'blob' })
}
export function importObjectives(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: `/classes/${classId}/objectives/import`, method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
}

// ===== 内部权重 =====
export function getSupportedIndicators(classId) {
  return request({ url: `/classes/${classId}/weights/supported-indicators`, method: 'get' })
}
export function getWeights(classId) {
  return request({ url: `/classes/${classId}/weights`, method: 'get' })
}
export function updateWeights(classId, data) {
  return request({ url: `/classes/${classId}/weights`, method: 'put', data })
}

// ===== 考核点 =====
export function getAssessments(classId) {
  return request({ url: `/classes/${classId}/assessments`, method: 'get' })
}
export function createAssessment(classId, data) {
  return request({ url: `/classes/${classId}/assessments`, method: 'post', data })
}
export function updateAssessment(classId, id, data) {
  return request({ url: `/classes/${classId}/assessments/${id}`, method: 'put', data })
}
export function deleteAssessment(classId, id) {
  return request({ url: `/classes/${classId}/assessments/${id}`, method: 'delete' })
}
export function downloadAssessmentTemplate(classId) {
  return request({ url: `/classes/${classId}/assessments/import-template`, method: 'get', responseType: 'blob' })
}
export function importAssessments(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: `/classes/${classId}/assessments/import`, method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
}

// ===== 成绩管理 =====
export function downloadScoreTemplate(classId) {
  return request({ url: `/classes/${classId}/score-template`, method: 'get', responseType: 'blob' })
}
export function uploadScores(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: `/classes/${classId}/scores/upload`, method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
}
export function getScores(classId) {
  return request({ url: `/classes/${classId}/scores`, method: 'get' })
}
export function updateSingleScore(classId, data) {
  return request({ url: `/classes/${classId}/scores`, method: 'put', data })
}
export function getScoreStatus(classId) {
  return request({ url: `/classes/${classId}/score-status`, method: 'get' })
}

// ===== 计算 =====
export function triggerCourseCompute(classId, operator) {
  return request({ url: `/classes/${classId}/compute`, method: 'post', params: { operator } })
}
export function getComputeResults(classId) {
  return request({ url: `/classes/${classId}/compute/results`, method: 'get' })
}

// ===== 勘误申请 =====
export function requestUnlock(classId, reason) {
  return request({ url: `/classes/${classId}/request-unlock`, method: 'post', data: { reason } })
}
export function getMyUnlockRequests(classId) {
  return request({ url: `/classes/${classId}/my-unlock-requests`, method: 'get' })
}
export function cancelUnlockRequest(classId, requestId) {
  return request({ url: `/classes/${classId}/cancel-unlock-request/${requestId}`, method: 'post' })
}

// ===== 报表 =====
export function getCourseReport(classId) {
  return request({ url: `/reports/course/${classId}`, method: 'get' })
}
export function downloadCoursePdf(classId) {
  return request({ url: `/reports/course/${classId}/pdf`, method: 'get', responseType: 'blob' })
}
export function downloadCourseExcel(classId) {
  return request({ url: `/reports/course/${classId}/excel`, method: 'get', responseType: 'blob' })
}

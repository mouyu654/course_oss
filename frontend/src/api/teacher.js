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

// ===== 成绩管理 =====
export function downloadScoreTemplate(classId) {
  return request({ url: `/classes/${classId}/score-template`, method: 'get', responseType: 'blob' })
}
export function uploadScores(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: `/classes/${classId}/scores/upload`, method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
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
export function triggerCourseCompute(classId) {
  return request({ url: `/classes/${classId}/compute`, method: 'post' })
}
export function getComputeResults(classId) {
  return request({ url: `/classes/${classId}/compute/results`, method: 'get' })
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

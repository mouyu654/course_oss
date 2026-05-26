import api from './index.js'

// 课程目标
export function getObjectives(courseId) {
  return api.get('/teacher/objectives', { params: { course_id: courseId } })
}

export function createObjective(data) {
  return api.post('/teacher/objectives', data)
}

export function deleteObjective(id) {
  return api.delete(`/teacher/objectives/${id}`)
}

// 内部权重
export function getInternalWeights(courseId) {
  return api.get('/teacher/internal-weights', { params: { course_id: courseId } })
}

export function submitInternalWeights(courseId, data) {
  return api.post('/teacher/internal-weights', data, { params: { course_id: courseId } })
}

// 考核点
export function getAssessmentItems(courseId) {
  return api.get('/teacher/assessment-items', { params: { course_id: courseId } })
}

export function createAssessmentItem(data) {
  return api.post('/teacher/assessment-items', data)
}

export function deleteAssessmentItem(id) {
  return api.delete(`/teacher/assessment-items/${id}`)
}

// 成绩模板导出
export function exportTemplate(courseId) {
  return api.get(`/teacher/assessment-items/${courseId}/export-template`, { responseType: 'blob' })
}

// 成绩导入与矩阵
export function importScores(courseId, classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/teacher/scores/import', formData, {
    params: { course_id: courseId, class_id: classId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getScoresMatrix(courseId, classId) {
  return api.get('/teacher/scores/matrix', { params: { course_id: courseId, class_id: classId } })
}

// 课程级计算
export function calculateCourse(courseId, classId) {
  return api.post('/teacher/calculate', null, { params: { course_id: courseId, class_id: classId } })
}

// 课程级报告
export function getTeacherResults(courseId, classId) {
  return api.get('/teacher/results', {
    params: { course_id: courseId, class_id: classId },
    responseType: 'blob',
  })
}

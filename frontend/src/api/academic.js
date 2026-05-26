import api from './index.js'

// 课程管理
export function getCourses() {
  return api.get('/academic/courses')
}

export function createCourse(data) {
  return api.post('/academic/courses', data)
}

export function importCourses(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/academic/courses/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteCourse(id) {
  return api.delete(`/academic/courses/${id}`)
}

// 教学班管理
export function getClasses(courseId) {
  return api.get('/academic/classes', { params: { course_id: courseId } })
}

export function createClass(data) {
  return api.post('/academic/classes', data)
}

// 学生管理
export function getStudents(classId) {
  return api.get('/academic/students', { params: { class_id: classId } })
}

export function importStudents(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/academic/students/import', formData, {
    params: { class_id: classId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 计算进度
export function getCalculationProgress() {
  return api.get('/academic/calculation/progress')
}

// 专业级全局计算
export function calculateProgram() {
  return api.post('/academic/calculate/program')
}

// 成绩解锁
export function unlockScore(data) {
  return api.post('/academic/unlock', data)
}

// 专业级报告
export function getProgramResults() {
  return api.get('/academic/results/program')
}

export function exportProgramExcel() {
  return api.get('/academic/reports/program-excel', { responseType: 'blob' })
}

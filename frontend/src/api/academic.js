import request from '@/utils/request'

// ===== 课程管理 =====
export function getCourses(params) {
  return request({ url: '/courses', method: 'get', params })
}
export function createCourse(data) {
  return request({ url: '/courses', method: 'post', data })
}
export function updateCourse(id, data) {
  return request({ url: `/courses/${id}`, method: 'put', data })
}
export function deleteCourse(id) {
  return request({ url: `/courses/${id}`, method: 'delete' })
}
export function getCourseClasses(courseId) {
  return request({ url: `/courses/${courseId}/classes`, method: 'get' })
}

// ===== 学生管理 =====
export function importStudents(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/students/import', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function getStudents(params) {
  return request({ url: '/students', method: 'get', params })
}
export function createStudent(data) {
  return request({ url: '/students', method: 'post', data })
}
export function updateStudent(id, data) {
  return request({ url: `/students/${id}`, method: 'put', data })
}
export function deleteStudent(id) {
  return request({ url: `/students/${id}`, method: 'delete' })
}
export function batchUpdateStudentStatus(data) {
  return request({ url: '/students/batch-status', method: 'put', data })
}

// ===== 教学班级管理 =====
export function getTeachingClasses(params) {
  return request({ url: '/teaching-classes', method: 'get', params })
}
export function createTeachingClass(data) {
  return request({ url: '/teaching-classes', method: 'post', data })
}
export function updateTeachingClass(id, data) {
  return request({ url: `/teaching-classes/${id}`, method: 'put', data })
}
export function deleteTeachingClass(id) {
  return request({ url: `/teaching-classes/${id}`, method: 'delete' })
}
export function getTeachingClassStudents(classId) {
  return request({ url: `/teaching-classes/${classId}/students`, method: 'get' })
}
export function addTeachingClassStudent(classId, studentId) {
  return request({ url: `/teaching-classes/${classId}/students/${studentId}`, method: 'post' })
}
export function removeTeachingClassStudent(classId, studentId) {
  return request({ url: `/teaching-classes/${classId}/students/${studentId}`, method: 'delete' })
}

// ===== 全局计算 =====
export function getEnrollmentYears() {
  return request({ url: '/global/enrollment-years', method: 'get' })
}
export function getGlobalDashboard(params) {
  return request({ url: '/global/dashboard', method: 'get', params })
}
export function triggerGlobalCompute(params) {
  return request({ url: '/global/compute', method: 'post', params })
}
export function getGlobalResults(params) {
  return request({ url: '/global/results', method: 'get', params })
}

// ===== 报表 =====
export function getCalcBatches(params) {
  return request({ url: '/reports/major/batches', method: 'get', params })
}
export function getCalcRadarData(params) {
  return request({ url: '/reports/major/data', method: 'get', params })
}
export function deleteCalcBatch(params) {
  return request({ url: '/reports/major/data', method: 'delete', params })
}
export function downloadMajorExcel(params) {
  return request({ url: '/reports/major/excel', method: 'get', params, responseType: 'blob' })
}

// ===== 批量导入 =====
export function downloadStudentTemplate() {
  return request({ url: '/batch-import/templates/students', method: 'get', responseType: 'blob' })
}
export function downloadCourseTemplate() {
  return request({ url: '/batch-import/templates/courses', method: 'get', responseType: 'blob' })
}
export function downloadClassStudentTemplate() {
  return request({ url: '/batch-import/templates/class-students', method: 'get', responseType: 'blob' })
}
export function batchImportStudents(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/batch-import/students', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function batchImportCourses(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/batch-import/courses', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function batchImportAdminClassStudents(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: `/batch-import/admin-classes/${classId}/students`, method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function batchImportTeachingClassStudents(classId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: `/batch-import/teaching-classes/${classId}/students`, method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}

import request from '@/utils/request'

// ===== 毕业要求管理（需要 majorId） =====
export function getGradReqs(majorId) {
  return request({ url: '/grad-req', method: 'get', params: { majorId } })
}
export function createGradReq(data) {
  return request({ url: '/grad-req', method: 'post', data })
}
export function updateGradReq(id, data) {
  return request({ url: `/grad-req/${id}`, method: 'put', data })
}
export function deleteGradReq(id) {
  return request({ url: `/grad-req/${id}`, method: 'delete' })
}
export function createIndicator(gradReqId, data) {
  return request({ url: `/grad-req/${gradReqId}/indicators`, method: 'post', data })
}
export function updateIndicator(id, data) {
  return request({ url: `/grad-req/indicators/${id}`, method: 'put', data })
}
export function deleteIndicator(id) {
  return request({ url: `/grad-req/indicators/${id}`, method: 'delete' })
}

// ===== 宏观支撑矩阵（需要 majorId） =====
export function getMacroMatrix(majorId) {
  return request({ url: '/macro-matrix', method: 'get', params: { majorId } })
}
export function updateMacroMatrix(data) {
  return request({ url: '/macro-matrix', method: 'put', data })
}
export function getSupportedIndicators(courseId) {
  return request({ url: `/macro-matrix/course/${courseId}/supported-indicators`, method: 'get' })
}

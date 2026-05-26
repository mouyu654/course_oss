import api from './index.js'

// 毕业要求
export function getRequirements() {
  return api.get('/director/requirements')
}

export function createRequirement(data) {
  return api.post('/director/requirements', data)
}

export function deleteRequirement(id) {
  return api.delete(`/director/requirements/${id}`)
}

// 指标点
export function getIndicators(requirementId) {
  return api.get('/director/indicators', { params: { requirement_id: requirementId } })
}

export function createIndicator(data) {
  return api.post('/director/indicators', data)
}

export function deleteIndicator(id) {
  return api.delete(`/director/indicators/${id}`)
}

// 宏观支撑矩阵
export function getMacroMatrix() {
  return api.get('/director/macro-matrix')
}

export function submitMacroMatrix(data) {
  return api.post('/director/macro-matrix', data)
}

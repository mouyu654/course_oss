/**
 * 权重归一化校验
 * @param {number} sum - 当前权重总和
 * @param {number} tolerance - 容差阈值，默认 0.01
 * @returns {boolean} 是否通过校验
 */
export function isValidWeight(sum, tolerance = 0.01) {
  return Math.abs(sum - 1.0) <= tolerance
}

/**
 * 校验权重值是否在合法范围 [0, 1]
 * @param {number} value
 * @returns {boolean}
 */
export function isValidWeightValue(value) {
  return typeof value === 'number' && !isNaN(value) && value >= 0 && value <= 1
}

/**
 * 表单必填校验规则
 */
export const requiredRule = (message = '此项为必填项') => ({
  required: true,
  message,
  trigger: 'blur'
})

/**
 * 数字范围校验规则
 */
export const rangeRule = (min, max, message) => ({
  type: 'number',
  min,
  max,
  message: message || `请输入 ${min} ~ ${max} 之间的数值`,
  trigger: 'blur'
})

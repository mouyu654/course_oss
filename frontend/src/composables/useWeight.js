import { ref, computed } from 'vue'
import { isValidWeight } from '@/utils/validate'

/**
 * 权重矩阵校验 composable
 * @param {import('vue').Ref<Object>} matrixRef - 响应式矩阵数据 { rowKey: { colKey: weightValue } }
 * @param {string[]} columnKeys - 列（指标点）ID 列表
 */
export function useWeight(matrixRef, columnKeys) {
  const columnSums = computed(() => {
    const sums = {}
    for (const col of columnKeys.value) {
      let sum = 0
      for (const row of Object.values(matrixRef.value)) {
        const val = row[col]
        if (val !== null && val !== undefined && val !== '') {
          sum += parseFloat(val) || 0
        }
      }
      sums[col] = Math.round(sum * 10000) / 10000
    }
    return sums
  })

  const columnValid = computed(() => {
    const valid = {}
    for (const col of columnKeys.value) {
      valid[col] = isValidWeight(columnSums.value[col])
    }
    return valid
  })

  const allValid = computed(() => {
    return Object.values(columnValid.value).every(v => v)
  })

  /**
   * 获取列合计的 CSS 类名
   * @param {string} colKey
   * @returns {string}
   */
  function getSumClass(colKey) {
    return columnValid.value[colKey] ? '' : 'weight-invalid'
  }

  return { columnSums, columnValid, allValid, getSumClass }
}

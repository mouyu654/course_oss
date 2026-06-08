<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMajors } from '@/api/admin'
import { getMacroMatrix, updateMacroMatrix } from '@/api/director'
import { getCourses } from '@/api/academic'
import { getGradReqs } from '@/api/director'
import { ElMessage } from 'element-plus'
import { isValidWeight } from '@/utils/validate'

/* ---------- 选择器 ---------- */
const majorOptions = ref([])
const selectedMajorId = ref(null)

/* ---------- 矩阵数据 ---------- */
const loading = ref(false)
const courses = ref([])       // [{ id, courseName }]
const indicators = ref([])    // [{ id, indicatorNo, content }]
const weights = ref({})       // { `${courseId}_${indicatorId}`: number }

/* ---------- 选择器初始化 ---------- */
onMounted(() => { loadMajors() })
watch(selectedMajorId, (v) => { if (v) loadData(); else resetMatrix() })

async function loadMajors() {
  try {
    const res = await getMajors()
    majorOptions.value = res.data?.records || res.data || []
  } catch { /* ignore */ }
}

function resetMatrix() {
  courses.value = []
  indicators.value = []
  weights.value = {}
}

/* ---------- 加载并转置矩阵数据 ---------- */
async function loadData() {
  if (!selectedMajorId.value) return
  loading.value = true
  try {
    const res = await getMacroMatrix(selectedMajorId.value)
    const list = res.data || []
    if (list.length === 0) {
      resetMatrix()
      return
    }
    buildPivot(list)
  } finally {
    loading.value = false
  }
}

async function buildPivot(list) {
  // 收集唯一的 courseId / indicatorId
  const courseIds = [...new Set(list.map(r => r.courseId))]
  const indicatorIds = [...new Set(list.map(r => r.indicatorId))]

  // 探测后端是否已返回名称字段
  const hasCourseName = list.some(r => r.courseName)
  const hasIndicatorNo = list.some(r => r.indicatorNo)

  // 构建课程映射
  let courseMap = {}
  if (hasCourseName) {
    for (const r of list) {
      if (r.courseName && !courseMap[r.courseId]) {
        courseMap[r.courseId] = { id: r.courseId, courseName: r.courseName }
      }
    }
  }

  // 构建指标点映射
  let indMap = {}
  if (hasIndicatorNo) {
    for (const r of list) {
      if (r.indicatorNo && !indMap[r.indicatorId]) {
        indMap[r.indicatorId] = { id: r.indicatorId, indicatorNo: r.indicatorNo, content: r.content || '' }
      }
    }
  }

  // 如果后端未返回名称，通过额外 API 获取
  const fallbackPromises = []
  if (!hasCourseName) {
    fallbackPromises.push(
      getCourses({ size: 999 }).then(r => {
        const rows = r.data?.records || r.data || []
        for (const c of rows) {
          if (courseIds.includes(c.id)) {
            courseMap[c.id] = { id: c.id, courseName: c.name || c.courseName }
          }
        }
      })
    )
  }
  if (!hasIndicatorNo) {
    fallbackPromises.push(
      getGradReqs(selectedMajorId.value).then(r => {
        const reqs = r.data || []
        for (const req of reqs) {
          for (const ind of (req.indicators || [])) {
            if (indicatorIds.includes(ind.id)) {
              indMap[ind.id] = { id: ind.id, indicatorNo: ind.indicatorNo, content: ind.content || '' }
            }
          }
        }
      })
    )
  }
  if (fallbackPromises.length) {
    await Promise.all(fallbackPromises)
  }

  // 排序课程：按 ID 升序
  courses.value = courseIds
    .map(id => courseMap[id] || { id, courseName: `课程#${id}` })
    .sort((a, b) => a.id - b.id)

  // 排序指标点：按编号自然排序
  indicators.value = indicatorIds
    .map(id => indMap[id] || { id, indicatorNo: `${id}`, content: '' })
    .sort((a, b) => String(a.indicatorNo).localeCompare(String(b.indicatorNo), undefined, { numeric: true }))

  // 填充权重
  const wMap = {}
  for (const r of list) {
    wMap[`${r.courseId}_${r.indicatorId}`] = r.weight
  }
  weights.value = wMap
}

/* ---------- 权重读写 ---------- */
function getWeight(courseId, indicatorId) {
  const v = weights.value[`${courseId}_${indicatorId}`]
  return v !== null && v !== undefined ? v : ''
}

function setWeight(courseId, indicatorId, value) {
  const num = value === '' || value === null ? null : parseFloat(value)
  weights.value[`${courseId}_${indicatorId}`] = isNaN(num) ? null : num
}

/* ---------- 列合计与校验 ---------- */
const columnSums = computed(() => {
  const sums = {}
  for (const ind of indicators.value) {
    let sum = 0
    for (const c of courses.value) {
      const w = weights.value[`${c.id}_${ind.id}`]
      if (w !== null && w !== undefined) {
        sum += w
      }
    }
    sums[ind.id] = Math.round(sum * 10000) / 10000
  }
  return sums
})

const allValid = computed(() => {
  if (indicators.value.length === 0) return false
  return indicators.value.every(ind => isValidWeight(columnSums.value[ind.id]))
})

const hasData = computed(() => courses.value.length > 0 && indicators.value.length > 0)

/* ---------- 提交 ---------- */
async function handleSubmit() {
  if (!allValid.value) {
    ElMessage.warning('请确保每列权重合计为 1.0')
    return
  }
  loading.value = true
  try {
    const items = []
    for (const c of courses.value) {
      for (const ind of indicators.value) {
        const w = weights.value[`${c.id}_${ind.id}`]
        if (w !== null && w !== undefined) {
          items.push({ courseId: c.id, indicatorId: ind.id, weight: w })
        }
      }
    }
    await updateMacroMatrix(items)
    ElMessage.success('宏观支撑矩阵保存成功')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <!-- 专业选择器 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <div style="display: flex; align-items: center; gap: 12px;">
        <span style="font-weight: 500; white-space: nowrap;">选择专业：</span>
        <el-select
          v-model="selectedMajorId"
          placeholder="请选择专业"
          filterable
          style="width: 320px;"
        >
          <el-option
            v-for="m in majorOptions"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>
      </div>
    </el-card>

    <!-- 矩阵表格 -->
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <h3>宏观支撑矩阵配置</h3>
          <el-button
            type="primary"
            :disabled="!selectedMajorId || !allValid"
            @click="handleSubmit"
          >
            提交生效
          </el-button>
        </div>
      </template>

      <template v-if="selectedMajorId">
        <div v-if="hasData" class="matrix-wrap">
          <table class="matrix-table">
            <thead>
              <tr>
                <th class="col-course">课程</th>
                <th
                  v-for="ind in indicators"
                  :key="ind.id"
                  class="col-indicator"
                >
                  <span>{{ ind.indicatorNo }}</span>
                  <br v-if="ind.content" />
                  <span v-if="ind.content" class="indicator-desc">{{ ind.content }}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="course in courses" :key="course.id">
                <td class="col-course">{{ course.courseName }}</td>
                <td
                  v-for="ind in indicators"
                  :key="ind.id"
                  class="cell-input"
                >
                  <el-input
                    :model-value="getWeight(course.id, ind.id)"
                    @update:model-value="v => setWeight(course.id, ind.id, v)"
                    size="small"
                    style="width: 72px;"
                    placeholder="-"
                  />
                </td>
              </tr>
              <!-- 列合计行 -->
              <tr class="sum-row">
                <td class="col-course">列合计</td>
                <td
                  v-for="ind in indicators"
                  :key="ind.id"
                  class="sum-cell"
                  :class="{ 'weight-invalid': !isValidWeight(columnSums[ind.id]) }"
                >
                  {{ columnSums[ind.id] }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <el-empty v-else description="暂无数据，请先配置毕业要求指标点和课程" />
      </template>

      <el-empty v-else description="请先选择专业" />
    </el-card>
  </div>
</template>

<style scoped>
.matrix-wrap {
  overflow-x: auto;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.matrix-table th,
.matrix-table td {
  padding: 10px 8px;
  border: 1px solid #ebeef5;
  text-align: center;
  white-space: nowrap;
}

.col-course {
  text-align: left;
  min-width: 140px;
  padding-left: 12px !important;
  padding-right: 12px !important;
  font-weight: 500;
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 1;
}

.matrix-table thead .col-course {
  background: #f5f7fa;
}

.col-indicator {
  min-width: 80px;
  background: #f5f7fa;
}

.indicator-desc {
  font-size: 11px;
  font-weight: 400;
  color: #909399;
}

.cell-input {
  text-align: center;
}

.sum-row {
  background-color: #fafafa;
  font-weight: 600;
}

.sum-row .col-course {
  background: #fafafa;
}

.sum-cell {
  font-weight: 600;
}
</style>

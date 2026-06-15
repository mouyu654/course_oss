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
    // 始终加载全部课程和全部指标点，再覆盖已有权重
    const [matrixRes, courseRes, gradRes] = await Promise.all([
      getMacroMatrix(selectedMajorId.value),
      getCourses({ majorId: selectedMajorId.value, size: 999 }),
      getGradReqs(selectedMajorId.value)
    ])
    const matrixList = matrixRes.data || []

    // 构建本专业关联的课程
    const allCourses = (courseRes.data?.records || courseRes.data || [])
    courses.value = allCourses.map(c => ({ id: c.id, courseName: c.name || c.courseName }))
      .sort((a, b) => a.id - b.id)

    // 构建全部指标点
    const reqs = gradRes.data || []
    const allIndicators = []
    for (const req of reqs) {
      for (const ind of (req.indicators || [])) {
        allIndicators.push({ id: ind.id, indicatorNo: ind.indicatorNo, content: ind.content || '' })
      }
    }
    indicators.value = allIndicators.sort((a, b) =>
      String(a.indicatorNo).localeCompare(String(b.indicatorNo), undefined, { numeric: true })
    )

    // 填充已有权重
    const wMap = {}
    for (const r of matrixList) {
      wMap[`${r.courseId}_${r.indicatorId}`] = r.weight
    }
    weights.value = wMap
  } finally {
    loading.value = false
  }
}

/* ---------- 权重读写 ---------- */
function getWeight(courseId, indicatorId) {
  const v = weights.value[`${courseId}_${indicatorId}`]
  return v !== null && v !== undefined ? v : undefined
}

function setWeight(courseId, indicatorId, value) {
  if (value === '' || value === null || value === undefined) {
    weights.value[`${courseId}_${indicatorId}`] = undefined
    return
  }
  const num = parseFloat(value)
  weights.value[`${courseId}_${indicatorId}`] = isNaN(num) ? undefined : num
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
                  <el-input-number
                    :model-value="getWeight(course.id, ind.id)"
                    @update:model-value="v => setWeight(course.id, ind.id, v)"
                    :min="0"
                    :max="1"
                    :step="0.05"
                    :precision="4"
                    :controls="false"
                    size="small"
                    style="width: 85px;"
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

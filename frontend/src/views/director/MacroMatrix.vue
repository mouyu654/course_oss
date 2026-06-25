<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMajors } from '@/api/admin'
import { getMacroMatrix, updateMacroMatrix } from '@/api/director'
import { getCourses } from '@/api/academic'
import { getGradReqs } from '@/api/director'
import { ElMessage } from 'element-plus'
import { isValidWeight } from '@/utils/validate'
import { Search, RefreshRight } from '@element-plus/icons-vue'

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
    <el-card class="main-card filter-card">
      <div class="filter-section">
        <div class="filter-item">
          <span class="filter-label">选择专业：</span>
          <el-select
            v-model="selectedMajorId"
            placeholder="请选择专业"
            filterable
            class="major-select"
          >
            <el-option
              v-for="m in majorOptions"
              :key="m.id"
              :label="m.name"
              :value="m.id"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 矩阵表格 -->
    <el-card v-loading="loading" class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #059669 0%, #10B981 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">宏观支撑矩阵配置</h3>
              <p class="header-subtitle">配置课程对毕业要求指标点的支撑权重</p>
            </div>
          </div>
          <div class="header-actions">
            <el-button
              type="primary"
              :disabled="!selectedMajorId || !allValid"
              @click="handleSubmit"
            >
              <el-icon><Check /></el-icon>
              提交生效
            </el-button>
          </div>
        </div>
      </template>

      <template v-if="selectedMajorId">
        <div v-if="hasData" class="matrix-wrapper">
          <table class="matrix-table">
            <thead>
              <tr>
                <th class="col-course">课程</th>
                <th
                  v-for="ind in indicators"
                  :key="ind.id"
                  class="col-indicator"
                >
                  <div class="indicator-no">{{ ind.indicatorNo }}</div>
                  <div v-if="ind.content" class="indicator-desc">{{ ind.content }}</div>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="course in courses" :key="course.id">
                <td class="col-course">
                  <span class="course-name">{{ course.courseName }}</span>
                </td>
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
                    class="weight-input"
                    placeholder="-"
                  />
                </td>
              </tr>
              <!-- 列合计行 -->
              <tr class="sum-row">
                <td class="col-course sum-label">
                  <span class="sum-icon">Σ</span>
                  列合计
                </td>
                <td
                  v-for="ind in indicators"
                  :key="ind.id"
                  class="sum-cell"
                  :class="{ 'sum-invalid': !isValidWeight(columnSums[ind.id]), 'sum-valid': isValidWeight(columnSums[ind.id]) }"
                >
                  {{ columnSums[ind.id] }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <el-empty v-else description="暂无数据，请先配置毕业要求指标点和课程" class="empty-state" />
      </template>

      <el-empty v-else description="请先选择专业" class="empty-state" />
    </el-card>
  </div>
</template>

<style scoped>
/* ===== Filter Card ===== */
.filter-card :deep(.el-card__body) {
  padding: 16px 24px;
}

.filter-section {
  background: #F8FAFC;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #F1F5F9;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-weight: 500;
  color: #1E293B;
  white-space: nowrap;
}

.major-select {
  width: 320px;
}

/* ===== Card Styles ===== */
.main-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.main-card :deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #F1F5F9;
}

.main-card :deep(.el-card__body) {
  padding: 24px;
}

/* ===== Page Header ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 4px 0;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.header-accent {
  width: 4px;
  height: 48px;
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 2px;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1E293B;
  line-height: 1.3;
}

.header-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748B;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* ===== Matrix ===== */
.matrix-wrapper {
  overflow-x: auto;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.matrix-table th,
.matrix-table td {
  padding: 12px 10px;
  border: 1px solid #E2E8F0;
  text-align: center;
  white-space: nowrap;
}

.col-course {
  text-align: left;
  min-width: 160px;
  padding-left: 16px !important;
  padding-right: 16px !important;
  font-weight: 500;
  position: sticky;
  left: 0;
  background: #FFFFFF;
  z-index: 1;
}

.matrix-table thead .col-course {
  background: #F8FAFC;
}

.col-indicator {
  min-width: 90px;
  background: #F8FAFC;
}

.indicator-no {
  font-weight: 600;
  color: #1E293B;
  font-size: 13px;
}

.indicator-desc {
  font-size: 11px;
  font-weight: 400;
  color: #64748B;
  margin-top: 4px;
  white-space: normal;
  max-width: 120px;
}

.course-name {
  font-weight: 500;
  color: #1E293B;
}

.cell-input {
  text-align: center;
  vertical-align: middle;
}

.weight-input {
  width: 90px;
}

.weight-input :deep(.el-input__wrapper) {
  border-radius: 6px;
}

.sum-row {
  background-color: #F8FAFC;
  font-weight: 600;
}

.sum-row .col-course {
  background: #F8FAFC !important;
}

.sum-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sum-icon {
  font-size: 16px;
  color: #64748B;
}

.sum-cell {
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 13px;
}

.sum-valid {
  color: #059669;
  background: #ECFDF5;
}

.sum-invalid {
  color: #DC2626;
  background: #FEF2F2;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 40px 0;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    flex-direction: column;
    align-items: stretch;
  }

  .major-select {
    width: 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent {
    transition: none;
  }
}
</style>

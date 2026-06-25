<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMyClasses, getObjectives, getSupportedIndicators, getWeights, updateWeights } from '@/api/teacher'
import { ElMessage } from 'element-plus'
import { isValidWeight } from '@/utils/validate'

/* ---- state ---- */
const myClasses = ref([])
const selectedClassId = ref(null)
const loading = ref(false)
const saving = ref(false)
const objectives = ref([])
const indicators = ref([])

// weight matrix keyed by "objectiveId_indicatorId"
const weightMap = ref({})

/* ---- lifecycle ---- */
onMounted(async () => {
  const res = await getMyClasses()
  myClasses.value = res.data || []
  if (myClasses.value.length) {
    selectedClassId.value = myClasses.value[0].id
  }
})

watch(selectedClassId, (v) => { if (v) loadData() })

/* ---- data loading ---- */
async function loadData() {
  if (!selectedClassId.value) return
  loading.value = true
  try {
    const [objRes, indRes, wRes] = await Promise.all([
      getObjectives(selectedClassId.value),
      getSupportedIndicators(selectedClassId.value),
      getWeights(selectedClassId.value)
    ])
    objectives.value = objRes.data || []
    indicators.value = indRes.data || []

    // pre-fill weight matrix from existing weights
    const map = {}
    for (const w of (wRes.data || [])) {
      map[`${w.objectiveId}_${w.indicatorId}`] = w.weight
    }
    weightMap.value = map
  } finally {
    loading.value = false
  }
}

/* ---- matrix helpers ---- */
function getCell(objId, indId) {
  return weightMap.value[`${objId}_${indId}`] ?? undefined
}

function setCell(objId, indId, val) {
  if (val === '' || val === null || val === undefined) {
    weightMap.value[`${objId}_${indId}`] = undefined
    return
  }
  const num = parseFloat(val)
  weightMap.value[`${objId}_${indId}`] = isNaN(num) ? undefined : num
}

const columnSums = computed(() => {
  const sums = {}
  for (const ind of indicators.value) {
    let sum = 0
    for (const obj of objectives.value) {
      const w = getCell(obj.id, ind.id)
      if (w !== '' && w !== null && w !== undefined) {
        sum += parseFloat(w) || 0
      }
    }
    sums[ind.id] = Math.round(sum * 10000) / 10000
  }
  return sums
})

const allValid = computed(() => {
  if (!indicators.value.length) return false
  return indicators.value.every(ind => isValidWeight(columnSums.value[ind.id]))
})

/* ---- save ---- */
async function handleSubmit() {
  if (!allValid.value) {
    ElMessage.warning('每列权重合计必须等于 1.0，请检查')
    return
  }
  saving.value = true
  try {
    const items = []
    for (const obj of objectives.value) {
      for (const ind of indicators.value) {
        const w = getCell(obj.id, ind.id)
        if (w !== '' && w !== null && w !== undefined && parseFloat(w) > 0) {
          items.push({
            objectiveId: obj.id,
            indicatorId: ind.id,
            weight: parseFloat(w)
          })
        }
      }
    }
    await updateWeights(selectedClassId.value, items)
    ElMessage.success('内部权重保存成功')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #7C3AED 0%, #8B5CF6 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">内部权重分配</h3>
              <p class="header-subtitle">分配课程目标对毕业要求指标点的支撑权重</p>
            </div>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" class="class-selector">
              <el-option
                  v-for="c in myClasses"
                  :key="c.id"
                  :label="`${c.courseName} - ${c.className}`"
                  :value="c.id"
              />
            </el-select>
            <el-button
                type="primary"
                :disabled="!selectedClassId || !allValid"
                :loading="saving"
                @click="handleSubmit"
                class="save-button"
            >
              <el-icon><Check /></el-icon>
              保存权重
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="indicators.length" v-loading="loading" class="matrix-wrapper">
        <table class="weight-matrix">
          <thead>
          <tr>
            <th class="col-label">课程目标</th>
            <th v-for="ind in indicators" :key="ind.id" class="col-header">
              <div class="indicator-no">{{ ind.indicatorNo }}</div>
              <div class="indicator-hint">{{ ind.content }}</div>
            </th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="obj in objectives" :key="obj.id">
            <td class="col-label">
              <div class="objective-content">
                <span class="obj-no">{{ obj.objNo }}</span>
                <span class="obj-desc" :title="obj.description">{{ obj.description }}</span>
              </div>
            </td>
            <td v-for="ind in indicators" :key="ind.id" class="cell">
              <el-input-number
                  :model-value="getCell(obj.id, ind.id)"
                  @update:model-value="v => setCell(obj.id, ind.id, v)"
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
          <!-- column sum row -->
          <tr class="sum-row">
            <td class="col-label sum-label">
              <span class="sum-icon">Σ</span>
              列合计
            </td>
            <td
                v-for="ind in indicators"
                :key="ind.id"
                class="sum-cell"
                :class="{ 'sum-invalid': !isValidWeight(columnSums[ind.id]), 'sum-valid': isValidWeight(columnSums[ind.id]) }"
            >
              {{ columnSums[ind.id] ?? '-' }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-state">
        <el-empty description="请先设定课程目标并配置宏观支撑矩阵" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
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

.class-selector {
  width: 280px;
}

.save-button {
  background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
  border: none;
}

.save-button:hover {
  background: linear-gradient(135deg, #6D28D9 0%, #7C3AED 100%);
}

/* ===== Card Styles ===== */
.main-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.main-card :deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #F1F5F9;
}

.main-card :deep(.el-card__body) {
  padding: 24px;
}

/* ===== Matrix Styles ===== */
.matrix-wrapper {
  overflow-x: auto;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
}

.weight-matrix {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.weight-matrix th,
.weight-matrix td {
  padding: 12px 10px;
  border: 1px solid #E2E8F0;
  text-align: center;
  white-space: nowrap;
}

.col-label {
  text-align: left;
  min-width: 180px;
  max-width: 260px;
  white-space: normal;
  position: sticky;
  left: 0;
  background: #FFFFFF;
  z-index: 1;
}

.objective-content {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.col-header {
  min-width: 100px;
  background: #F8FAFC;
}

.indicator-no {
  font-weight: 600;
  color: #1E293B;
  font-size: 13px;
}

.indicator-hint {
  font-size: 11px;
  color: #64748B;
  margin-top: 4px;
  white-space: normal;
  max-width: 120px;
  line-height: 1.4;
}

.obj-no {
  font-weight: 600;
  margin-right: 8px;
  color: #7C3AED;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.obj-desc {
  color: #475569;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.cell {
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

.sum-label {
  background: #F8FAFC !important;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sum-icon {
  font-size: 16px;
  color: #64748B;
}

.sum-cell {
  font-size: 14px;
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
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
  padding: 60px 0;
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

  .class-selector {
    width: 100%;
  }

  .save-button {
    width: 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .weight-input :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>

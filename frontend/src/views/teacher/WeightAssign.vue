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
  return weightMap.value[`${objId}_${indId}`] ?? ''
}

function setCell(objId, indId, val) {
  const num = val === '' ? '' : parseFloat(val)
  weightMap.value[`${objId}_${indId}`] = isNaN(num) ? 0 : num
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
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>内部权重分配</h3>
          <div style="display: flex; gap: 12px; align-items: center;">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" style="width: 280px;">
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
            >
              保存
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
                <div>{{ ind.indicatorNo }}</div>
                <div class="indicator-hint">{{ ind.content }}</div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="obj in objectives" :key="obj.id">
              <td class="col-label">
                <span class="obj-no">{{ obj.objNo }}</span>
                <span class="obj-desc">{{ obj.description }}</span>
              </td>
              <td v-for="ind in indicators" :key="ind.id" class="cell">
                <el-input
                  :model-value="getCell(obj.id, ind.id)"
                  @update:model-value="v => setCell(obj.id, ind.id, v)"
                  size="small"
                  style="width: 72px;"
                  placeholder="-"
                />
              </td>
            </tr>
            <!-- column sum row -->
            <tr class="sum-row">
              <td class="col-label sum-label">列合计</td>
              <td
                v-for="ind in indicators"
                :key="ind.id"
                class="sum-cell"
                :class="{ 'sum-invalid': !isValidWeight(columnSums[ind.id]) }"
              >
                {{ columnSums[ind.id] ?? '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <el-empty v-else description="请先设定课程目标并配置宏观支撑矩阵" />
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 {
  margin: 0;
  font-size: 16px;
}

.matrix-wrapper {
  overflow-x: auto;
}

.weight-matrix {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.weight-matrix th,
.weight-matrix td {
  padding: 10px 8px;
  border: 1px solid #ebeef5;
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
  background: #fff;
  z-index: 1;
}

.col-header {
  min-width: 90px;
  background: #f5f7fa;
}

.indicator-hint {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  white-space: normal;
  max-width: 120px;
}

.obj-no {
  font-weight: 600;
  margin-right: 6px;
}

.obj-desc {
  color: #606266;
}

.cell {
  vertical-align: middle;
}

.sum-row {
  background-color: #fafafa;
  font-weight: 600;
}

.sum-label {
  background: #fafafa !important;
}

.sum-cell {
  font-size: 14px;
}

.sum-invalid {
  color: #f56c6c;
  background: #fef0f0;
}
</style>

<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card class="main-card filter-card">
      <div class="filter-section">
        <el-form :inline="true" @submit.prevent class="filter-form">
          <el-form-item label="专业" required>
            <el-select v-model="selectedMajorId" placeholder="请选择专业" filterable clearable class="filter-select-large">
              <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="年级" required>
            <el-select v-model="selectedEnrollmentYear" placeholder="请选择年级" filterable clearable class="filter-select">
              <el-option v-for="y in yearOptions" :key="y" :label="y + ' 级'" :value="y" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!selectedMajorId || !selectedEnrollmentYear"
              @click="loadData"
            >
              <el-icon><Search /></el-icon>
              加载数据
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="main-card chart-card">
          <template #header>
            <div class="card-header">
              <div class="card-icon">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <span class="card-title">专业达成度雷达图</span>
            </div>
          </template>
          <div ref="chartRef" class="radar-chart"></div>
          <el-empty v-if="!radarData && !loading" description="点击加载数据查看雷达图" class="empty-state" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="main-card table-card">
          <template #header>
            <div class="card-header">
              <div class="card-icon" style="background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;">
                <el-icon><Aim /></el-icon>
              </div>
              <span class="card-title">各指标点达成度</span>
            </div>
          </template>
          <el-table :data="indicatorTable" stripe border size="small" class="indicator-table">
            <el-table-column prop="indicatorNo" label="指标点" width="100" align="center">
              <template #default="{ row }">
                <span class="indicator-no">{{ row.indicatorNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="描述" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="indicator-content">{{ row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column label="达成度" width="120" align="center">
              <template #default="{ row }">
                <span :class="['achievement-value', row.achievement >= 0.7 ? 'text-pass' : row.achievement >= 0.6 ? 'text-warning' : 'text-fail']">
                  {{ row.achievement.toFixed(4) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!indicatorTable.length && !loading" description="暂无数据" class="empty-state" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search, TrendCharts, Aim } from '@element-plus/icons-vue'
import { getMajorRadar, getGlobalResults, getEnrollmentYears } from '@/api/academic'
import { getGradReqs } from '@/api/director'
import { getMajors } from '@/api/admin'
import * as echarts from 'echarts'

const majorOptions = ref([])
const yearOptions = ref([])
const selectedMajorId = ref(null)
const selectedEnrollmentYear = ref(null)
const loading = ref(false)
const radarData = ref(null)
const indicatorTable = ref([])
const chartRef = ref(null)
let chartInstance = null
let resizeHandler = null

onMounted(async () => {
  const [yearRes, majRes] = await Promise.all([getEnrollmentYears(), getMajors()])
  yearOptions.value = yearRes?.data || yearRes || []
  majorOptions.value = majRes?.data?.records || majRes?.data || []
  resizeHandler = () => chartInstance?.resize()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  chartInstance?.dispose()
})

async function loadData() {
  if (!selectedMajorId.value || !selectedEnrollmentYear.value) return
  const enrollmentYear = selectedEnrollmentYear.value
  loading.value = true
  try {
    const [radarRes, resultsRes, gradReqsRes] = await Promise.all([
      getMajorRadar({ majorId: selectedMajorId.value, enrollmentYear }),
      getGlobalResults({ majorId: selectedMajorId.value, enrollmentYear }),
      getGradReqs(selectedMajorId.value)
    ])
    const radar = radarRes?.data || radarRes
    const results = resultsRes?.data || resultsRes
    const gradReqs = gradReqsRes?.data || gradReqsRes

    // Backend returns { "1.1": 0.852, "1.2": 0.781, ... }
    // Transform to ECharts radar format: { indicator: [...], value: [...] }
    const rawRadar = radar?.radarData || radar?.data || radar
    if (rawRadar && typeof rawRadar === 'object' && !Array.isArray(rawRadar)) {
      const keys = Object.keys(rawRadar)
      radarData.value = {
        indicator: keys.map(k => ({ name: k, max: 1 })),
        value: keys.map(k => Number(rawRadar[k]))
      }
    } else {
      radarData.value = rawRadar
    }

    // Build indicator name lookup
    const indMap = {}
    for (const req of (gradReqs || [])) {
      for (const ind of (req.indicators || [])) {
        indMap[ind.id] = { indicatorNo: ind.indicatorNo, content: ind.content }
      }
    }
    // Build table
    const table = []
    const achievements = results || {}
    for (const [indId, ach] of Object.entries(achievements)) {
      const info = indMap[indId] || { indicatorNo: `ID:${indId}`, content: '' }
      table.push({ indicatorId: Number(indId), indicatorNo: info.indicatorNo, content: info.content, achievement: Number(ach) })
    }
    table.sort((a, b) => a.indicatorNo.localeCompare(b.indicatorNo, undefined, { numeric: true }))
    indicatorTable.value = table
    // Render chart
    await nextTick()
    renderChart()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || !radarData.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  const data = radarData.value
  chartInstance.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator: data.indicator || [],
      shape: 'polygon',
      splitNumber: 5,
      axisName: { fontSize: 13, color: '#1E293B' },
      splitLine: { lineStyle: { color: '#E2E8F0' } },
      splitArea: { areaStyle: { color: ['#F8FAFC', '#FFFFFF'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: data.value || [],
        name: '专业达成度',
        areaStyle: { color: 'rgba(37, 99, 235, 0.15)' },
        lineStyle: { color: '#2563EB', width: 2 },
        itemStyle: { color: '#2563EB' }
      }]
    }]
  })
}
</script>

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

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-select {
  width: 160px;
}

.filter-select-large {
  width: 260px;
}

/* ===== Card Styles ===== */
.main-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.main-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #F1F5F9;
}

.main-card :deep(.el-card__body) {
  padding: 20px;
}

/* ===== Card Header ===== */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563EB;
  font-size: 18px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
}

/* ===== Radar Chart ===== */
.radar-chart {
  width: 100%;
  height: 400px;
}

/* ===== Indicator Table ===== */
.indicator-table {
  border-radius: 8px;
  overflow: hidden;
}

.indicator-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.indicator-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #059669;
}

.indicator-content {
  color: #1E293B;
}

.achievement-value {
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.text-pass { color: #059669; }
.text-warning { color: #D97706; }
.text-fail { color: #DC2626; }

/* ===== Empty State ===== */
.empty-state {
  padding: 40px 0;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-select,
  .filter-select-large {
    width: 100%;
  }

  :deep(.el-col-12) {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .indicator-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

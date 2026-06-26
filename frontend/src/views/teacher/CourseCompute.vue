<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #0891B2 0%, #06B6D4 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">课程级达成度计算</h3>
              <p class="header-subtitle">计算课程目标达成度，生成分析报告</p>
            </div>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" class="class-selector">
              <el-option v-for="c in myClasses" :key="c.id" :label="`${c.courseName} - ${c.className}`" :value="c.id" />
            </el-select>
            <el-tag
              v-if="scoreStatus"
              :type="scoreStatus === 'LOCKED' ? 'success' : scoreStatus === 'IMPORTED' ? 'warning' : 'info'"
              size="default"
              class="status-tag"
              effect="dark"
            >
              <el-icon class="status-icon"><component :is="statusIcon" /></el-icon>
              {{ { EMPTY: '未录入', IMPORTED: '已导入', LOCKED: '已锁定' }[scoreStatus] || scoreStatus }}
            </el-tag>
          </div>
        </div>
      </template>

      <div class="action-bar">
        <el-button
          type="primary"
          :icon="DataAnalysis"
          :disabled="!selectedClassId || scoreStatus !== 'IMPORTED'"
          :loading="computing"
          @click="handleCompute"
          class="compute-button"
        >
          <el-icon><Cpu /></el-icon>
          一键计算
        </el-button>
        <el-button :disabled="!selectedClassId" :loading="loadingResults" @click="loadResults">
          <el-icon><View /></el-icon>
          查看结果
        </el-button>
        <el-button :disabled="!selectedClassId" :loading="downloadingPdf" @click="handleDownloadPdf">
          <el-icon><Document /></el-icon>
          导出 PDF
        </el-button>
        <el-button :disabled="!selectedClassId" :loading="downloadingExcel" @click="handleDownloadExcel">
          <el-icon><Document /></el-icon>
          导出 Excel
        </el-button>
      </div>

      <el-alert
        v-if="scoreStatus !== 'IMPORTED' && scoreStatus !== 'LOCKED'"
        title="请先完成成绩录入后再进行计算。"
        type="info"
        show-icon
        :closable="false"
        class="info-alert"
      />

      <div v-if="calcResult" class="result-section">
        <!-- 概览统计卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%); color: #2563EB;">
              <el-icon><Flag /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ objectiveTable.length }}</div>
              <div class="stat-label">课程目标数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;">
              <el-icon><Aim /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ indicatorTable.length }}</div>
              <div class="stat-label">支撑指标点数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" :style="overallPassRate >= 60 ? 'background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;' : 'background: linear-gradient(135deg, #FEF2F2 0%, #FEE2E2 100%); color: #DC2626;'">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number" :style="{ color: overallPassRate >= 60 ? '#059669' : '#DC2626' }">{{ overallPassRate.toFixed(1) }}%</div>
              <div class="stat-label">整体达标率</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" :style="passCount === indicatorTable.length ? 'background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;' : 'background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%); color: #D97706;'">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number" :style="{ color: passCount === indicatorTable.length ? '#059669' : '#D97706' }">{{ passCount }}/{{ indicatorTable.length }}</div>
              <div class="stat-label">指标点达标</div>
            </div>
          </div>
        </div>

        <!-- 图表 + 目标达成度表 -->
        <div class="result-grid">
          <el-card shadow="never" class="result-card chart-card">
            <template #header>
              <div class="card-header">
                <div class="card-icon">
                  <el-icon><DataAnalysis /></el-icon>
                </div>
                <span class="card-title">课程目标达成度柱状图</span>
              </div>
            </template>
            <div ref="chartRef" style="width:100%;height:340px"></div>
          </el-card>

          <el-card shadow="never" class="result-card achievement-card">
            <template #header>
              <div class="card-header">
                <div class="card-icon" style="background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;">
                  <el-icon><TrendCharts /></el-icon>
                </div>
                <span class="card-title">课程目标达成度</span>
              </div>
            </template>
            <div class="achievement-list">
              <div v-for="row in objectiveTable" :key="row.id" class="achievement-row">
                <div class="achievement-label">
                  <span class="achievement-no">{{ row.label }}</span>
                </div>
                <el-progress
                  :percentage="Math.round(row.value * 100)"
                  :stroke-width="18"
                  :color="row.value >= 0.6 ? '#059669' : row.value >= 0.5 ? '#D97706' : '#DC2626'"
                  class="achievement-progress"
                />
                <span class="achievement-value" :class="row.value >= 0.6 ? 'text-pass' : 'text-fail'">
                  {{ row.value?.toFixed(4) }}
                </span>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 指标点达成度 -->
        <el-card shadow="never" class="result-card indicator-card">
          <template #header>
            <div class="card-header">
              <div class="card-icon" style="background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%); color: #7C3AED;">
                <el-icon><Aim /></el-icon>
              </div>
              <span class="card-title">毕业要求指标点达成度</span>
            </div>
          </template>
          <el-table :data="indicatorTable" stripe size="small" class="indicator-table">
            <el-table-column prop="label" label="指标点" width="120" align="center">
              <template #default="{ row }">
                <span class="indicator-label">{{ row.label }}</span>
              </template>
            </el-table-column>
            <el-table-column label="达成度" width="180" align="center">
              <template #default="{ row }">
                <el-progress
                  :percentage="Math.round(row.value * 100)"
                  :stroke-width="16"
                  :color="row.value >= 0.6 ? '#059669' : row.value >= 0.5 ? '#D97706' : '#DC2626'"
                  class="indicator-progress"
                />
              </template>
            </el-table-column>
            <el-table-column label="数值" width="100" align="center">
              <template #default="{ row }">
                <span :class="row.value >= 0.6 ? 'text-pass' : 'text-fail'" class="value-text">
                  {{ row.value?.toFixed(4) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.value >= 0.6 ? 'success' : 'danger'"
                  size="small"
                  effect="dark"
                  class="status-badge"
                >
                  <el-icon class="status-icon"><component :is="row.value >= 0.6 ? 'CircleCheck' : 'CircleClose'" /></el-icon>
                  {{ row.value >= 0.6 ? '达标' : '未达标' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <p v-if="calcResult.calcTime" class="calc-time">
          <el-icon><Clock /></el-icon>
          计算时间：{{ calcResult.calcTime }}
        </p>
      </div>

      <div v-else class="empty-state">
        <el-empty description="请先选择教学班级，完成成绩录入后点击一键计算" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, Download, Cpu, View, Document, Flag, Aim, TrendCharts, CircleCheck, CircleClose, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyClasses, getScoreStatus, triggerCourseCompute, getComputeResults, downloadCoursePdf, downloadCourseExcel } from '@/api/teacher'
import * as echarts from 'echarts'

const myClasses = ref([])
const selectedClassId = ref(null)
const scoreStatus = ref('')
const computing = ref(false)
const loadingResults = ref(false)
const downloadingPdf = ref(false)
const downloadingExcel = ref(false)
const calcResult = ref(null)
const chartRef = ref(null)
let chartInstance = null
let resizeHandler = null

const statusIcon = computed(() => {
  const map = { EMPTY: Clock, IMPORTED: Warning, LOCKED: CircleCheck }
  return map[scoreStatus.value] || Clock
})

const objectiveTable = computed(() => {
  if (!calcResult.value) return []
  const { objectiveAchievements = {}, objectiveLabels = {} } = calcResult.value
  return Object.entries(objectiveAchievements).map(([id, val]) => ({
    id: Number(id), label: objectiveLabels[id] || `目标${id}`, value: Number(val)
  })).sort((a, b) => a.label.localeCompare(b.label, undefined, { numeric: true }))
})

const indicatorTable = computed(() => {
  if (!calcResult.value) return []
  const { courseAchievements = {}, indicatorLabels = {} } = calcResult.value
  return Object.entries(courseAchievements).map(([id, val]) => ({
    id: Number(id), label: indicatorLabels[id] || `指标${id}`, value: Number(val)
  })).sort((a, b) => a.label.localeCompare(b.label, undefined, { numeric: true }))
})

const passCount = computed(() => indicatorTable.value.filter(r => r.value >= 0.6).length)
const overallPassRate = computed(() => {
  if (!indicatorTable.value.length) return 0
  return (passCount.value / indicatorTable.value.length) * 100
})

onMounted(async () => {
  const route = useRoute()
  const res = await getMyClasses()
  myClasses.value = res.data || res || []
  if (myClasses.value.length) {
    // Auto-select by courseId from route, otherwise first class
    const cid = parseInt(route.params.courseId)
    const match = cid ? myClasses.value.find(c => c.id === cid || c.courseId === cid) : null
    selectedClassId.value = match ? match.id : myClasses.value[0].id
  }
  resizeHandler = () => chartInstance?.resize()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  chartInstance?.dispose()
})

watch(selectedClassId, async (v) => {
  if (v) {
    try { const s = await getScoreStatus(v); scoreStatus.value = s?.data?.status || s?.status || 'EMPTY' } catch { scoreStatus.value = '' }
    calcResult.value = null
  }
})

async function handleCompute() {
  try { await ElMessageBox.confirm('确认触发课程级计算？计算完成后成绩将被锁定。', '确认', { type: 'warning' }) } catch { return }
  computing.value = true
  try {
    const userStore = useUserStore()
    const res = await triggerCourseCompute(selectedClassId.value, userStore.userId)
    calcResult.value = res?.data || res
    scoreStatus.value = 'LOCKED'
    ElMessage.success('计算完成，成绩已锁定')
    await nextTick()
    renderChart()
  } finally { computing.value = false }
}

async function loadResults() {
  loadingResults.value = true
  try {
    const res = await getComputeResults(selectedClassId.value)
    calcResult.value = res?.data || res
    await nextTick()
    renderChart()
  } catch { calcResult.value = null } finally { loadingResults.value = false }
}

function renderChart() {
  if (!chartRef.value || !objectiveTable.value.length) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  const data = objectiveTable.value
  chartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => { const item = p[0]; return `<b>${item.name}</b><br/>达成度：${item.value.toFixed(4)}` } },
    grid: { top: 20, right: 30, bottom: 30, left: 50 },
    xAxis: { type: 'category', data: data.map(d => d.label), axisLabel: { fontSize: 12 }, axisTick: { alignWithLabel: true } },
    yAxis: { type: 'value', max: 1, name: '达成度', nameTextStyle: { fontSize: 12, color: '#64748B' }, axisLabel: { formatter: v => v.toFixed(2) }, splitLine: { lineStyle: { type: 'dashed', color: '#E2E8F0' } } },
    series: [{
      type: 'bar', data: data.map(d => ({ value: d.value, itemStyle: { color: d.value >= 0.6 ? '#059669' : d.value >= 0.5 ? '#D97706' : '#DC2626', borderRadius: [6, 6, 0, 0] } })),
      barWidth: '40%', label: { show: true, position: 'top', fontSize: 12, fontWeight: 600, formatter: p => p.value.toFixed(4) },
      markLine: { silent: true, symbol: 'none', lineStyle: { color: '#DC2626', type: 'dashed', width: 1.5 }, label: { fontSize: 11, formatter: '60%' }, data: [{ yAxis: 0.6 }] }
    }]
  })
}

function downloadBlob(blob, filename) {
  if (!blob || blob.size < 200) { ElMessage.warning('文件内容为空，请确认已完成计算'); return }
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = filename
  document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
}

async function handleDownloadPdf() {
  downloadingPdf.value = true
  const name = myClasses.value.find(c => c.id === selectedClassId.value)?.className || '课程达成度报告'
  try { const res = await downloadCoursePdf(selectedClassId.value); downloadBlob(res, name + '.pdf') } catch {} finally { downloadingPdf.value = false }
}

async function handleDownloadExcel() {
  downloadingExcel.value = true
  const name = myClasses.value.find(c => c.id === selectedClassId.value)?.className || '课程达成度报告'
  try { const res = await downloadCourseExcel(selectedClassId.value); downloadBlob(res, name + '.xlsx') } catch {} finally { downloadingExcel.value = false }
}
</script>

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

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
}

.status-icon {
  font-size: 16px;
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

/* ===== Action Bar ===== */
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.compute-button {
  background: linear-gradient(135deg, #0891B2 0%, #06B6D4 100%);
  border: none;
}

.compute-button:hover {
  background: linear-gradient(135deg, #0E7490 0%, #0891B2 100%);
}

/* ===== Info Alert ===== */
.info-alert {
  margin-bottom: 24px;
  border-radius: 8px;
}

/* ===== Stats Grid ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: #CBD5E1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1E293B;
  line-height: 1.2;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.stat-label {
  font-size: 13px;
  color: #64748B;
}

/* ===== Result Grid ===== */
.result-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.result-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
}

.result-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #F1F5F9;
}

.result-card :deep(.el-card__body) {
  padding: 20px;
}

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

/* ===== Achievement List ===== */
.achievement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.achievement-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #F1F5F9;
}

.achievement-row:last-child {
  border-bottom: none;
}

.achievement-label {
  min-width: 60px;
}

.achievement-no {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.achievement-progress {
  flex: 1;
  margin: 0 16px;
}

.achievement-value {
  min-width: 80px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'Consolas', monospace;
}

/* ===== Indicator Card ===== */
.indicator-card {
  margin-bottom: 20px;
}

.indicator-table {
  border-radius: 8px;
  overflow: hidden;
}

.indicator-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.indicator-label {
  font-weight: 600;
  color: #1E293B;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.indicator-progress {
  width: 140px;
  display: inline-block;
  vertical-align: middle;
}

.value-text {
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.status-badge {
  border-radius: 6px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== Text Colors ===== */
.text-pass { color: #059669; }
.text-fail { color: #DC2626; }

/* ===== Calc Time ===== */
.calc-time {
  margin-top: 20px;
  text-align: right;
  font-size: 13px;
  color: #64748B;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 60px 0;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .result-grid {
    grid-template-columns: 1fr;
  }
}

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

  .status-tag {
    justify-content: center;
  }

  .action-bar {
    flex-wrap: wrap;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-number {
    font-size: 24px;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .stat-card,
  .compute-button {
    transition: none;
  }
}
</style>

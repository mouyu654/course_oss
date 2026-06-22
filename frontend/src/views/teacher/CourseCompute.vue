<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px">
          <h3 style="margin:0;font-size:16px">课程级达成度计算</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" style="width:280px">
              <el-option v-for="c in myClasses" :key="c.id" :label="`${c.courseName} - ${c.className}`" :value="c.id" />
            </el-select>
            <el-tag v-if="scoreStatus" :type="scoreStatus === 'LOCKED' ? 'success' : scoreStatus === 'IMPORTED' ? 'warning' : 'info'" size="default">
              {{ { EMPTY: '未录入', IMPORTED: '已导入', LOCKED: '已锁定' }[scoreStatus] || scoreStatus }}
            </el-tag>
          </div>
        </div>
      </template>

      <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
        <el-button type="primary" :icon="DataAnalysis" :disabled="!selectedClassId || scoreStatus !== 'IMPORTED'" :loading="computing" @click="handleCompute">一键计算</el-button>
        <el-button :disabled="!selectedClassId" :loading="loadingResults" @click="loadResults">查看结果</el-button>
        <el-button :icon="Download" :disabled="!selectedClassId" :loading="downloadingPdf" @click="handleDownloadPdf">导出 PDF</el-button>
        <el-button :icon="Download" :disabled="!selectedClassId" :loading="downloadingExcel" @click="handleDownloadExcel">导出 Excel</el-button>
      </div>

      <el-alert v-if="scoreStatus !== 'IMPORTED' && scoreStatus !== 'LOCKED'" title="请先完成成绩录入后再进行计算。" type="info" show-icon :closable="false" style="margin-bottom:16px" />

      <div v-if="calcResult">
        <!-- 概览统计卡片 -->
        <el-row :gutter="16" style="margin-bottom:20px">
          <el-col :span="6">
            <div class="stat-card"><div class="stat-number">{{ objectiveTable.length }}</div><div class="stat-label">课程目标数</div></div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card"><div class="stat-number">{{ indicatorTable.length }}</div><div class="stat-label">支撑指标点数</div></div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card"><div class="stat-number" :style="{ color: overallPassRate >= 60 ? '#67c23a' : '#f56c6c' }">{{ overallPassRate.toFixed(1) }}%</div><div class="stat-label">整体达标率</div></div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card"><div class="stat-number" :style="{ color: passCount === indicatorTable.length ? '#67c23a' : '#e6a23c' }">{{ passCount }}/{{ indicatorTable.length }}</div><div class="stat-label">指标点达标</div></div>
          </el-col>
        </el-row>

        <!-- 图表 + 目标达成度表 -->
        <el-row :gutter="20">
          <el-col :span="14">
            <el-card shadow="never" class="result-card">
              <template #header><span class="card-title">课程目标达成度柱状图</span></template>
              <div ref="chartRef" style="width:100%;height:340px"></div>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never" class="result-card">
              <template #header><span class="card-title">课程目标达成度</span></template>
              <div v-for="row in objectiveTable" :key="row.id" class="achievement-row">
                <div class="achievement-label">{{ row.label }}</div>
                <el-progress :percentage="Math.round(row.value * 100)" :stroke-width="18" :color="row.value >= 0.6 ? '#67c23a' : row.value >= 0.5 ? '#e6a23c' : '#f56c6c'" style="flex:1;margin:0 12px" />
                <span class="achievement-value" :class="row.value >= 0.6 ? 'text-pass' : 'text-fail'">{{ row.value?.toFixed(4) }}</span>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 指标点达成度 -->
        <el-card shadow="never" class="result-card" style="margin-top:20px">
          <template #header><span class="card-title">毕业要求指标点达成度</span></template>
          <el-table :data="indicatorTable" stripe size="small" style="width:100%">
            <el-table-column prop="label" label="指标点" width="120" align="center" />
            <el-table-column label="达成度" width="160" align="center">
              <template #default="{ row }">
                <el-progress :percentage="Math.round(row.value * 100)" :stroke-width="16" :color="row.value >= 0.6 ? '#67c23a' : row.value >= 0.5 ? '#e6a23c' : '#f56c6c'" style="width:120px;display:inline-block;vertical-align:middle" />
              </template>
            </el-table-column>
            <el-table-column label="数值" width="100" align="center">
              <template #default="{ row }">
                <span :class="row.value >= 0.6 ? 'text-pass' : 'text-fail'" style="font-weight:600">{{ row.value?.toFixed(4) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.value >= 0.6 ? 'success' : 'danger'" size="small" effect="dark">{{ row.value >= 0.6 ? '达标' : '未达标' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <p v-if="calcResult.calcTime" style="margin-top:20px;text-align:right;font-size:13px;color:#909399">计算时间：{{ calcResult.calcTime }}</p>
      </div>

      <el-empty v-if="!calcResult && !loadingResults" description="请先选择教学班级，完成成绩录入后点击一键计算" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { DataAnalysis, Download } from '@element-plus/icons-vue'
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
  const res = await getMyClasses()
  myClasses.value = res.data || res || []
  if (myClasses.value.length) selectedClassId.value = myClasses.value[0].id
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
    yAxis: { type: 'value', max: 1, name: '达成度', nameTextStyle: { fontSize: 12, color: '#909399' }, axisLabel: { formatter: v => v.toFixed(2) }, splitLine: { lineStyle: { type: 'dashed', color: '#E4E7ED' } } },
    series: [{
      type: 'bar', data: data.map(d => ({ value: d.value, itemStyle: { color: d.value >= 0.6 ? '#67c23a' : d.value >= 0.5 ? '#e6a23c' : '#f56c6c', borderRadius: [4, 4, 0, 0] } })),
      barWidth: '40%', label: { show: true, position: 'top', fontSize: 12, fontWeight: 600, formatter: p => p.value.toFixed(4) },
      markLine: { silent: true, symbol: 'none', lineStyle: { color: '#f56c6c', type: 'dashed', width: 1.5 }, label: { fontSize: 11, formatter: '60%' }, data: [{ yAxis: 0.6 }] }
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
.stat-card { text-align: center; padding: 20px 12px; background: #fff; border: 1px solid #e4e7ed; border-radius: 6px; }
.stat-number { font-size: 32px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
.result-card { border: 1px solid #e4e7ed; }
.card-title { font-size: 14px; font-weight: 600; color: #303133; }
.achievement-row { display: flex; align-items: center; padding: 10px 0; border-bottom: 1px solid #f2f3f5; }
.achievement-row:last-child { border-bottom: none; }
.achievement-label { min-width: 50px; font-size: 13px; font-weight: 600; color: #606266; }
.achievement-value { min-width: 70px; text-align: right; font-size: 14px; font-weight: 600; font-variant-numeric: tabular-nums; }
.text-pass { color: #67c23a; }
.text-fail { color: #f56c6c; }
</style>

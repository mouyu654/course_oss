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
        <!-- Level 1: Objective Achievements -->
        <h4 style="margin:16px 0 8px;font-size:14px;color:#303133">课程目标达成度</h4>
        <el-table :data="objectiveTable" stripe border size="small" style="width:100%;margin-bottom:16px">
          <el-table-column prop="label" label="目标编号" width="120" />
          <el-table-column label="达成度" width="120" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.value >= 0.6 ? '#67c23a' : '#f56c6c', fontWeight: 600 }">
                {{ row.value?.toFixed(4) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <div ref="chartRef" style="width:100%;height:300px;margin-bottom:16px"></div>

        <!-- Level 2: Indicator Achievements -->
        <h4 style="margin:16px 0 8px;font-size:14px;color:#303133">毕业要求指标点达成度</h4>
        <el-table :data="indicatorTable" stripe border size="small" style="width:100%">
          <el-table-column prop="label" label="指标点" width="120" />
          <el-table-column label="达成度" width="120" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.value >= 0.6 ? '#67c23a' : '#f56c6c', fontWeight: 600 }">
                {{ row.value?.toFixed(4) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <p v-if="calcResult.calcTime" style="margin-top:12px;font-size:12px;color:#909399">计算时间：{{ calcResult.calcTime }}</p>
      </div>

      <el-empty v-if="!calcResult && !loadingResults" description="请先选择教学班级，完成成绩录入后点击一键计算" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { DataAnalysis, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

onMounted(async () => {
  const res = await getMyClasses()
  myClasses.value = res || []
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
    try { const s = await getScoreStatus(v); scoreStatus.value = s?.status || 'EMPTY' } catch { scoreStatus.value = '' }
    calcResult.value = null
  }
})

async function handleCompute() {
  try { await ElMessageBox.confirm('确认触发课程级计算？计算完成后成绩将被锁定。', '确认', { type: 'warning' }) } catch { return }
  computing.value = true
  try {
    calcResult.value = await triggerCourseCompute(selectedClassId.value)
    scoreStatus.value = 'LOCKED'
    ElMessage.success('计算完成，成绩已锁定')
    await nextTick()
    renderChart()
  } finally { computing.value = false }
}

async function loadResults() {
  loadingResults.value = true
  try {
    calcResult.value = await getComputeResults(selectedClassId.value)
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
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>达成度: ${p[0].value.toFixed(4)}` },
    xAxis: { type: 'category', data: data.map(d => d.label) },
    yAxis: { type: 'value', max: 1, name: '达成度' },
    series: [{
      type: 'bar', data: data.map(d => d.value),
      itemStyle: { color: p => p.value >= 0.6 ? '#67c23a' : '#f56c6c' },
      label: { show: true, position: 'top', formatter: p => p.value.toFixed(4) }
    }]
  })
}

async function handleDownloadPdf() {
  downloadingPdf.value = true
  try {
    const blob = await downloadCoursePdf(selectedClassId.value)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a'); a.href = url; a.download = '课程达成度报告.pdf'
    document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
  } catch { /* handled */ } finally { downloadingPdf.value = false }
}

async function handleDownloadExcel() {
  downloadingExcel.value = true
  try {
    const blob = await downloadCourseExcel(selectedClassId.value)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a'); a.href = url; a.download = '课程达成度报告.xlsx'
    document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
  } catch { /* handled */ } finally { downloadingExcel.value = false }
}
</script>

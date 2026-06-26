<template>
  <div class="page-container">
    <!-- 查询栏 -->
    <el-card class="main-card filter-card">
      <div class="filter-section">
        <el-form :inline="true" @submit.prevent="loadBatches" class="filter-form">
          <el-form-item label="年级">
            <el-select v-model="filters.enrollmentYear" placeholder="全部" clearable class="filter-select">
              <el-option v-for="y in yearOptions" :key="y" :label="y + ' 级'" :value="y" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业">
            <el-select v-model="filters.majorId" placeholder="全部" clearable filterable class="filter-select-large">
              <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadBatches">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 计算结果列表 -->
    <el-card class="main-card result-card">
      <template #header>
        <div class="result-header">
          <div class="result-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <span class="result-title">计算结果列表</span>
          <el-tag v-if="batches.length" type="info" effect="plain" class="count-tag">{{ batches.length }}</el-tag>
        </div>
      </template>
      <el-table ref="tableRef" :data="pagedBatches" v-loading="loading" stripe class="result-table" highlight-current-row @current-change="handleRowClick">
        <el-table-column prop="id" label="ID" width="60" align="center">
          <template #default="{ row }">
            <span class="batch-id">#{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="enrollmentYear" label="年级" width="100" align="center">
          <template #default="{ row }">
            <span class="year-text">{{ row.enrollmentYear ? row.enrollmentYear + ' 级' : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="majorName" label="专业" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="major-name">{{ row.majorName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="calcTime" label="计算时间" min-width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.calcTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button link type="success" @click.stop="handleDownload(row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper" v-if="batches.length > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="batches.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
      <el-empty v-if="!loading && batches.length === 0 && queried" description="暂无计算结果" class="empty-state" />
    </el-card>

    <!-- 雷达图详情 -->
    <el-card v-if="radarData && selectedBatch" class="main-card radar-card">
      <template #header>
        <div class="radar-header">
          <div class="radar-title-group">
            <div class="radar-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <span class="radar-title">
              {{ selectedBatch.enrollmentYear ? selectedBatch.enrollmentYear + ' 级 — ' : '' }}{{ selectedBatch.majorName || '' }} 专业评价雷达图
            </span>
          </div>
          <div class="radar-actions">
            <el-button type="primary" @click="exportRadarImage">
              <el-icon><Download /></el-icon>
              导出图片
            </el-button>
            <el-button @click="closeRadar">
              <el-icon><Close /></el-icon>
              关闭
            </el-button>
          </div>
        </div>
      </template>
      <div class="radar-content">
        <div ref="radarChartRef" class="radar-chart"></div>
        <div class="radar-details">
          <h4 class="radar-details-title">各指标点达成度</h4>
          <div class="indicator-list">
            <div v-for="(value, key) in radarData" :key="key" class="indicator-row">
              <span class="indicator-label">{{ key }}</span>
              <el-progress :percentage="Math.round(value * 100)" :stroke-width="16" class="indicator-progress" :color="value >= 0.7 ? '#059669' : '#D97706'" />
              <span class="indicator-value">{{ value.toFixed(4) }}</span>
            </div>
          </div>
          <p v-if="selectedBatch.calcTime" class="calc-time">
            <el-icon><Clock /></el-icon>
            计算时间：{{ selectedBatch.calcTime }}
          </p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search, RefreshRight, View, Download, Delete, TrendCharts, Close, Clock, DataAnalysis } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getEnrollmentYears, getCalcBatches, getCalcRadarData, deleteCalcBatch, downloadMajorExcel } from '@/api/academic'
import { getMajors } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const yearOptions = ref([])
const majorOptions = ref([])
const filters = ref({ enrollmentYear: null, majorId: null })
const batches = ref([])
const selectedBatch = ref(null)
const radarData = ref(null)
const loading = ref(false)
const queried = ref(false)
const page = ref(1)
const pageSize = ref(20)

const pagedBatches = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return batches.value.slice(start, start + pageSize.value)
})

function handlePageChange(p) { page.value = p }
function handleSizeChange(s) { pageSize.value = s; page.value = 1 }

const radarChartRef = ref(null)
const tableRef = ref(null)
let radarChart = null
let resizeHandler = null

function handleReset() {
  filters.value = { enrollmentYear: null, majorId: null }
  batches.value = []
  selectedBatch.value = null
  radarData.value = null
}

function closeRadar() {
  selectedBatch.value = null
  radarData.value = null
  if (tableRef.value) {
    tableRef.value.setCurrentRow()
  }
}

function exportRadarImage() {
  if (!radarChart || !radarData.value) return
  const b = selectedBatch.value
  const keys = Object.keys(radarData.value)
  const values = Object.values(radarData.value).map(v => Number(v))

  const chartUrl = radarChart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const chartImg = new Image()
  chartImg.src = chartUrl
  chartImg.onload = () => {
    const scale = 2
    const chartW = 520, chartH = 420
    const tableX = chartW + 40, rowH = 36, headerH = 44
    const tableW = 320
    const totalW = tableX + tableW + 40
    const totalH = Math.max(chartH, headerH + keys.length * rowH + 60)

    const canvas = document.createElement('canvas')
    canvas.width = totalW * scale
    canvas.height = totalH * scale
    const ctx = canvas.getContext('2d')
    ctx.scale(scale, scale)
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, totalW, totalH)

    const title = [b.enrollmentYear ? b.enrollmentYear + '级' : '', b.majorName || '', '专业评价雷达图'].filter(Boolean).join(' — ')
    ctx.fillStyle = '#1E293B'
    ctx.font = 'bold 18px sans-serif'
    ctx.fillText(title, 20, 30)

    ctx.drawImage(chartImg, 0, 44, chartW, chartH)

    let y = 44 + 10
    ctx.fillStyle = '#F8FAFC'
    ctx.fillRect(tableX, y, tableW, headerH)
    ctx.strokeStyle = '#E2E8F0'
    ctx.strokeRect(tableX, y, tableW, headerH)
    ctx.fillStyle = '#1E293B'
    ctx.font = 'bold 14px sans-serif'
    ctx.fillText('指标点', tableX + 16, y + 28)
    ctx.fillText('达成度', tableX + 160, y + 28)
    y += headerH

    ctx.font = '13px sans-serif'
    keys.forEach((key, i) => {
      const val = values[i].toFixed(4)
      ctx.fillStyle = i % 2 === 0 ? '#FFFFFF' : '#F8FAFC'
      ctx.fillRect(tableX, y, tableW, rowH)
      ctx.strokeStyle = '#E2E8F0'
      ctx.strokeRect(tableX, y, tableW, rowH)
      ctx.fillStyle = '#1E293B'
      ctx.fillText(key, tableX + 16, y + 24)
      const barX = tableX + 100, barW = 120, barH = 12, barY = y + 12
      ctx.fillStyle = '#E2E8F0'
      ctx.fillRect(barX, barY, barW, barH)
      ctx.fillStyle = values[i] >= 0.7 ? '#059669' : '#D97706'
      ctx.fillRect(barX, barY, barW * values[i], barH)
      ctx.fillStyle = '#1E293B'
      ctx.textAlign = 'right'
      ctx.fillText(val, tableX + tableW - 16, y + 24)
      ctx.textAlign = 'left'
      y += rowH
    })

    if (b.calcTime) {
      ctx.fillStyle = '#94A3B8'
      ctx.font = '12px sans-serif'
      ctx.fillText('计算时间：' + b.calcTime, tableX + 16, y + 20)
    }

    const link = document.createElement('a')
    link.href = canvas.toDataURL('image/png')
    const nameParts = [b.enrollmentYear ? b.enrollmentYear + '级' : '', b.majorName || '', '专业评价雷达图']
    link.download = nameParts.filter(Boolean).join('_') + '.png'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('导出成功')
  }
}

async function loadOptions() {
  try {
    const [yearRes, majRes] = await Promise.all([getEnrollmentYears(), getMajors()])
    yearOptions.value = yearRes?.data || yearRes || []
    majorOptions.value = majRes?.data?.records || majRes?.data || []
  } catch { /* ignore */ }
}

async function loadBatches() {
  loading.value = true
  queried.value = true
  selectedBatch.value = null
  radarData.value = null
  try {
    const params = {}
    if (filters.value.enrollmentYear) params.enrollmentYear = filters.value.enrollmentYear
    if (filters.value.majorId) params.majorId = filters.value.majorId
    const res = await getCalcBatches(params)
    batches.value = res?.data || res || []
  } catch {
    batches.value = []
  } finally {
    loading.value = false
  }
}

async function handleView(row) {
  selectedBatch.value = row
  try {
    const params = { majorId: row.majorId, enrollmentYear: row.enrollmentYear }
    const res = await getCalcRadarData(params)
    const data = res?.data || res
    radarData.value = data?.radarData || data || null
    await nextTick()
    renderRadar()
  } catch {
    radarData.value = null
  }
}

function handleRowClick(row) {
  if (!row) return
  handleView(row)
}

function renderRadar() {
  if (!radarChartRef.value || !radarData.value) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)

  const keys = Object.keys(radarData.value)
  const values = Object.values(radarData.value).map(v => Number(v))

  radarChart.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator: keys.map(k => ({ name: k, max: 1.0 })),
      radius: '65%',
      splitNumber: 5,
      axisName: { fontSize: 13, color: '#1E293B' },
      splitLine: { lineStyle: { color: '#E2E8F0' } },
      splitArea: { areaStyle: { color: ['#F8FAFC', '#FFFFFF'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '达成度',
        areaStyle: { color: 'rgba(37, 99, 235, 0.15)' },
        lineStyle: { color: '#2563EB', width: 2 },
        itemStyle: { color: '#2563EB' }
      }]
    }]
  })
}

async function handleDownload(row) {
  try {
    const params = { majorId: row.majorId, enrollmentYear: row.enrollmentYear }
    const blob = await downloadMajorExcel(params)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    const nameParts = [row.enrollmentYear ? row.enrollmentYear + '级' : '', row.majorName || '', '达成度台账']
    a.download = nameParts.filter(Boolean).join('_') + '.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch { /* interceptor handles error */ }
}

async function handleDelete(row) {
  const label = [(row.enrollmentYear ? row.enrollmentYear + '级' : ''), row.majorName || ''].filter(Boolean).join(' ') || '该批次'
  await ElMessageBox.confirm(`确认删除「${label}」的计算结果？此操作不可恢复。`, '删除确认', { type: 'error' })
  await deleteCalcBatch({ majorId: row.majorId, enrollmentYear: row.enrollmentYear })
  ElMessage.success('删除成功')
  if (selectedBatch.value?.majorId === row.majorId && selectedBatch.value?.enrollmentYear === row.enrollmentYear) {
    selectedBatch.value = null
    radarData.value = null
  }
  loadBatches()
}

onMounted(() => {
  loadOptions()
  loadBatches()
  resizeHandler = () => radarChart?.resize()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  radarChart?.dispose()
})
</script>

<style scoped>
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

/* ===== Filter Section ===== */
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
  width: 140px;
}

.filter-select-large {
  width: 200px;
}

/* ===== Result Card ===== */
.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-icon {
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

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.count-tag {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.result-table {
  border-radius: 8px;
  overflow: hidden;
}

.result-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}

.batch-id {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #64748B;
}

.year-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #475569;
}

.major-name {
  font-weight: 500;
  color: #1E293B;
}

.time-text {
  font-size: 13px;
  color: #64748B;
}

/* ===== Radar Card ===== */
.radar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.radar-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.radar-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #059669;
  font-size: 18px;
}

.radar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.radar-actions {
  display: flex;
  gap: 8px;
}

.radar-content {
  display: flex;
  align-items: flex-start;
  gap: 40px;
  flex-wrap: wrap;
}

.radar-chart {
  width: 480px;
  height: 380px;
}

.radar-details {
  flex: 1;
  min-width: 280px;
}

.radar-details-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.indicator-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.indicator-label {
  min-width: 50px;
  font-weight: 600;
  font-size: 13px;
  color: #475569;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.indicator-progress {
  flex: 1;
}

.indicator-value {
  min-width: 50px;
  text-align: right;
  font-size: 13px;
  color: #1E293B;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.calc-time {
  margin: 20px 0 0;
  font-size: 12px;
  color: #94A3B8;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== Pagination ===== */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

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

  .radar-content {
    flex-direction: column;
    align-items: center;
  }

  .radar-details {
    width: 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .result-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

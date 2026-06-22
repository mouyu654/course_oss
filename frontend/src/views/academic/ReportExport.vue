<template>
  <div class="page-container">
    <!-- 查询栏 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :inline="true" @submit.prevent="loadBatches">
        <el-form-item label="年级">
          <el-select v-model="filters.enrollmentYear" placeholder="全部" clearable style="width:140px">
            <el-option v-for="y in yearOptions" :key="y" :label="y + ' 级'" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="filters.majorId" placeholder="全部" clearable filterable style="width:200px">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBatches">查询</el-button>
          <el-button @click="filters = { enrollmentYear: null, majorId: null }; batches = []; selectedBatch = null; radarData = null">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 计算结果列表 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <template #header><span style="font-weight:600">计算结果列表</span></template>
      <el-table ref="tableRef" :data="pagedBatches" v-loading="loading" stripe style="width:100%" highlight-current-row @current-change="handleRowClick">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="enrollmentYear" label="年级" width="100" align="center">
          <template #default="{ row }">{{ row.enrollmentYear ? row.enrollmentYear + ' 级' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="majorName" label="专业" min-width="200" show-overflow-tooltip />
        <el-table-column prop="calcTime" label="计算时间" min-width="170" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleView(row)">查看</el-button>
            <el-button link type="success" @click.stop="handleDownload(row)">下载</el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px" v-if="batches.length > 0">
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
      <el-empty v-if="!loading && batches.length === 0 && queried" description="暂无计算结果" />
    </el-card>

    <!-- 雷达图详情 -->
    <el-card v-if="radarData && selectedBatch" shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h3 style="margin:0;font-size:15px">{{ selectedBatch.enrollmentYear ? selectedBatch.enrollmentYear + ' 级 — ' : '' }}{{ selectedBatch.majorName || '' }} 专业评价雷达图</h3>
          <div>
            <el-button size="small" type="primary" @click="exportRadarImage">导出图片</el-button>
            <el-button size="small" @click="closeRadar">关闭</el-button>
          </div>
        </div>
      </template>
      <div style="display:flex;align-items:center;gap:32px;flex-wrap:wrap">
        <div ref="radarChartRef" style="width:480px;height:380px"></div>
        <div style="flex:1;min-width:280px">
          <h4 style="margin:0 0 12px;font-size:14px;color:#606266">各指标点达成度</h4>
          <div v-for="(value, key) in radarData" :key="key" style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
            <span style="min-width:50px;font-weight:600;font-size:13px">{{ key }}</span>
            <el-progress :percentage="Math.round(value * 100)" :stroke-width="16" style="flex:1" :color="value >= 0.7 ? '#67C23A' : '#E6A23C'" />
            <span style="min-width:48px;text-align:right;font-size:13px;color:#303133">{{ (value * 100).toFixed(1) }}%</span>
          </div>
          <p v-if="selectedBatch.calcTime" style="margin:12px 0 0;font-size:12px;color:#909399">计算时间：{{ selectedBatch.calcTime }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
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
    ctx.fillStyle = '#303133'
    ctx.font = 'bold 18px sans-serif'
    ctx.fillText(title, 20, 30)

    ctx.drawImage(chartImg, 0, 44, chartW, chartH)

    let y = 44 + 10
    ctx.fillStyle = '#F5F7FA'
    ctx.fillRect(tableX, y, tableW, headerH)
    ctx.strokeStyle = '#EBEEF5'
    ctx.strokeRect(tableX, y, tableW, headerH)
    ctx.fillStyle = '#303133'
    ctx.font = 'bold 14px sans-serif'
    ctx.fillText('指标点', tableX + 16, y + 28)
    ctx.fillText('达成度', tableX + 160, y + 28)
    y += headerH

    ctx.font = '13px sans-serif'
    keys.forEach((key, i) => {
      const pct = (values[i] * 100).toFixed(1) + '%'
      ctx.fillStyle = i % 2 === 0 ? '#FFFFFF' : '#FAFAFA'
      ctx.fillRect(tableX, y, tableW, rowH)
      ctx.strokeStyle = '#EBEEF5'
      ctx.strokeRect(tableX, y, tableW, rowH)
      ctx.fillStyle = '#303133'
      ctx.fillText(key, tableX + 16, y + 24)
      const barX = tableX + 100, barW = 120, barH = 12, barY = y + 12
      ctx.fillStyle = '#EBEEF5'
      ctx.fillRect(barX, barY, barW, barH)
      ctx.fillStyle = values[i] >= 0.7 ? '#67C23A' : '#E6A23C'
      ctx.fillRect(barX, barY, barW * values[i], barH)
      ctx.fillStyle = '#303133'
      ctx.textAlign = 'right'
      ctx.fillText(pct, tableX + tableW - 16, y + 24)
      ctx.textAlign = 'left'
      y += rowH
    })

    if (b.calcTime) {
      ctx.fillStyle = '#909399'
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
      axisName: { fontSize: 13, color: '#303133' },
      splitLine: { lineStyle: { color: '#E4E7ED' } },
      splitArea: { areaStyle: { color: ['#F5F7FA', '#FFFFFF'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '达成度',
        areaStyle: { color: 'rgba(64,158,255,0.15)' },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' }
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

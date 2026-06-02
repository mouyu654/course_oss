<template>
  <div class="page-container">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="专业" required>
          <el-select v-model="selectedMajorId" placeholder="请选择专业" filterable clearable style="width:260px">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期" required>
          <el-select v-model="selectedSemesterId" placeholder="请选择学期" filterable clearable style="width:220px">
            <el-option v-for="s in semesterOptions" :key="s.id" :label="`${s.academicYear} ${s.semester === 1 ? '第一学期' : '第二学期'}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="!selectedMajorId || !selectedSemesterId" @click="loadData">加载数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span>专业达成度雷达图</span></template>
          <div ref="chartRef" style="width:100%;height:400px"></div>
          <el-empty v-if="!radarData && !loading" description="点击加载数据查看雷达图" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span>各指标点达成度</span></template>
          <el-table :data="indicatorTable" stripe border size="small" style="width:100%">
            <el-table-column prop="indicatorNo" label="指标点" width="100" />
            <el-table-column prop="content" label="描述" show-overflow-tooltip />
            <el-table-column label="达成度" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.achievement >= 0.7 ? 'success' : row.achievement >= 0.6 ? 'warning' : 'danger'" size="small">
                  {{ (row.achievement * 100).toFixed(1) }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!indicatorTable.length && !loading" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getMajorRadar, getGlobalResults } from '@/api/academic'
import { getGradReqs } from '@/api/director'
import { getSemesters, getMajors } from '@/api/admin'
import * as echarts from 'echarts'

const majorOptions = ref([])
const semesterOptions = ref([])
const selectedMajorId = ref(null)
const selectedSemesterId = ref(null)
const loading = ref(false)
const radarData = ref(null)
const indicatorTable = ref([])
const chartRef = ref(null)
let chartInstance = null
let resizeHandler = null

onMounted(async () => {
  const [semRes, majRes] = await Promise.all([getSemesters(), getMajors()])
  semesterOptions.value = semRes?.data || semRes || []
  majorOptions.value = majRes?.data?.records || majRes?.data || []
  resizeHandler = () => chartInstance?.resize()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  chartInstance?.dispose()
})

async function loadData() {
  if (!selectedMajorId.value || !selectedSemesterId.value) return
  loading.value = true
  try {
    const [radarRes, resultsRes, gradReqsRes] = await Promise.all([
      getMajorRadar({ majorId: selectedMajorId.value, semesterId: selectedSemesterId.value }),
      getGlobalResults({ majorId: selectedMajorId.value, semesterId: selectedSemesterId.value }),
      getGradReqs(selectedMajorId.value)
    ])
    const radar = radarRes?.data || radarRes
    const results = resultsRes?.data || resultsRes
    const gradReqs = gradReqsRes?.data || gradReqsRes
    radarData.value = radar?.radarData || radar
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
      splitNumber: 5
    },
    series: [{
      type: 'radar',
      data: [{ value: data.value || [], name: '专业达成度', areaStyle: { opacity: 0.15 } }]
    }]
  })
}
</script>

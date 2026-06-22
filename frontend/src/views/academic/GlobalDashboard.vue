<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :inline="true" @submit.prevent="handleSearch">
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
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="dashboard">
      <!-- 顶部：左侧统计 + 右侧饼图 + 计算按钮 -->
      <el-card shadow="never" style="margin-bottom:16px">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:24px">
          <!-- 左：统计数字和进度 -->
          <div style="flex:1;min-width:300px">
            <div style="display:flex;align-items:baseline;gap:8px;margin-bottom:12px">
              <span style="font-size:14px;color:#606266">支撑课程计算进度</span>
              <span style="font-size:30px;font-weight:700;color:#303133">{{ dashboard.lockedCount }}</span>
              <span style="font-size:16px;color:#909399">/ {{ dashboard.totalCount }} 门</span>
            </div>
            <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="20" style="max-width:400px" />
            <div style="margin-top:12px;display:flex;align-items:center;gap:12px">
              <el-tag v-if="dashboard.allReady" type="success" effect="dark">所有课程已就绪</el-tag>
              <el-tag v-else type="warning" effect="dark">尚有 {{ dashboard.totalCount - dashboard.lockedCount }} 门未就绪</el-tag>
              <el-button type="primary" :loading="computing" :disabled="!canCompute" @click="handleCompute">执行专业级计算</el-button>
            </div>
          </div>
          <!-- 右：饼图 -->
          <div ref="pieChartRef" style="width:220px;height:200px"></div>
        </div>
      </el-card>

      <!-- 权重配置校验 -->
      <el-card v-if="hasWeightIssues" shadow="never" style="margin-bottom:16px">
        <template #header><span style="font-weight:600;color:#E6A23C">权重配置异常</span></template>
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px">
          以下指标点的宏观支撑权重之和不为 1.0，请联系专业负责人修正后再执行计算。
        </el-alert>
        <el-table :data="dashboard.weightStatuses.filter(w => !w.valid)" stripe size="small" style="width:100%">
          <el-table-column prop="indicatorId" label="指标点ID" width="120" align="center" />
          <el-table-column label="权重之和" width="120" align="center">
            <template #default="{ row }">
              <span style="color:#F56C6C;font-weight:600">{{ row.weightSum }}</span>
            </template>
          </el-table-column>
          <el-table-column label="期望值" width="100" align="center">
            <template #default>1.0000</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 课程状态明细表 -->
      <el-card shadow="never">
        <template #header><span style="font-weight:600">支撑课程状态明细</span></template>
        <el-table :data="classDetailList" v-loading="loading" stripe style="width:100%">
          <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="className" label="教学班级" min-width="160" show-overflow-tooltip />
          <el-table-column prop="teacherName" label="主讲教师" width="120" />
          <el-table-column prop="semesterName" label="学期编码" width="160" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-if="!loading && !dashboard" description="请选择查询条件以加载宏观看板" />
    <div v-if="loading && !dashboard" v-loading="true" element-loading-text="加载中..." style="min-height:200px" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getEnrollmentYears, getGlobalDashboard, triggerGlobalCompute } from '@/api/academic'
import { getMajors } from '@/api/admin'
import StatusTag from '@/components/StatusTag.vue'

const yearOptions = ref([])
const majorOptions = ref([])
const filters = ref({ enrollmentYear: null, majorId: null })
const dashboard = ref(null)
const loading = ref(false)
const computing = ref(false)

const pieChartRef = ref(null)
let pieChart = null
let resizeHandler = null

const progressPercent = computed(() => {
  if (!dashboard.value || dashboard.value.totalCount === 0) return 0
  return Math.round((dashboard.value.lockedCount / dashboard.value.totalCount) * 100)
})
const progressStatus = computed(() => {
  if (!dashboard.value) return ''
  return dashboard.value.allReady ? 'success' : ''
})

const hasWeightIssues = computed(() => {
  return dashboard.value?.weightStatuses?.some(w => !w.valid) || false
})

const canCompute = computed(() => {
  return dashboard.value?.allReady && !hasWeightIssues.value
})

const classDetailList = computed(() => {
  if (!dashboard.value?.courseStatuses) return []
  const list = []
  for (const cs of dashboard.value.courseStatuses) {
    for (const cd of (cs.classDetails || [])) {
      list.push({ ...cd, courseName: cs.courseName })
    }
  }
  return list
})

function handleSearch() {
  if (filters.value.enrollmentYear || filters.value.majorId) {
    loadDashboard()
  } else {
    dashboard.value = null
    ElMessage.warning('请至少选择一个查询条件')
  }
}

function handleReset() {
  filters.value = { enrollmentYear: null, majorId: null }
  dashboard.value = null
}

async function loadOptions() {
  try {
    const [yearRes, majRes] = await Promise.all([getEnrollmentYears(), getMajors()])
    yearOptions.value = yearRes?.data || yearRes || []
    majorOptions.value = majRes?.data?.records || majRes?.data || []
  } catch (e) {
    console.error('Failed to load options', e)
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const params = {}
    if (filters.value.enrollmentYear) params.enrollmentYear = filters.value.enrollmentYear
    if (filters.value.majorId) params.majorId = filters.value.majorId
    const res = await getGlobalDashboard(params)
    dashboard.value = res?.data || res
    await nextTick()
    renderPieChart()
  } catch (e) {
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

function renderPieChart() {
  if (!pieChartRef.value || !dashboard.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const locked = dashboard.value.lockedCount
  const unlocked = dashboard.value.totalCount - locked

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}门 ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['42%', '72%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}门', fontSize: 12 },
      data: [
        { value: locked, name: '已锁定', itemStyle: { color: '#67C23A' } },
        { value: unlocked, name: '未提交', itemStyle: { color: '#E6A23C' } }
      ]
    }]
  })
}

async function handleCompute() {
  if (!filters.value.majorId) {
    ElMessage.warning('请先选择专业')
    return
  }
  if (!filters.value.enrollmentYear) {
    ElMessage.warning('请先选择年级')
    return
  }
  try {
    await ElMessageBox.confirm('确定要执行专业级计算吗？此操作将基于所有已锁定课程的达成度进行汇总计算。', '确认', { type: 'warning' })
  } catch { return }
  computing.value = true
  try {
    const params = {}
    if (filters.value.enrollmentYear) params.enrollmentYear = filters.value.enrollmentYear
    if (filters.value.majorId) params.majorId = filters.value.majorId
    await triggerGlobalCompute(params)
    ElMessage.success('专业级计算执行完成')
    await loadDashboard()
  } finally {
    computing.value = false
  }
}

onMounted(() => {
  loadOptions()
  resizeHandler = () => pieChart?.resize()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  pieChart?.dispose()
})
</script>

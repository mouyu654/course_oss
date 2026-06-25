<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card class="main-card filter-card">
      <div class="filter-section">
        <el-form :inline="true" @submit.prevent="handleSearch" class="filter-form">
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
            <el-button type="primary" @click="handleSearch">
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

    <template v-if="dashboard">
      <!-- 顶部：左侧统计 + 右侧饼图 + 计算按钮 -->
      <el-card class="main-card stats-card">
        <div class="stats-content">
          <!-- 左：统计数字和进度 -->
          <div class="stats-left">
            <div class="stats-header">
              <span class="stats-label">支撑课程计算进度</span>
              <div class="stats-numbers">
                <span class="stats-current">{{ dashboard.lockedCount }}</span>
                <span class="stats-total">/ {{ dashboard.totalCount }} 门</span>
              </div>
            </div>
            <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="20" class="stats-progress" />
            <div class="stats-footer">
              <el-tag v-if="dashboard.allReady" type="success" effect="dark" class="status-tag">
                <el-icon><CircleCheck /></el-icon>
                所有课程已就绪
              </el-tag>
              <el-tag v-else type="warning" effect="dark" class="status-tag">
                <el-icon><Warning /></el-icon>
                尚有 {{ dashboard.totalCount - dashboard.lockedCount }} 门未就绪
              </el-tag>
              <el-button type="primary" :loading="computing" :disabled="!canCompute" @click="handleCompute" class="compute-btn">
                <el-icon><TrendCharts /></el-icon>
                执行专业级计算
              </el-button>
            </div>
          </div>
          <!-- 右：饼图 -->
          <div class="stats-right">
            <div ref="pieChartRef" class="pie-chart"></div>
          </div>
        </div>
      </el-card>

      <!-- 权重配置校验 -->
      <el-card v-if="hasWeightIssues" class="main-card warning-card">
        <template #header>
          <div class="warning-header">
            <el-icon class="warning-icon"><WarningFilled /></el-icon>
            <span class="warning-title">权重配置异常</span>
          </div>
        </template>
        <el-alert type="warning" :closable="false" show-icon class="warning-alert">
          以下指标点的宏观支撑权重之和不为 1.0，请联系专业负责人修正后再执行计算。
        </el-alert>
        <el-table :data="dashboard.weightStatuses.filter(w => !w.valid)" stripe size="small" class="warning-table">
          <el-table-column prop="indicatorId" label="指标点ID" width="120" align="center">
            <template #default="{ row }">
              <span class="indicator-id">{{ row.indicatorId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="权重之和" width="120" align="center">
            <template #default="{ row }">
              <span class="weight-invalid">{{ row.weightSum }}</span>
            </template>
          </el-table-column>
          <el-table-column label="期望值" width="100" align="center">
            <template #default>
              <span class="weight-expected">1.0000</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 课程状态明细表 -->
      <el-card class="main-card detail-card">
        <template #header>
          <div class="detail-header">
            <div class="detail-icon">
              <el-icon><Document /></el-icon>
            </div>
            <span class="detail-title">支撑课程状态明细</span>
          </div>
        </template>
        <el-table :data="classDetailList" v-loading="loading" stripe class="detail-table">
          <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="course-name">{{ row.courseName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="className" label="教学班级" min-width="160" show-overflow-tooltip />
          <el-table-column prop="teacherName" label="主讲教师" width="120" />
          <el-table-column prop="semesterName" label="学期编码" width="160">
            <template #default="{ row }">
              <el-tag type="info" effect="plain" size="small">{{ row.semesterName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-if="!loading && !dashboard" description="请选择查询条件以加载宏观看板" class="empty-state" />
    <div v-if="loading && !dashboard" v-loading="true" element-loading-text="加载中..." style="min-height:200px" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, CircleCheck, Warning, WarningFilled, TrendCharts, Document } from '@element-plus/icons-vue'
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
        { value: locked, name: '已锁定', itemStyle: { color: '#059669' } },
        { value: unlocked, name: '未提交', itemStyle: { color: '#D97706' } }
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

/* ===== Stats Card ===== */
.stats-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}

.stats-left {
  flex: 1;
  min-width: 300px;
}

.stats-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.stats-label {
  font-size: 14px;
  color: #64748B;
}

.stats-numbers {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stats-current {
  font-size: 36px;
  font-weight: 700;
  color: #1E293B;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.stats-total {
  font-size: 16px;
  color: #94A3B8;
}

.stats-progress {
  max-width: 400px;
  margin-bottom: 16px;
}

.stats-footer {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
}

.compute-btn {
  background: linear-gradient(135deg, #2563EB 0%, #3B82F6 100%);
  border: none;
}

.compute-btn:hover {
  background: linear-gradient(135deg, #1D4ED8 0%, #2563EB 100%);
}

.stats-right {
  flex-shrink: 0;
}

.pie-chart {
  width: 240px;
  height: 220px;
}

/* ===== Warning Card ===== */
.warning-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.warning-icon {
  font-size: 20px;
  color: #D97706;
}

.warning-title {
  font-weight: 600;
  color: #D97706;
  font-size: 16px;
}

.warning-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}

.warning-table {
  border-radius: 8px;
  overflow: hidden;
}

.indicator-id {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #475569;
}

.weight-invalid {
  color: #DC2626;
  font-weight: 700;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.weight-expected {
  color: #059669;
  font-family: 'SF Mono', 'Consolas', monospace;
}

/* ===== Detail Card ===== */
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-icon {
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

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.detail-table {
  border-radius: 8px;
  overflow: hidden;
}

.detail-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}

.course-name {
  font-weight: 500;
  color: #1E293B;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 60px 0;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .stats-content {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-right {
    display: flex;
    justify-content: center;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-select,
  .filter-select-large {
    width: 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .compute-btn,
  .detail-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

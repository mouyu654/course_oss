<template>
  <div class="page-container">
    <!-- 快速搜索（保留） -->
    <el-card class="main-card search-card">
      <div class="search-section">
        <el-form :inline="true" @submit.prevent class="search-form">
          <el-form-item label="快速查找（学号）">
            <el-input v-model="searchNo" placeholder="输入学号直接查询" clearable class="search-input" @keyup.enter="searchByNo">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="searchByNo">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 逐级浏览 -->
    <el-card v-if="!showReport" class="main-card browse-card">
      <template #header>
        <div class="browse-header">
          <el-breadcrumb separator=">" class="breadcrumb-nav">
            <el-breadcrumb-item :to="{ path: '#' }" @click="goBreadcrumb(0)">
              <el-icon><School /></el-icon>
              学院
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="selectedCollege" :to="{ path: '#' }" @click="goBreadcrumb(1)">
              {{ selectedCollege.name }}
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="selectedMajor" :to="{ path: '#' }" @click="goBreadcrumb(2)">
              {{ selectedMajor.name }}
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="selectedAdminClass" :to="{ path: '#' }" @click="goBreadcrumb(3)">
              {{ selectedAdminClass.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <div class="step-indicator">
            <el-tag type="info" effect="plain">第 {{ step }} 步</el-tag>
          </div>
        </div>
      </template>

      <!-- Step 1: 学院列表 -->
      <div v-if="step === 1" class="step-content">
        <h4 class="step-title">选择学院</h4>
        <el-row :gutter="16">
          <el-col v-for="c in colleges" :key="c.id" :span="6" style="margin-bottom:16px">
            <el-card shadow="hover" class="college-card" @click="selectCollege(c)">
              <div class="college-content">
                <div class="college-icon">
                  <el-icon><School /></el-icon>
                </div>
                <span class="college-name">{{ c.name }}</span>
              </div>
            </el-card>
          </el-col>
          <el-empty v-if="!colleges.length" description="暂无学院数据" />
        </el-row>
      </div>

      <!-- Step 2: 专业列表 -->
      <div v-if="step === 2" class="step-content">
        <h4 class="step-title">选择专业</h4>
        <el-table :data="majors" stripe highlight-current-row @row-click="selectMajor" class="browse-table">
          <el-table-column prop="name" label="专业名称" min-width="200">
            <template #default="{ row }">
              <span class="major-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="专业代码" width="150">
            <template #default="{ row }">
              <span class="code-text">{{ row.code }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="step === 2 && !majors.length" description="该学院下暂无专业" />
      </div>

      <!-- Step 3: 行政班级列表 -->
      <div v-if="step === 3" class="step-content">
        <h4 class="step-title">选择班级</h4>
        <el-table :data="adminClasses" stripe highlight-current-row @row-click="selectAdminClass" class="browse-table">
          <el-table-column prop="className" label="班级名称" min-width="200">
            <template #default="{ row }">
              <span class="class-name">{{ row.className }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="enrollmentYear" label="年级" width="100">
            <template #default="{row}">
              <span class="year-text">{{ row.enrollmentYear }}级</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="step === 3 && !adminClasses.length" description="该专业下暂无班级" />
      </div>

      <!-- Step 4: 学生列表 -->
      <div v-if="step === 4" class="step-content">
        <h4 class="step-title">选择学生</h4>
        <el-table :data="students" stripe highlight-current-row @row-click="selectStudent" class="browse-table">
          <el-table-column prop="studentNo" label="学号" width="150">
            <template #default="{ row }">
              <span class="student-no">{{ row.studentNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="120">
            <template #default="{ row }">
              <span class="student-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="enrollmentYear" label="入学年份" width="100">
            <template #default="{row}">
              <span class="year-text">{{ row.enrollmentYear }}级</span>
            </template>
          </el-table-column>
          <el-table-column prop="enrollmentStatus" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.enrollmentStatus)" size="small" effect="dark" class="status-tag">
                {{ row.enrollmentStatus }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="step === 4 && !students.length" description="该班级下暂无学生" />
      </div>
    </el-card>

    <!-- 达成度报告 -->
    <el-card v-if="showReport" class="main-card report-card">
      <template #header>
        <div class="report-header">
          <div class="report-title-group">
            <el-button link type="primary" @click="showReport = false" class="back-btn">
              <el-icon><ArrowLeft /></el-icon>
              返回列表
            </el-button>
            <div class="report-icon">
              <el-icon><Document /></el-icon>
            </div>
            <span class="report-title">学生个人达成度报告</span>
          </div>
          <div class="report-actions">
            <el-button type="success" @click="downloadExcel">
              <el-icon><Download /></el-icon>
              下载 Excel
            </el-button>
            <el-button type="warning" @click="downloadWord">
              <el-icon><Document /></el-icon>
              下载 Word
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="4" border class="student-info">
        <el-descriptions-item label="姓名">
          <span class="info-value">{{ reportData?.student?.name }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="学号">
          <span class="info-value student-no">{{ reportData?.student?.studentNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="专业">
          <span class="info-value">{{ reportData?.student?.major }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="学院">
          <span class="info-value">{{ reportData?.student?.college }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="section-header">
        <div class="section-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <h4 class="section-title">毕业要求指标点达成度总览</h4>
      </div>
      <el-table v-if="reportData && Object.keys(reportData.indicatorAchievements || {}).length" :data="indicatorRows" border stripe highlight-current-row @current-change="onIndicatorSelect" class="indicator-table">
        <el-table-column prop="no" label="指标点" width="120" align="center">
          <template #default="{ row }">
            <span class="indicator-no">{{ row.no }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="指标名称" min-width="250" />
        <el-table-column label="达成度" width="100" align="center">
          <template #default="{ row }">
            <span :class="['achievement-value', row.value >= 0.6 ? 'text-pass' : 'text-fail']">
              {{ row.value.toFixed(4) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="溯源" width="80" align="center">
          <template #default>
            <el-button link type="primary">
              <el-icon><View /></el-icon>
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 溯源结果 -->
      <el-card v-if="traceData" shadow="never" class="trace-card">
        <template #header>
          <div class="trace-header">
            <div class="trace-title-group">
              <div class="trace-icon">
                <el-icon><Connection /></el-icon>
              </div>
              <span class="trace-title">指标 {{ traceData.indicatorNo }} 达成度溯源：{{ traceData.indicatorContent }}</span>
            </div>
            <el-tag type="primary" effect="dark" class="trace-tag">总达成度: {{ traceData.overallAchievement.toFixed(4) }}</el-tag>
          </div>
        </template>
        <div v-for="(course, ci) in traceData.courses" :key="ci" class="trace-course">
          <div class="course-header">
            <h5 class="course-title">
              {{ course.courseCode }} {{ course.courseName }} ({{ course.className }})
            </h5>
            <el-tag size="small" type="info" effect="plain">达成度: {{ course.indicatorAchievement.toFixed(4) }}</el-tag>
          </div>
          <el-table :data="course.objectives" border stripe size="small" class="trace-table">
            <el-table-column prop="objNo" label="课程目标" width="120">
              <template #default="{ row: r }">
                <span class="obj-no">{{ r.objNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="weight" label="权重" width="80">
              <template #default="{ row: r }">
                <span class="weight-value">{{ r.weight.toFixed(4) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="目标达成度" width="120" align="center">
              <template #default="{ row: r }">
                <span :class="['achievement-value', r.objAchievement >= 0.6 ? 'text-pass' : 'text-fail']">
                  {{ r.objAchievement.toFixed(4) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="贡献值" width="120" align="center">
              <template #default="{ row: r }">
                <span class="contribution-value">{{ (r.weight * r.objAchievement).toFixed(4) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="考核点成绩明细" min-width="300">
              <template #default="{ row: r }">
                <div class="score-list">
                  <span v-for="(s, si) in r.scores" :key="si" class="score-item">
                    {{ s.assessmentName }}{{ s.questionContent ? '-' + s.questionContent : '' }}:
                    <b>{{ s.score }}/{{ s.maxScore || '-' }}</b>
                  </span>
                  <span v-if="!r.scores.length" class="no-score">-</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-if="!traceData.courses.length" description="该指标暂无课程数据" :image-size="60" />
      </el-card>

      <div v-if="reportData?.courses?.length" class="section-header" style="margin-top: 24px;">
        <div class="section-icon" style="background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <h4 class="section-title">各课程达成度明细</h4>
      </div>
      <el-collapse accordion class="course-collapse">
        <el-collapse-item v-for="course in reportData.courses" :key="course.courseId" class="course-item">
          <template #title>
            <div class="course-collapse-title">
              <span class="course-code">{{ course.courseCode }}</span>
              <span class="course-name">{{ course.courseName }}</span>
              <span class="course-class">({{ course.className }})</span>
            </div>
          </template>
          <div class="course-detail">
            <h5 class="detail-subtitle">
              <el-icon><Flag /></el-icon>
              课程目标达成度
            </h5>
            <el-table :data="objRows(course)" border stripe size="small" class="detail-table">
              <el-table-column prop="label" label="目标" width="80" align="center">
                <template #default="{ row: r }">
                  <span class="obj-label">{{ r.label }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="desc" label="目标描述" min-width="250" show-overflow-tooltip />
              <el-table-column label="达成度" width="100" align="center">
                <template #default="{ row: r }">
                  <span :class="['achievement-value', r.value >= 0.6 ? 'text-pass' : 'text-fail']">
                    {{ r.value.toFixed(4) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>

            <h5 class="detail-subtitle" style="margin-top: 20px;">
              <el-icon><Aim /></el-icon>
              指标点达成度
            </h5>
            <el-table :data="indRows(course)" border stripe size="small" class="detail-table">
              <el-table-column prop="label" label="指标点" width="80" align="center">
                <template #default="{ row: r }">
                  <span class="ind-label">{{ r.label }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="desc" label="指标点描述" min-width="250" show-overflow-tooltip />
              <el-table-column label="达成度" width="100" align="center">
                <template #default="{ row: r }">
                  <span :class="['achievement-value', r.value >= 0.6 ? 'text-pass' : 'text-fail']">
                    {{ r.value.toFixed(4) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ArrowLeft, Download, Document, TrendCharts, View, Connection, DataAnalysis, Flag, Aim, School } from '@element-plus/icons-vue'
import { getColleges, getMajors, getAdminClasses, getAdminClassStudents } from '@/api/admin'
import request from '@/utils/request'

const searchNo = ref('')
const loading = ref(false)

// Navigation state
const step = ref(1)
const selectedCollege = ref(null)
const selectedMajor = ref(null)
const selectedAdminClass = ref(null)

const colleges = ref([])
const majors = ref([])
const adminClasses = ref([])
const students = ref([])

const showReport = ref(false)
const reportData = ref(null)
const currentStudentId = ref(null)
const traceData = ref(null)
const selectedIndicatorId = ref(null)

const indicatorRows = computed(() => {
  if (!reportData.value) return []
  return Object.entries(reportData.value.indicatorAchievements || {}).map(([no, value]) => ({
    no, name: reportData.value.indicatorNames?.[no] || '', value
  }))
})

function objRows(c) {
  return Object.entries(c.objectiveAchievements || {}).map(([k, v]) => ({
    label: c.objectiveLabels?.[k] || '' + k,
    desc: c.objectiveDescs?.[k] || '',
    value: v
  }))
}
function indRows(c) {
  return Object.entries(c.indicatorAchievements || {}).map(([k, v]) => ({
    label: c.indicatorLabels?.[k] || '' + k,
    desc: c.indicatorDescs?.[k] || '',
    value: v
  }))
}

function statusTagType(status) {
  const map = { '在读': 'success', '毕业': 'info', '休学': 'warning', '退学': 'danger', '延毕': 'warning' }
  return map[status] || 'info'
}

onMounted(async () => {
  try { const r = await getColleges(); colleges.value = r?.data || r || [] } catch { }
})

function goBreadcrumb(level) {
  if (level === 0) { selectedCollege.value = null; selectedMajor.value = null; selectedAdminClass.value = null; step.value = 1 }
  else if (level === 1) { selectedMajor.value = null; selectedAdminClass.value = null; step.value = 2; loadMajors() }
  else if (level === 2) { selectedAdminClass.value = null; step.value = 3; loadAdminClasses() }
  else if (level === 3) { step.value = 4; loadStudents(selectedAdminClass.value.id) }
}

function selectCollege(c) {
  selectedCollege.value = c; step.value = 2; loadMajors()
}
function selectMajor(m) {
  selectedMajor.value = m; step.value = 3; loadAdminClasses()
}
function selectAdminClass(ac) {
  selectedAdminClass.value = ac; step.value = 4; loadStudents(ac.id)
}
async function selectStudent(s) {
  currentStudentId.value = s.id
  await loadReport(s.id)
}

async function loadMajors() {
  try {
    const r = await getMajors({ collegeId: selectedCollege.value.id, page: 1, size: 200 })
    majors.value = r?.data?.records || r?.data || []
  } catch { majors.value = [] }
}

async function loadAdminClasses() {
  try {
    const r = await getAdminClasses({ majorId: selectedMajor.value.id, page: 1, size: 200 })
    adminClasses.value = r?.data?.records || r?.data || []
  } catch { adminClasses.value = [] }
}

async function loadStudents(adminClassId) {
  try {
    students.value = await getAdminClassStudents(adminClassId)
    if (!Array.isArray(students.value)) students.value = students.value?.data || []
  } catch { students.value = [] }
}

async function loadReport(studentId) {
  loading.value = true
  try {
    const res = await request.get('/reports/student/' + studentId)
    reportData.value = res?.data || res
    showReport.value = true
  } catch {
    ElMessage.error('查询失败，请确认该学生所在课程已完成计算')
  } finally { loading.value = false }
}

async function onIndicatorSelect(row) {
  if (!row) return
  const indId = reportData.value?.indicatorIds?.[row.no]
  if (!indId) { ElMessage.warning('未找到该指标ID'); return }
  selectedIndicatorId.value = row.no
  try {
    const res = await request.get('/reports/student/' + currentStudentId.value + '/trace/' + indId)
    traceData.value = res?.data || res
  } catch { ElMessage.error('溯源查询失败') }
}

async function searchByNo() {
  if (!searchNo.value.trim()) { ElMessage.warning('请输入学号'); return }
  loading.value = true
  showReport.value = false
  try {
    const res = await request.get('/reports/student/by-no/' + encodeURIComponent(searchNo.value.trim()))
    reportData.value = res?.data || res
    showReport.value = true
  } catch {
    ElMessage.error('查询失败，请确认学号正确且该学生所在课程已完成计算')
  } finally { loading.value = false }
}

async function downloadExcel() {
  if (!currentStudentId.value) return
  try {
    const res = await request.get('/reports/student/' + currentStudentId.value + '/excel', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a'); a.href = url; a.download = '学生个人达成度报告.xlsx'; a.click(); window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch { ElMessage.error('下载失败') }
}

async function downloadWord() {
  if (!currentStudentId.value) return
  try {
    const res = await request.get('/reports/student/' + currentStudentId.value + '/word', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a'); a.href = url; a.download = '学生个人达成度报告.docx'; a.click(); window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch { ElMessage.error('下载失败') }
}
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

/* ===== Search Section ===== */
.search-card :deep(.el-card__body) {
  padding: 16px 24px;
}

.search-section {
  background: #F8FAFC;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #F1F5F9;
}

.search-form {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-input {
  width: 240px;
}

/* ===== Browse Card ===== */
.browse-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb-nav {
  font-size: 14px;
}

.step-indicator {
  flex-shrink: 0;
}

.step-content {
  padding: 8px 0;
}

.step-title {
  margin: 0 0 20px;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

/* ===== College Card ===== */
.college-card {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #E2E8F0;
}

.college-card:hover {
  border-color: #2563EB;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}

.college-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.college-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563EB;
  font-size: 24px;
}

.college-name {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  text-align: center;
}

/* ===== Browse Table ===== */
.browse-table {
  border-radius: 8px;
  overflow: hidden;
}

.browse-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.browse-table :deep(.el-table__row) {
  cursor: pointer;
}

.major-name {
  font-weight: 500;
  color: #1E293B;
}

.code-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #64748B;
}

.class-name {
  font-weight: 500;
  color: #1E293B;
}

.year-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #475569;
}

.student-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #7C3AED;
}

.student-name {
  font-weight: 500;
  color: #1E293B;
}

.status-tag {
  border-radius: 6px;
  font-weight: 600;
}

/* ===== Report Card ===== */
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  margin-right: 8px;
}

.report-icon {
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

.report-title {
  font-size: 18px;
  font-weight: 600;
  color: #1E293B;
}

.report-actions {
  display: flex;
  gap: 8px;
}

/* ===== Student Info ===== */
.student-info {
  margin-bottom: 24px;
}

.info-value {
  font-weight: 500;
  color: #1E293B;
}

/* ===== Section Header ===== */
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7C3AED;
  font-size: 18px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

/* ===== Indicator Table ===== */
.indicator-table {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.indicator-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.indicator-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #7C3AED;
}

.achievement-value {
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.text-pass { color: #059669; }
.text-fail { color: #DC2626; }

/* ===== Trace Card ===== */
.trace-card {
  margin: 16px 0;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
}

.trace-card :deep(.el-card__header) {
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
}

.trace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trace-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.trace-icon {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563EB;
  font-size: 16px;
}

.trace-title {
  font-weight: 600;
  color: #1E293B;
  font-size: 14px;
}

.trace-tag {
  border-radius: 6px;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.trace-course {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #E2E8F0;
}

.trace-course:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.course-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.course-title {
  margin: 0;
  color: #2563EB;
  font-size: 14px;
  font-weight: 600;
}

.trace-table {
  border-radius: 8px;
  overflow: hidden;
}

.obj-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #475569;
}

.weight-value {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #64748B;
}

.contribution-value {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #1E293B;
}

.score-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
}

.score-item {
  color: #475569;
}

.no-score {
  color: #94A3B8;
  font-style: italic;
}

/* ===== Course Collapse ===== */
.course-collapse {
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
}

.course-item :deep(.el-collapse-item__header) {
  background: #F8FAFC;
  padding: 0 20px;
  font-weight: 500;
}

.course-item :deep(.el-collapse-item__content) {
  padding: 20px;
}

.course-collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-code {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #2563EB;
}

.course-name {
  font-weight: 500;
  color: #1E293B;
}

.course-class {
  color: #64748B;
}

.course-detail {
  padding: 8px 0;
}

.detail-subtitle {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-table {
  border-radius: 8px;
  overflow: hidden;
}

.obj-label {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #7C3AED;
}

.ind-label {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #059669;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .report-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .course-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .college-card,
  .browse-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

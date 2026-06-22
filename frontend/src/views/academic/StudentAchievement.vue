<template>
  <div class="page-container">
    <!-- 快速搜索（保留） -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="快速查找（学号）">
          <el-input v-model="searchNo" placeholder="输入学号直接查询" clearable style="width:220px" @keyup.enter="searchByNo" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="searchByNo">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 逐级浏览 -->
    <el-card v-if="!showReport" shadow="never">
      <template #header>
        <el-breadcrumb separator=">">
          <el-breadcrumb-item :to="{ path: '#' }" @click="goBreadcrumb(0)">学院</el-breadcrumb-item>
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
      </template>

      <!-- Step 1: 学院列表 -->
      <el-row v-if="step === 1" :gutter="16">
        <el-col v-for="c in colleges" :key="c.id" :span="6" style="margin-bottom:12px">
          <el-card shadow="hover" class="click-card" @click="selectCollege(c)">
            <div style="text-align:center;padding:12px">{{ c.name }}</div>
          </el-card>
        </el-col>
        <el-empty v-if="!colleges.length" description="暂无学院数据" />
      </el-row>

      <!-- Step 2: 专业列表 -->
      <el-table v-if="step === 2" :data="majors" stripe highlight-current-row @row-click="selectMajor" style="cursor:pointer">
        <el-table-column prop="name" label="专业名称" min-width="200" />
        <el-table-column prop="code" label="专业代码" width="150" />
      </el-table>
      <el-empty v-if="step === 2 && !majors.length" description="该学院下暂无专业" />

      <!-- Step 3: 行政班级列表 -->
      <el-table v-if="step === 3" :data="adminClasses" stripe highlight-current-row @row-click="selectAdminClass" style="cursor:pointer">
        <el-table-column prop="className" label="班级名称" min-width="200" />
        <el-table-column prop="enrollmentYear" label="年级" width="100">
          <template #default="{row}">{{ row.enrollmentYear }}级</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="step === 3 && !adminClasses.length" description="该专业下暂无班级" />

      <!-- Step 4: 学生列表 -->
      <el-table v-if="step === 4" :data="students" stripe highlight-current-row @row-click="selectStudent" style="cursor:pointer">
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="enrollmentYear" label="入学年份" width="100">
          <template #default="{row}">{{ row.enrollmentYear }}级</template>
        </el-table-column>
        <el-table-column prop="enrollmentStatus" label="状态" width="100" />
      </el-table>
      <el-empty v-if="step === 4 && !students.length" description="该班级下暂无学生" />
    </el-card>

    <!-- 达成度报告 -->
    <el-card v-if="showReport" shadow="never" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:15px">
            <el-button link type="primary" @click="showReport = false"><el-icon><ArrowLeft /></el-icon> 返回列表</el-button>
            学生个人达成度报告
          </span>
          <div>
            <el-button type="success" @click="downloadExcel">下载 Excel</el-button>
            <el-button type="warning" @click="downloadWord">下载 Word</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="4" border style="margin-bottom:16px">
        <el-descriptions-item label="姓名">{{ reportData?.student?.name }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ reportData?.student?.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ reportData?.student?.major }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ reportData?.student?.college }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin:16px 0 8px;font-weight:600">毕业要求指标点达成度总览</h4>
      <el-table v-if="reportData && Object.keys(reportData.indicatorAchievements || {}).length" :data="indicatorRows" border stripe highlight-current-row @current-change="onIndicatorSelect">
        <el-table-column prop="no" label="指标点" width="120" />
        <el-table-column prop="name" label="指标名称" min-width="250" />
        <el-table-column label="达成度" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.value >= 0.6 ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
              {{ row.value.toFixed(4) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="溯源" width="80" align="center">
          <template #default>
            <el-button link type="primary">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 溯源结果 -->
      <el-card v-if="traceData" shadow="never" style="margin:16px 0;background:#fafbfc">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-weight:600">指标 {{ traceData.indicatorNo }} 达成度溯源：{{ traceData.indicatorContent }}</span>
            <el-tag type="primary">总达成度: {{ traceData.overallAchievement.toFixed(4) }}</el-tag>
          </div>
        </template>
        <div v-for="(course, ci) in traceData.courses" :key="ci" style="margin-bottom:16px">
          <h5 style="margin:8px 0;color:#409EFF">
            {{ course.courseCode }} {{ course.courseName }} ({{ course.className }})
            <el-tag size="small" style="margin-left:8px">达成度: {{ course.indicatorAchievement.toFixed(4) }}</el-tag>
          </h5>
          <el-table :data="course.objectives" border stripe size="small">
            <el-table-column prop="objNo" label="课程目标" width="120" />
            <el-table-column prop="weight" label="权重" width="80">
              <template #default="{ row: r }">{{ r.weight.toFixed(4) }}</template>
            </el-table-column>
            <el-table-column label="目标达成度" width="120">
              <template #default="{ row: r }">
                <span :style="{ color: r.objAchievement >= 0.6 ? '#67C23A' : '#F56C6C' }">{{ r.objAchievement.toFixed(4) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="贡献值" width="120">
              <template #default="{ row: r }">
                {{ (r.weight * r.objAchievement).toFixed(4) }}
              </template>
            </el-table-column>
            <el-table-column label="考核点成绩明细" min-width="300">
              <template #default="{ row: r }">
                <span v-for="(s, si) in r.scores" :key="si" style="margin-right:12px;font-size:12px">
                  {{ s.assessmentName }}{{ s.questionContent ? '-' + s.questionContent : '' }}:
                  <b>{{ s.score }}/{{ s.maxScore || '-' }}</b>
                </span>
                <span v-if="!r.scores.length" style="color:#999">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-if="!traceData.courses.length" description="该指标暂无课程数据" :image-size="60" />
      </el-card>

      <h4 v-if="reportData?.courses?.length" style="margin:20px 0 8px;font-weight:600">各课程达成度明细</h4>
      <el-collapse accordion>
        <el-collapse-item v-for="course in reportData.courses" :key="course.courseId"
          :title="course.courseCode + ' ' + course.courseName + ' (' + course.className + ')'">
          <el-divider content-position="left">课程目标达成度</el-divider>
          <el-table :data="objRows(course)" border stripe size="small">
            <el-table-column prop="label" label="目标" width="80" />
            <el-table-column prop="desc" label="目标描述" min-width="250" show-overflow-tooltip />
            <el-table-column label="达成度" width="100">
              <template #default="{ row: r }">
                <span :style="{ color: r.value >= 0.6 ? '#67C23A' : '#F56C6C' }">{{ r.value.toFixed(4) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-divider content-position="left">指标点达成度</el-divider>
          <el-table :data="indRows(course)" border stripe size="small">
            <el-table-column prop="label" label="指标点" width="80" />
            <el-table-column prop="desc" label="指标点描述" min-width="250" show-overflow-tooltip />
            <el-table-column label="达成度" width="100">
              <template #default="{ row: r }">
                <span :style="{ color: r.value >= 0.6 ? '#67C23A' : '#F56C6C' }">{{ r.value.toFixed(4) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
.click-card { cursor: pointer; transition: all 0.2s; }
.click-card:hover { border-color: #409EFF; transform: translateY(-2px); }
</style>

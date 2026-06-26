<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllScores, unlockScore, getScoreDetails, getUnlockRequests, approveUnlockRequest, unlockApprovedRequest, rejectUnlockRequest, getSemesters } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, View, Unlock } from '@element-plus/icons-vue'

const activeTab = ref('sheets')

// ===== 成绩单列表 =====
const loading = ref(false)
const tableData = ref([])
const semesters = ref([])
const filters = ref({ keyword: '', status: '', semesterId: null })
const page = ref(1)
const pageSize = ref(20)

const statusOptions = [
  { label: '已导入', value: 'IMPORTED' },
  { label: '已锁定', value: 'LOCKED' }
]

const filteredData = computed(() => {
  return tableData.value.filter(r => {
    if (filters.value.keyword) {
      const kw = filters.value.keyword.toLowerCase()
      const matchName = r.courseName && r.courseName.toLowerCase().includes(kw)
      const matchClass = r.className && r.className.toLowerCase().includes(kw)
      const matchTeacher = r.teacherName && r.teacherName.toLowerCase().includes(kw)
      if (!matchName && !matchClass && !matchTeacher) return false
    }
    if (filters.value.status && r.status !== filters.value.status) return false
    if (filters.value.semesterId && r.semesterId !== filters.value.semesterId) return false
    return true
  })
})

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

function handlePageChange(p) { page.value = p }
function handleSizeChange(s) { pageSize.value = s; page.value = 1 }

// ===== 解锁工单 =====
const requestLoading = ref(false)
const requests = ref([])

// 成绩明细
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailHeaders = ref([])
const detailRows = ref([])
const detailClassName = ref('')

onMounted(async () => {
  await Promise.all([loadSheets(), loadSemesters(), loadRequests()])
})

async function loadSheets() {
  loading.value = true
  try {
    const res = await getAllScores()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadSemesters() {
  try {
    const res = await getSemesters()
    semesters.value = res.data || res || []
  } catch { /* ignore */ }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const res = await getUnlockRequests()
    requests.value = res.data || []
  } finally {
    requestLoading.value = false
  }
}

function handleSearch() {}
function handleReset() { filters.value = { keyword: '', status: '', semesterId: null } }

// 查看成绩
async function handleViewScores(row) {
  detailClassName.value = row.courseName ? `${row.courseName} — ${row.className}` : row.className
  detailVisible.value = true
  detailLoading.value = true
  detailHeaders.value = []
  detailRows.value = []
  try {
    const res = await getScoreDetails(row.classId)
    detailHeaders.value = res.data?.headers || []
    detailRows.value = res.data?.rows || []
  } finally {
    detailLoading.value = false
  }
}

// 解锁
async function handleUnlock(row) {
  await ElMessageBox.confirm(`确认解锁「${row.courseName || row.className}」的成绩单？教师将可以重新修改成绩。`, '解锁确认', { type: 'warning' })
  await unlockScore(row.id)
  ElMessage.success('解锁成功')
  loadSheets()
}

// 工单操作
async function handleApprove(row) {
  await ElMessageBox.confirm(`确认通过「${row.requesterName}」的勘误申请？`, '审批确认', { type: 'warning' })
  await approveUnlockRequest(row.id)
  ElMessage.success('审批通过')
  loadRequests()
}

async function handleDoUnlock(row) {
  await ElMessageBox.confirm(`确认解锁「${row.requesterName}」申请的成绩单？`, '解锁确认', { type: 'warning' })
  if (row.status === 'PENDING') {
    await approveUnlockRequest(row.id)
  }
  await unlockApprovedRequest(row.id)
  ElMessage.success('解锁成功')
  Promise.all([loadSheets(), loadRequests()])
}

async function handleReject(row) {
  await ElMessageBox.confirm(`确认驳回「${row.requesterName}」的勘误申请？`, '驳回确认', { type: 'warning' })
  await rejectUnlockRequest(row.id)
  ElMessage.success('已驳回')
  loadRequests()
}

function statusLabel(status) {
  const map = { IMPORTED: '已导入', LOCKED: '已锁定', COMPUTED: '已计算' }
  return map[status] || status
}

function statusType(status) {
  const map = { IMPORTED: 'warning', LOCKED: 'success', COMPUTED: '' }
  return map[status] || 'info'
}

function requestStatusLabel(status) {
  const map = { PENDING: '待审批', APPROVED: '已通过', UNLOCKED: '已解锁', REJECTED: '已驳回' }
  return map[status] || status
}

function requestStatusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'primary', UNLOCKED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #0891B2 0%, #06B6D4 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">成绩管理</h3>
              <p class="header-subtitle">管理成绩单状态和解锁工单审批</p>
            </div>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="custom-tabs">
        <!-- 成绩单列表 -->
        <el-tab-pane label="成绩单列表" name="sheets">
          <div class="filter-section">
            <el-form :inline="true" @submit.prevent="handleSearch" class="filter-form">
              <el-form-item label="关键词">
                <el-input v-model="filters.keyword" placeholder="课程/班级/教师" clearable class="filter-input">
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="filters.status" placeholder="全部" clearable class="filter-select-small">
                  <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="学期">
                <el-select v-model="filters.semesterId" placeholder="全部" clearable filterable class="filter-select">
                  <el-option v-for="s in semesters" :key="s.id" :label="s.semesterCode" :value="s.id" />
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

          <el-table :data="pagedData" v-loading="loading" stripe class="data-table">
            <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="course-name">{{ row.courseName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="className" label="教学班级" min-width="160" show-overflow-tooltip />
            <el-table-column prop="teacherName" label="主讲教师" min-width="110" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small" effect="dark" class="status-tag">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lockedAt" label="锁定时间" min-width="170">
              <template #default="{ row }">
                <span class="time-text">{{ row.lockedAt || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewScores(row)">
                  <el-icon><View /></el-icon>
                  查看成绩
                </el-button>
                <el-button link type="warning" :disabled="row.status !== 'LOCKED'" @click="handleUnlock(row)">
                  <el-icon><Unlock /></el-icon>
                  解锁
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="pageSize"
              :total="filteredData.length"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- 解锁工单审批 -->
        <el-tab-pane label="解锁工单审批" name="requests">
          <el-table :data="requests" v-loading="requestLoading" stripe class="data-table">
            <el-table-column prop="className" label="教学班级" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="class-name">{{ row.className }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="requesterName" label="申请人" min-width="100">
              <template #default="{ row }">
                <span class="requester-name">{{ row.requesterName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="requestStatusType(row.status)" size="small" effect="dark" class="status-tag">
                  {{ requestStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" min-width="170">
              <template #default="{ row }">
                <span class="time-text">{{ row.createdAt }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button link type="success" @click="handleDoUnlock(row)">
                    <el-icon><Unlock /></el-icon>
                    解锁
                  </el-button>
                  <el-button link type="danger" @click="handleReject(row)">
                    <el-icon><Close /></el-icon>
                    驳回
                  </el-button>
                </template>
                <span v-else class="processed-text">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 成绩明细弹窗 -->
    <el-dialog v-model="detailVisible" :title="`成绩明细 — ${detailClassName}`" width="90%" destroy-on-close class="custom-dialog detail-dialog">
      <el-table :data="detailRows" v-loading="detailLoading" stripe border class="detail-table" max-height="500">
        <el-table-column prop="studentNo" label="学号" width="120" fixed>
          <template #default="{ row }">
            <span class="student-no">{{ row.studentNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentName" label="姓名" width="100" fixed>
          <template #default="{ row }">
            <span class="student-name">{{ row.studentName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-for="h in detailHeaders"
          :key="h.id"
          :label="`${h.name}(${h.maxScore})`"
          min-width="110"
          align="center"
        >
          <template #header>
            <div class="column-header">
              <div class="column-name">{{ h.name }}</div>
              <div class="column-score">满分 {{ h.maxScore }}</div>
            </div>
          </template>
          <template #default="{ row }">
            <span class="score-value">{{ row.scores?.[h.id] ?? '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

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

/* ===== Tabs ===== */
.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
}

/* ===== Filter Section ===== */
.filter-section {
  background: #F8FAFC;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 20px;
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

.filter-input {
  width: 200px;
}

.filter-select {
  width: 170px;
}

.filter-select-small {
  width: 110px;
}

/* ===== Table Styles ===== */
.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.data-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.data-table :deep(.el-table__row:hover > td) {
  background: #F8FAFC !important;
}

.course-name {
  font-weight: 500;
  color: #1E293B;
}

.class-name {
  font-weight: 500;
  color: #1E293B;
}

.requester-name {
  font-weight: 500;
  color: #0891B2;
}

.status-tag {
  border-radius: 6px;
  font-weight: 600;
}

.time-text {
  font-size: 13px;
  color: #64748B;
}

.processed-text {
  color: #94A3B8;
  font-size: 13px;
}

/* ===== Detail Dialog ===== */
.detail-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.detail-table {
  border-radius: 8px;
  overflow: hidden;
}

.detail-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.student-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #0891B2;
}

.student-name {
  font-weight: 500;
  color: #1E293B;
}

.column-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.column-name {
  font-weight: 600;
  color: #1E293B;
}

.column-score {
  font-size: 11px;
  color: #64748B;
  font-weight: 400;
}

.score-value {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

/* ===== Pagination ===== */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* ===== Dialog Styles ===== */
.custom-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #F1F5F9;
  margin: 0;
}

.custom-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #1E293B;
}

.custom-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input,
  .filter-select,
  .filter-select-small {
    width: 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .data-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

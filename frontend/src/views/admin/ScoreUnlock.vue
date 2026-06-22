<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllScores, unlockScore, getScoreDetails, getUnlockRequests, approveUnlockRequest, unlockApprovedRequest, rejectUnlockRequest, getSemesters } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

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
    <el-card>
      <template #header><h3>成绩管理</h3></template>
      <el-tabs v-model="activeTab">

        <!-- 成绩单列表 -->
        <el-tab-pane label="成绩单列表" name="sheets">
          <el-form :inline="true" style="margin-bottom:16px" @submit.prevent="handleSearch">
            <el-form-item label="关键词">
              <el-input v-model="filters.keyword" placeholder="课程/班级/教师" clearable style="width:180px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="全部" clearable style="width:110px">
                <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="学期">
              <el-select v-model="filters.semesterId" placeholder="全部" clearable filterable style="width:170px">
                <el-option v-for="s in semesters" :key="s.id" :label="s.semesterCode" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="pagedData" v-loading="loading" stripe style="width:100%">
            <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="className" label="教学班级" min-width="160" show-overflow-tooltip />
            <el-table-column prop="teacherName" label="主讲教师" min-width="110" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lockedAt" label="锁定时间" min-width="170" />
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewScores(row)">查看成绩</el-button>
                <el-button link type="warning" :disabled="row.status !== 'LOCKED'" @click="handleUnlock(row)">解锁</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:16px">
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
          <el-table :data="requests" v-loading="requestLoading" stripe style="width:100%">
            <el-table-column prop="className" label="教学班级" min-width="160" show-overflow-tooltip />
            <el-table-column prop="requesterName" label="申请人" min-width="100" />
            <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="requestStatusType(row.status)" size="small">{{ requestStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" min-width="170" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button link type="success" @click="handleDoUnlock(row)">解锁</el-button>
                  <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
                </template>
                <span v-else style="color:#909399;font-size:13px">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

      </el-tabs>
    </el-card>

    <!-- 成绩明细弹窗 -->
    <el-dialog v-model="detailVisible" :title="`成绩明细 — ${detailClassName}`" width="90%" destroy-on-close>
      <el-table :data="detailRows" v-loading="detailLoading" stripe border style="width:100%" max-height="500">
        <el-table-column prop="studentNo" label="学号" width="120" fixed />
        <el-table-column prop="studentName" label="姓名" width="100" fixed />
        <el-table-column
          v-for="h in detailHeaders"
          :key="h.id"
          :label="`${h.name}(${h.maxScore})`"
          min-width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ row.scores?.[h.id] ?? '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

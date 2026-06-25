<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #EA580C 0%, #F97316 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">成绩录入与预览</h3>
              <p class="header-subtitle">导入学生考核成绩，支持 Excel 批量上传</p>
            </div>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" class="class-selector">
              <el-option v-for="c in myClasses" :key="c.id" :label="`${c.courseName} - ${c.className}`" :value="c.id" />
            </el-select>
            <el-tag
              v-if="scoreStatus"
              :type="statusTagType"
              size="default"
              class="status-tag"
              effect="dark"
            >
              <el-icon class="status-icon"><component :is="statusIcon" /></el-icon>
              {{ statusLabel }}
            </el-tag>
          </div>
        </div>
      </template>

      <div class="action-bar">
        <el-button :disabled="!selectedClassId" :loading="downloading" @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          下载模板
        </el-button>
        <el-upload :show-file-list="false" accept=".xlsx,.xls" :before-upload="handleUpload" :auto-upload="false" :on-change="handleUpload">
          <el-button type="primary" :disabled="!selectedClassId || scoreStatus === 'LOCKED'" :loading="uploading">
            <el-icon><Upload /></el-icon>
            上传成绩
          </el-button>
        </el-upload>
        <el-button :disabled="!selectedClassId" :loading="loadingScores" @click="loadScores">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <el-alert
        v-if="scoreStatus === 'LOCKED'"
        title="成绩已锁定，无法修改。如需勘误请联系管理员解锁。"
        type="warning"
        show-icon
        :closable="false"
        class="locked-alert"
      />

      <div v-if="previewData" class="data-grid-wrapper" @mouseleave="clearCrosshair">
        <el-table
          :data="previewData.rows" v-loading="loadingScores" stripe border size="small"
          max-height="500" style="width:100%"
          :row-class-name="rowClassName"
          @cell-mouse-enter="onCellEnter" @cell-mouse-leave="onCellLeave"
          class="score-table"
        >
          <el-table-column prop="studentNo" label="学号" width="120" fixed :class-name="colClassName(0)">
            <template #default="{ row }">
              <span class="student-no">{{ row.studentNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="studentName" label="姓名" width="100" fixed :class-name="colClassName(1)">
            <template #default="{ row }">
              <span class="student-name">{{ row.studentName }}</span>
            </template>
          </el-table-column>
          <el-table-column v-for="(h, idx) in previewData.headers" :key="h.id"
            :label="`${h.name} (${h.maxScore})`" min-width="110" align="center"
            :class-name="colClassName(idx + 2)">
            <template #header>
              <div class="column-header">
                <div class="column-name">{{ h.name }}</div>
                <div class="column-score">满分 {{ h.maxScore }}</div>
              </div>
            </template>
            <template #default="{ row }">
              <span
                :class="['score-cell', { 'cell-null': getCellScore(row, h.id) === null, 'cell-valid': getCellScore(row, h.id) !== null }]"
              >
                {{ getCellScore(row, h.id) ?? '缺失' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else class="empty-state">
        <el-empty description="请先选择教学班级并上传成绩" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Download, Upload, Refresh, CircleCheck, Clock, Warning } from '@element-plus/icons-vue'
import { getMyClasses, downloadScoreTemplate, uploadScores, getScores, getScoreStatus } from '@/api/teacher'
import { ElMessage } from 'element-plus'

const myClasses = ref([])
const selectedClassId = ref(null)
const loadingScores = ref(false)
const downloading = ref(false)
const uploading = ref(false)
const previewData = ref(null)
const scoreStatus = ref('')
const crossRow = ref(-1)
const crossCol = ref(-1)

function onCellEnter(row, col) { crossRow.value = previewData.value.rows.indexOf(row); crossCol.value = col.getColIndex() }
function onCellLeave() { crossRow.value = -1; crossCol.value = -1 }
function clearCrosshair() { crossRow.value = -1; crossCol.value = -1 }
function rowClassName({rowIndex}) { return rowIndex === crossRow.value ? 'cross-row' : '' }
function colClassName(idx) { return idx === crossCol.value ? 'cross-col' : '' }

const statusTagType = computed(() => {
  const map = { EMPTY: 'info', IMPORTED: 'warning', LOCKED: 'success' }
  return map[scoreStatus.value] || 'info'
})

const statusIcon = computed(() => {
  const map = { EMPTY: Clock, IMPORTED: Warning, LOCKED: CircleCheck }
  return map[scoreStatus.value] || Clock
})

const statusLabel = computed(() => {
  const map = { EMPTY: '未录入', IMPORTED: '已导入', LOCKED: '已锁定' }
  return map[scoreStatus.value] || scoreStatus.value
})

onMounted(async () => {
  const route = useRoute()
  const res = await getMyClasses()
  myClasses.value = res.data || res || []
  if (myClasses.value.length) {
    const cid = parseInt(route.params.courseId)
    const match = cid ? myClasses.value.find(c => c.id === cid || c.courseId === cid) : null
    selectedClassId.value = match ? match.id : myClasses.value[0].id
  }
})

watch(selectedClassId, (v) => { if (v) loadScores() })

function getCellScore(row, assessmentId) {
  const cell = row.cells?.find(c => c.assessmentId === assessmentId)
  if (!cell) return null
  let val = null
  if (cell.score !== null && cell.score !== undefined) {
    val = cell.score
  } else if (cell.questionScores) {
    const values = Object.values(cell.questionScores).filter(v => v !== null && v !== undefined)
    if (values.length > 0) val = values.reduce((a, b) => a + b, 0)
  }
  if (val !== null && val !== undefined) return Number(val).toFixed(2)
  return null
}

async function loadScores() {
  if (!selectedClassId.value) return
  loadingScores.value = true
  try {
    const [scores, status] = await Promise.all([
      getScores(selectedClassId.value),
      getScoreStatus(selectedClassId.value)
    ])
    previewData.value = scores?.data || scores
    scoreStatus.value = status?.data?.status || status?.status || scores?.data?.status || scores?.status || 'EMPTY'
    ElMessage.success('刷新成功')
  } catch {
    previewData.value = null
    scoreStatus.value = ''
  } finally {
    loadingScores.value = false
  }
}

async function handleDownloadTemplate() {
  downloading.value = true
  try {
    const blob = await downloadScoreTemplate(selectedClassId.value)
    if (!blob || blob.size < 200) { ElMessage.warning('模板生成失败，请确认已配置考核点'); return }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const name = myClasses.value.find(c => c.id === selectedClassId.value)?.className || '成绩录入模板'
    a.download = name + '.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch { /* handled */ } finally {
    downloading.value = false
  }
}

async function handleUpload(file) {
  uploading.value = true
  try {
    await uploadScores(selectedClassId.value, file.raw || file)
    ElMessage.success('成绩上传成功')
    await loadScores()
  } catch { /* handled */ } finally {
    uploading.value = false
  }
  return false
}
</script>

<style>
/* Crosshair - deep styles needed to override el-table internals */
.data-grid-wrapper .el-table .cross-row > td { background-color: #ECFDF5 !important; }
.data-grid-wrapper .el-table .cross-col { background-color: #ECFDF5 !important; }
.data-grid-wrapper .el-table th.cross-col { background-color: #D1FAE5 !important; }
.data-grid-wrapper .el-table .cross-row > td.cross-col { background-color: #A7F3D0 !important; }
.data-grid-wrapper .el-table .el-table__fixed .cross-row > td { background-color: #ECFDF5 !important; }
.data-grid-wrapper .el-table .el-table__fixed .cross-col { background-color: #ECFDF5 !important; }
</style>

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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.class-selector {
  width: 280px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
}

.status-icon {
  font-size: 16px;
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

/* ===== Action Bar ===== */
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

/* ===== Alert ===== */
.locked-alert {
  margin-bottom: 20px;
  border-radius: 8px;
}

/* ===== Table Styles ===== */
.data-grid-wrapper {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #E2E8F0;
}

.score-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.score-table :deep(.el-table__row:hover > td) {
  background: #F8FAFC !important;
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

.student-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 500;
  color: #475569;
}

.student-name {
  font-weight: 500;
  color: #1E293B;
}

.score-cell {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.cell-valid {
  color: #059669;
}

.cell-null {
  color: #F59E0B;
  font-style: italic;
  font-weight: 400;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 60px 0;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .class-selector {
    width: 100%;
  }

  .status-tag {
    justify-content: center;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .data-grid-wrapper .el-table .cross-row > td,
  .data-grid-wrapper .el-table .cross-col {
    transition: none;
  }
}
</style>

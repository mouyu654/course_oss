<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px">
          <h3 style="margin:0;font-size:16px">成绩录入与预览</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" style="width:280px">
              <el-option v-for="c in myClasses" :key="c.id" :label="`${c.courseName} - ${c.className}`" :value="c.id" />
            </el-select>
            <el-tag v-if="scoreStatus" :type="statusTagType" size="default">{{ statusLabel }}</el-tag>
          </div>
        </div>
      </template>

      <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
        <el-button :icon="Download" :disabled="!selectedClassId" :loading="downloading" @click="handleDownloadTemplate">下载模板</el-button>
        <el-upload :show-file-list="false" accept=".xlsx,.xls" :before-upload="handleUpload" :auto-upload="false" :on-change="handleUpload">
          <el-button type="primary" :icon="Upload" :disabled="!selectedClassId || scoreStatus === 'LOCKED'" :loading="uploading">上传成绩</el-button>
        </el-upload>
        <el-button :icon="Refresh" :disabled="!selectedClassId" :loading="loadingScores" @click="loadScores">刷新</el-button>
      </div>

      <el-alert v-if="scoreStatus === 'LOCKED'" title="成绩已锁定，无法修改。如需勘误请联系管理员解锁。" type="warning" show-icon :closable="false" style="margin-bottom:16px" />

      <div v-if="previewData" class="data-grid-wrapper" @mouseleave="clearCrosshair">
        <el-table
          :data="previewData.rows" v-loading="loadingScores" stripe border size="small"
          max-height="500" style="width:100%"
          :row-class-name="rowClassName"
          @cell-mouse-enter="onCellEnter" @cell-mouse-leave="onCellLeave"
        >
          <el-table-column prop="studentNo" label="学号" width="120" fixed :class-name="colClassName(0)" />
          <el-table-column prop="studentName" label="姓名" width="100" fixed :class-name="colClassName(1)" />
          <el-table-column v-for="(h, idx) in previewData.headers" :key="h.id"
            :label="`${h.name} (${h.maxScore})`" min-width="110" align="center"
            :class-name="colClassName(idx + 2)">
            <template #default="{ row }">
              <span :class="{ 'cell-null': getCellScore(row, h.id) === null }">
                {{ getCellScore(row, h.id) ?? '缺失' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-if="!previewData && !loadingScores" description="请先选择教学班级并上传成绩" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Download, Upload, Refresh } from '@element-plus/icons-vue'
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
.data-grid-wrapper .el-table .cross-row > td { background-color: #ecf5ff !important; }
.data-grid-wrapper .el-table .cross-col { background-color: #ecf5ff !important; }
.data-grid-wrapper .el-table th.cross-col { background-color: #d9ecff !important; }
.data-grid-wrapper .el-table .cross-row > td.cross-col { background-color: #c6e2ff !important; }
.data-grid-wrapper .el-table .el-table__fixed .cross-row > td { background-color: #ecf5ff !important; }
.data-grid-wrapper .el-table .el-table__fixed .cross-col { background-color: #ecf5ff !important; }
</style>
<style scoped>
.cell-null { color: #e6a23c; font-style: italic; }
.data-grid-wrapper :deep(.el-table__header-wrapper th) { position: sticky; top: 0; z-index: 3; background: #f5f7fa; }
</style>

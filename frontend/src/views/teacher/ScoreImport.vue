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

      <div v-if="previewData" style="overflow-x:auto">
        <el-table :data="previewData.rows" v-loading="loadingScores" stripe border size="small" max-height="500" style="width:100%">
          <el-table-column prop="studentNo" label="学号" width="120" fixed />
          <el-table-column prop="studentName" label="姓名" width="100" fixed />
          <el-table-column v-for="h in previewData.headers" :key="h.id" :label="`${h.name} (${h.maxScore})`" min-width="110" align="center">
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

const statusTagType = computed(() => {
  const map = { EMPTY: 'info', IMPORTED: 'warning', LOCKED: 'success' }
  return map[scoreStatus.value] || 'info'
})
const statusLabel = computed(() => {
  const map = { EMPTY: '未录入', IMPORTED: '已导入', LOCKED: '已锁定' }
  return map[scoreStatus.value] || scoreStatus.value
})

onMounted(async () => {
  const res = await getMyClasses()
  myClasses.value = res.data || res || []
  if (myClasses.value.length) selectedClassId.value = myClasses.value[0].id
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

<style scoped>
.cell-null { color: #e6a23c; font-style: italic; }
</style>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import {
  getMyClasses,
  getAssessments,
  createAssessment,
  updateAssessment,
  deleteAssessment,
  getObjectives,
  downloadAssessmentTemplate,
  importAssessments
} from '@/api/teacher'
import { ElMessage, ElMessageBox } from 'element-plus'

/* ---- state ---- */
const myClasses = ref([])
const selectedClassId = ref(null)
const loading = ref(false)
const assessments = ref([])
const objectives = ref([])
const importing = ref(false)
const fileInput = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', maxScore: null, objectiveIds: [], sortOrder: 0 })

const formRules = {
  name: [{ required: true, message: '请输入考核点名称', trigger: 'blur' }],
  maxScore: [{ required: true, type: 'number', min: 0.01, message: '满分必须大于 0', trigger: 'blur' }],
  objectiveIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个课程目标', trigger: 'change' }],
  sortOrder: [{ required: true, type: 'number', min: 0, message: '排序号不能为负', trigger: 'blur' }]
}

/* ---- lifecycle ---- */
onMounted(async () => {
  const res = await getMyClasses()
  myClasses.value = res.data || []
  if (myClasses.value.length) {
    selectedClassId.value = myClasses.value[0].id
  }
})

watch(selectedClassId, (v) => { if (v) loadData() })

/* ---- data loading ---- */
async function loadData() {
  if (!selectedClassId.value) return
  loading.value = true
  try {
    const [assessRes, objRes] = await Promise.all([
      getAssessments(selectedClassId.value),
      getObjectives(selectedClassId.value)
    ])
    assessments.value = (assessRes.data || []).sort((a, b) => a.sortOrder - b.sortOrder)
    objectives.value = objRes.data || []
  } finally {
    loading.value = false
  }
}

/* ---- helpers ---- */
function objectiveLabels(row) {
  const ids = (row.objectiveIds && row.objectiveIds.length > 0)
    ? row.objectiveIds
    : (row.objectiveId ? [row.objectiveId] : [])
  return ids.map(id => {
    const obj = objectives.value.find(o => o.id === id)
    return obj ? obj.objNo : '-'
  }).join(', ')
}

/* ---- dialog controls ---- */
function handleAdd() {
  isEdit.value = false
  form.value = { name: '', maxScore: null, objectiveIds: [], sortOrder: assessments.value.length }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEdit(row) {
  isEdit.value = true
  const ids = (row.objectiveIds && row.objectiveIds.length > 0)
    ? [...row.objectiveIds]
    : (row.objectiveId ? [row.objectiveId] : [])
  form.value = { id: row.id, name: row.name, maxScore: row.maxScore, objectiveIds: ids, sortOrder: row.sortOrder }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该考核点？', '提示', { type: 'warning' })
  await deleteAssessment(selectedClassId.value, row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  const payload = {
    name: form.value.name,
    maxScore: form.value.maxScore,
    objectiveIds: form.value.objectiveIds,
    sortOrder: form.value.sortOrder
  }
  if (isEdit.value) {
    await updateAssessment(selectedClassId.value, form.value.id, payload)
    ElMessage.success('修改成功')
  } else {
    await createAssessment(selectedClassId.value, payload)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

/* ---- 批量导入 ---- */
function handleDownloadTemplate() {
  if (!selectedClassId.value) return
  downloadAssessmentTemplate(selectedClassId.value).then(blob => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '考核点导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  })
}

function handleImportClick() {
  if (!selectedClassId.value) return
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  importing.value = true
  try {
    const res = await importAssessments(selectedClassId.value, file)
    const count = res?.data ?? res ?? 0
    ElMessage.success(`成功导入 ${count} 条考核点`)
    loadData()
  } catch (err) {
    // error handled by interceptor
  } finally {
    importing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #059669 0%, #10B981 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">考核点细分与映射</h3>
              <p class="header-subtitle">定义考核点并建立与课程目标的对应关系</p>
            </div>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" class="class-selector">
              <el-option
                v-for="c in myClasses"
                :key="c.id"
                :label="`${c.courseName} - ${c.className}`"
                :value="c.id"
              />
            </el-select>
            <div class="action-buttons">
              <el-button type="primary" :disabled="!selectedClassId" @click="handleAdd">
                <el-icon><Plus /></el-icon>
                新增考核点
              </el-button>
              <el-button :disabled="!selectedClassId" @click="handleDownloadTemplate">
                <el-icon><Download /></el-icon>
                下载模板
              </el-button>
              <el-button type="success" :disabled="!selectedClassId" :loading="importing" @click="handleImportClick">
                <el-icon><Upload /></el-icon>
                批量导入
              </el-button>
              <input ref="fileInput" type="file" accept=".xlsx" style="display:none" @change="handleFileChange" />
            </div>
          </div>
        </div>
      </template>

      <el-table :data="assessments" v-loading="loading" stripe class="data-table">
        <el-table-column prop="sortOrder" label="序号" width="80" align="center">
          <template #default="{ row }">
            <span class="sort-number">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="考核点名称" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="assessment-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="maxScore" label="满分" width="100" align="center">
          <template #default="{ row }">
            <span class="score-value">{{ row.maxScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="绑定课程目标" width="140" align="center">
          <template #default="{ row }">
            <div class="objective-tags">
              <el-tag
                v-for="label in objectiveLabels(row).split(', ')"
                :key="label"
                size="small"
                type="info"
                effect="plain"
                class="objective-tag"
              >
                {{ label }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- add / edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑考核点' : '新增考核点'"
      width="500px"
      destroy-on-close
      class="custom-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 期末大题1" maxlength="100" />
        </el-form-item>
        <el-form-item label="满分分值" prop="maxScore">
          <el-input-number v-model="form.maxScore" :min="0.01" :max="1000" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="绑定目标" prop="objectiveIds">
          <el-select v-model="form.objectiveIds" multiple placeholder="可多选课程目标" style="width: 100%;">
            <el-option
              v-for="obj in objectives"
              :key="obj.id"
              :label="`${obj.objNo} ${obj.description}`"
              :value="obj.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.class-selector {
  width: 280px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.sort-number {
  font-weight: 600;
  color: #64748B;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.assessment-name {
  font-weight: 500;
  color: #1E293B;
}

.score-value {
  font-weight: 600;
  color: #059669;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.objective-tags {
  display: flex;
  gap: 4px;
  justify-content: center;
  flex-wrap: wrap;
}

.objective-tag {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 500;
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

.custom-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #F1F5F9;
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

  .action-buttons {
    justify-content: flex-start;
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

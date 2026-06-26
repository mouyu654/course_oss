<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { getMyClasses, getObjectives, createObjective, updateObjective, deleteObjective, downloadObjectiveTemplate, importObjectives } from '@/api/teacher'
import { ElMessage, ElMessageBox } from 'element-plus'

/* ---- state ---- */
const myClasses = ref([])
const selectedClassId = ref(null)
const loading = ref(false)
const objectives = ref([])
const importing = ref(false)
const fileInput = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ objNo: '', dimension: '', description: '' })

const formRules = {
  objNo: [{ required: true, message: '请输入目标编号', trigger: 'blur' }],
  dimension: [{ required: true, message: '请选择维度', trigger: 'change' }],
  description: [{ required: true, message: '请输入目标描述', trigger: 'blur' }]
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
    const res = await getObjectives(selectedClassId.value)
    objectives.value = res.data || []
  } finally {
    loading.value = false
  }
}

/* ---- dialog controls ---- */
function handleAdd() {
  isEdit.value = false
  form.value = { objNo: '', dimension: '', description: '' }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该课程目标？', '提示', { type: 'warning' })
  await deleteObjective(selectedClassId.value, row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (isEdit.value) {
    await updateObjective(selectedClassId.value, form.value.id, {
      objNo: form.value.objNo,
      dimension: form.value.dimension,
      description: form.value.description
    })
    ElMessage.success('修改成功')
  } else {
    await createObjective(selectedClassId.value, {
      objNo: form.value.objNo,
      dimension: form.value.dimension,
      description: form.value.description
    })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

/* ---- 批量导入 ---- */
function handleDownloadTemplate() {
  if (!selectedClassId.value) return
  downloadObjectiveTemplate(selectedClassId.value).then(blob => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '课程目标导入模板.xlsx'
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
    const res = await importObjectives(selectedClassId.value, file)
    const count = res?.data ?? res ?? 0
    ElMessage.success(`成功导入 ${count} 条课程目标`)
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
            <div class="header-accent"></div>
            <div class="header-content">
              <h3 class="header-title">课程目标设定</h3>
              <p class="header-subtitle">设定课程目标，建立知识、能力、价值三维度评估体系</p>
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
                新增目标
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

      <el-table :data="objectives" v-loading="loading" stripe class="data-table">
        <el-table-column prop="objNo" label="编号" width="100" align="center">
          <template #default="{ row }">
            <span class="obj-number">{{ row.objNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dimension" label="维度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDimensionType(row.dimension)" effect="plain" size="small">
              {{ row.dimension }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="目标描述" show-overflow-tooltip />
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
      :title="isEdit ? '编辑课程目标' : '新增课程目标'"
      width="520px"
      destroy-on-close
      class="custom-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="编号" prop="objNo">
          <el-input v-model="form.objNo" placeholder="如 1-1" maxlength="20" />
        </el-form-item>
        <el-form-item label="维度" prop="dimension">
          <el-select v-model="form.dimension" placeholder="选择维度" style="width: 100%;">
            <el-option label="知识" value="知识" />
            <el-option label="能力" value="能力" />
            <el-option label="价值" value="价值" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程目标描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
export default {
  methods: {
    getDimensionType(dimension) {
      const map = {
        '知识': '',
        '能力': 'success',
        '价值': 'warning'
      }
      return map[dimension] || 'info'
    }
  }
}
</script>

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
  background: linear-gradient(180deg, #2563EB 0%, #3B82F6 100%);
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

.obj-number {
  font-weight: 600;
  color: #2563EB;
  font-family: 'SF Mono', 'Consolas', monospace;
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
  .obj-number,
  .data-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

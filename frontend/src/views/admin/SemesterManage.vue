<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSemesters, createSemester, updateSemester, deleteSemester } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const searchCode = ref('')
const searchYear = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({ academicYear: '', semester: 1, semesterCode: '', startDate: '', endDate: '' })

const rules = {
  semesterCode: [{ required: true, message: '请输入学期编码', trigger: 'blur' }],
  academicYear: [{ required: true, message: '请输入学年', trigger: 'blur' }]
}

const codeOptions = computed(() => [...new Set(tableData.value.map(r => r.semesterCode).filter(Boolean))])
const yearOptions = computed(() => [...new Set(tableData.value.map(r => r.academicYear).filter(Boolean))])

const filteredData = computed(() => {
  return tableData.value.filter(r => {
    if (searchCode.value && r.semesterCode !== searchCode.value) return false
    if (searchYear.value && r.academicYear !== searchYear.value) return false
    return true
  })
})

onMounted(() => { loadData() })

async function loadData() {
  loading.value = true
  try {
    const res = await getSemesters()
    tableData.value = res.data || res || []
  } finally {
    loading.value = false
  }
}

function handleSearch() {}
function handleReset() { searchCode.value = ''; searchYear.value = '' }

function handleAdd() {
  isEdit.value = false
  form.value = { academicYear: '', semester: 1, semesterCode: '', startDate: '', endDate: '' }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该学年学期？', '提示', { type: 'warning' })
  await deleteSemester(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSemester(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createSemester(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #2563EB 0%, #3B82F6 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">学期管理</h3>
              <p class="header-subtitle">管理学年学期信息</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="search-box">
              <el-select v-model="searchCode" placeholder="学期编码" clearable class="filter-select">
                <el-option v-for="c in codeOptions" :key="c" :label="c" :value="c" />
              </el-select>
              <el-select v-model="searchYear" placeholder="学年" clearable class="filter-select">
                <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
              </el-select>
              <el-button @click="handleReset">
                <el-icon><RefreshRight /></el-icon>
                重置
              </el-button>
            </div>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增学期
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="semesterCode" label="学期编码" min-width="160">
          <template #default="{ row }">
            <span class="code-text">{{ row.semesterCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="academicYear" label="学年" min-width="130">
          <template #default="{ row }">
            <span class="year-text">{{ row.academicYear }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.semester === 1 ? '' : 'success'" size="small" effect="dark" class="semester-tag">
              {{ row.semester === 1 ? '第一学期' : '第二学期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" min-width="130">
          <template #default="{ row }">
            <span class="date-text">{{ row.startDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束日期" min-width="130">
          <template #default="{ row }">
            <span class="date-text">{{ row.endDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              修改
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改学期' : '新增学期'" width="500px" destroy-on-close class="custom-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学期编码" prop="semesterCode">
          <el-input v-model="form.semesterCode" placeholder="如 2025-2026-1" />
        </el-form-item>
        <el-form-item label="学年" prop="academicYear">
          <el-input v-model="form.academicYear" placeholder="如 2025-2026" />
        </el-form-item>
        <el-form-item label="学期">
          <el-radio-group v-model="form.semester" class="semester-radio">
            <el-radio :value="1">第一学期</el-radio>
            <el-radio :value="2">第二学期</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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

.search-box {
  display: flex;
  gap: 8px;
}

.filter-select {
  width: 150px;
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

.code-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #2563EB;
}

.year-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #475569;
}

.semester-tag {
  border-radius: 6px;
  font-weight: 600;
}

.date-text {
  font-size: 13px;
  color: #64748B;
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

.semester-radio {
  display: flex;
  gap: 24px;
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

  .search-box {
    flex-wrap: wrap;
  }

  .filter-select {
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

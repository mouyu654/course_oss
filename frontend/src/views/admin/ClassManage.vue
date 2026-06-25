<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminClasses, createAdminClass, updateAdminClass, deleteAdminClass, getMajors, getAdminClassStudents, addStudentToAdminClass, removeStudentFromAdminClass } from '@/api/admin'
import { getStudents } from '@/api/academic'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const majors = ref([])
const searchClassName = ref('')
const searchMajor = ref('')
const searchYear = ref('')
const page = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({ majorId: null, className: '', enrollmentYear: new Date().getFullYear() })

const rules = {
  majorId: [{ required: true, message: '请选择所属专业', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  enrollmentYear: [{ required: true, message: '请输入入学年份', trigger: 'blur' }]
}

const yearOptions = computed(() => [...new Set(tableData.value.map(r => r.enrollmentYear).filter(Boolean))].sort((a, b) => b - a))

const filteredData = computed(() => {
  return tableData.value.filter(r => {
    if (searchClassName.value && !(r.className && r.className.toLowerCase().includes(searchClassName.value.toLowerCase()))) return false
    if (searchMajor.value && r.majorName !== searchMajor.value) return false
    if (searchYear.value && r.enrollmentYear !== searchYear.value) return false
    return true
  })
})

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

function handlePageChange(p) { page.value = p }
function handleSizeChange(s) { pageSize.value = s; page.value = 1 }

// 学生管理弹窗
const studentDialogVisible = ref(false)
const currentClass = ref(null)
const classStudents = ref([])
const studentLoading = ref(false)
const allStudents = ref([])
const addStudentId = ref(null)

onMounted(() => {
  loadData()
  loadMajors()
  loadAllStudents()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminClasses({ size: 999 })
    tableData.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

async function loadMajors() {
  try {
    const res = await getMajors()
    const data = res.data || res
    majors.value = Array.isArray(data) ? data : data?.records || []
  } catch { /* ignore */ }
}

async function loadAllStudents() {
  try {
    const res = await getStudents({ size: 9999 })
    allStudents.value = res.data?.records || res.data || []
  } catch { /* ignore */ }
}

function handleSearch() {}
function handleReset() { searchClassName.value = ''; searchMajor.value = ''; searchYear.value = '' }

function handleAdd() {
  isEdit.value = false
  form.value = { majorId: null, className: '', enrollmentYear: new Date().getFullYear() }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除班级「${row.className}」？`, '提示', { type: 'warning' })
  await deleteAdminClass(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAdminClass(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createAdminClass(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ===== 学生管理 =====
async function handleManageStudents(row) {
  currentClass.value = row
  studentDialogVisible.value = true
  addStudentId.value = null
  await loadClassStudents()
}

async function loadClassStudents() {
  if (!currentClass.value) return
  studentLoading.value = true
  try {
    const res = await getAdminClassStudents(currentClass.value.id)
    classStudents.value = res.data || []
  } finally {
    studentLoading.value = false
  }
}

async function handleAddStudent() {
  if (!addStudentId.value) {
    ElMessage.warning('请先选择要添加的学生')
    return
  }
  await addStudentToAdminClass(currentClass.value.id, addStudentId.value)
  ElMessage.success('添加成功')
  addStudentId.value = null
  await loadClassStudents()
}

async function handleRemoveStudent(student) {
  await ElMessageBox.confirm(`确认将「${student.name}」从该行政班级移除？`, '提示', { type: 'warning' })
  await removeStudentFromAdminClass(currentClass.value.id, student.id)
  ElMessage.success('移除成功')
  await loadClassStudents()
}

const availableStudents = ref([])
function onAddSelect(query) {
  const existIds = new Set(classStudents.value.map(s => s.id))
  availableStudents.value = allStudents.value.filter(s => {
    if (existIds.has(s.id)) return false
    if (!query) return true
    return (s.name && s.name.includes(query)) || (s.studentNo && s.studentNo.includes(query))
  })
}

defineOptions({ name: 'ClassManage' })
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #EA580C 0%, #F97316 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">班级管理</h3>
              <p class="header-subtitle">管理行政班级信息</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="search-box">
              <el-input v-model="searchClassName" placeholder="班级名称" clearable class="filter-input" @keyup.enter="handleSearch">
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select v-model="searchMajor" placeholder="所属专业" clearable class="filter-select">
                <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.name" />
              </el-select>
              <el-select v-model="searchYear" placeholder="入学年份" clearable class="filter-select-small">
                <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
              </el-select>
              <el-button @click="handleReset">
                <el-icon><RefreshRight /></el-icon>
                重置
              </el-button>
            </div>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增班级
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="pagedData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="className" label="班级名称" min-width="200">
          <template #default="{ row }">
            <span class="class-name">{{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="majorName" label="所属专业" min-width="180">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ row.majorName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enrollmentYear" label="入学年份" width="100" align="center">
          <template #default="{ row }">
            <span class="year-text">{{ row.enrollmentYear }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学生人数" width="100" align="center">
          <template #default="{ row }">
            <span class="count-text">{{ row.studentCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              修改
            </el-button>
            <el-button link type="success" @click="handleManageStudents(row)">
              <el-icon><User /></el-icon>
              学生管理
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
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
    </el-card>

    <!-- 班级编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '新增班级'" width="480px" destroy-on-close class="custom-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请选择专业" style="width:100%">
            <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如 计算机科学与技术2501班" />
        </el-form-item>
        <el-form-item label="入学年份" prop="enrollmentYear">
          <el-input-number v-model="form.enrollmentYear" :min="2000" :max="2030" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 学生管理弹窗 -->
    <el-dialog
      v-model="studentDialogVisible"
      :title="`学生管理 — ${currentClass?.className || ''}`"
      width="680px"
      destroy-on-close
      class="custom-dialog"
    >
      <div class="student-manage-header">
        <el-select
          v-model="addStudentId"
          filterable
          remote
          placeholder="输入学号或姓名搜索学生"
          :remote-method="onAddSelect"
          @focus="onAddSelect('')"
          class="student-search"
        >
          <el-option
            v-for="s in availableStudents"
            :key="s.id"
            :label="`${s.studentNo} - ${s.name}`"
            :value="s.id"
          />
        </el-select>
        <el-button type="primary" @click="handleAddStudent">
          <el-icon><Plus /></el-icon>
          添加
        </el-button>
      </div>

      <el-table :data="classStudents" v-loading="studentLoading" stripe class="student-table" max-height="400">
        <el-table-column prop="studentNo" label="学号" min-width="130">
          <template #default="{ row }">
            <span class="student-no">{{ row.studentNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="100">
          <template #default="{ row }">
            <span class="student-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleRemoveStudent(row)">
              <el-icon><Delete /></el-icon>
              移除
            </el-button>
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.search-box {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-input {
  width: 160px;
}

.filter-select {
  width: 170px;
}

.filter-select-small {
  width: 120px;
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

.class-name {
  font-weight: 500;
  color: #1E293B;
}

.year-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #475569;
}

.count-text {
  font-weight: 600;
  color: #EA580C;
  font-family: 'SF Mono', 'Consolas', monospace;
}

/* ===== Student Table ===== */
.student-manage-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.student-search {
  flex: 1;
}

.student-table {
  border-radius: 8px;
  overflow: hidden;
}

.student-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.student-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #EA580C;
}

.student-name {
  font-weight: 500;
  color: #1E293B;
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

  .search-box {
    flex-wrap: wrap;
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

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getTeachingClasses, createTeachingClass, updateTeachingClass, deleteTeachingClass,
  getTeachingClassStudents, addTeachingClassStudent, removeTeachingClassStudent, getCourses, getStudents
} from '@/api/academic'
import { getUsers, getRoles, getSemesters } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const courses = ref([])
const teachers = ref([])
const semesters = ref([])

// 查询条件
const filters = ref({ keyword: '', courseId: null, teacherId: null, semesterId: null })

// 教学班级弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)

const defaultForm = () => ({
  className: '',
  courseId: null,
  teacherId: null,
  semesterId: null
})

const form = reactive(defaultForm())

const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  teacherId: [{ required: true, message: '请选择教师', trigger: 'change' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }]
}

// 学生管理弹窗
const studentDialogVisible = ref(false)
const currentClass = ref(null)
const classStudents = ref([])
const studentLoading = ref(false)
const allStudents = ref([])
const addStudentId = ref(null)

onMounted(() => {
  loadData()
  loadCourses()
  loadTeachers()
  loadSemesters()
  loadAllStudents()
})

async function loadCourses() {
  try {
    const res = await getCourses({ size: 999 })
    courses.value = res.data?.records || res.data || []
  } catch { /* handled by interceptor */ }
}

async function loadTeachers() {
  try {
    const rolesRes = await getRoles()
    const roles = rolesRes.data || rolesRes || []
    const teacherRole = roles.find(r => r.roleCode === 'TEACHER')
    if (!teacherRole) return
    const res = await getUsers({ roleId: teacherRole.id, size: 999 })
    teachers.value = res.data?.records || res.data || []
  } catch { /* handled by interceptor */ }
}

async function loadSemesters() {
  try {
    const res = await getSemesters()
    semesters.value = res.data || []
  } catch { /* handled by interceptor */ }
}

async function loadAllStudents() {
  try {
    const res = await getStudents({ size: 9999 })
    allStudents.value = res.data?.records || res.data || []
  } catch { /* handled by interceptor */ }
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value, ...filters.value }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === null) delete params[k] })
    const res = await getTeachingClasses(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() { filters.value = { keyword: '', courseId: null, teacherId: null, semesterId: null }; handleSearch() }

function handlePageChange(newPage) {
  page.value = newPage
  loadData()
}

function handleSizeChange(newSize) {
  pageSize.value = newSize
  page.value = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    className: row.className,
    courseId: row.courseId,
    teacherId: row.teacherId,
    semesterId: row.semesterId
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除教学班级「${row.className}」？`, '提示', { type: 'warning' })
  await deleteTeachingClass(row.id)
  ElMessage.success('删除成功')
  if (tableData.value.length === 1 && page.value > 1) {
    page.value--
  }
  loadData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      className: form.className,
      courseId: form.courseId,
      teacherId: form.teacherId,
      semesterId: form.semesterId
    }
    if (isEdit.value) {
      await updateTeachingClass(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createTeachingClass(payload)
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
    const res = await getTeachingClassStudents(currentClass.value.id)
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
  await addTeachingClassStudent(currentClass.value.id, addStudentId.value)
  ElMessage.success('添加成功')
  addStudentId.value = null
  await loadClassStudents()
}

async function handleRemoveStudent(student) {
  await ElMessageBox.confirm(`确认将「${student.name}」从该教学班级移除？`, '提示', { type: 'warning' })
  await removeTeachingClassStudent(currentClass.value.id, student.id)
  ElMessage.success('移除成功')
  await loadClassStudents()
}

// 可添加的学生列表（排除已在班级中的）
const availableStudents = ref([])
function onAddSelect(query) {
  const existIds = new Set(classStudents.value.map(s => s.id))
  availableStudents.value = allStudents.value.filter(s => {
    if (existIds.has(s.id)) return false
    if (!query) return true
    return (s.name && s.name.includes(query)) || (s.studentNo && s.studentNo.includes(query))
  })
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
              <h3 class="header-title">教学班级管理</h3>
              <p class="header-subtitle">管理教学班级，分配课程和教师</p>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增教学班级
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="filter-section">
        <el-form :inline="true" @submit.prevent="handleSearch" class="filter-form">
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="班级名称" clearable class="filter-input">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="课程">
            <el-select v-model="filters.courseId" placeholder="全部" clearable filterable class="filter-select">
              <el-option v-for="c in courses" :key="c.id" :label="`${c.code} - ${c.name}`" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="主讲教师">
            <el-select v-model="filters.teacherId" placeholder="全部" clearable filterable class="filter-select">
              <el-option v-for="t in teachers" :key="t.id" :label="t.realName || t.username" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="学期编码">
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

      <el-table :data="tableData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="className" label="班级名称" min-width="160">
          <template #default="{ row }">
            <span class="class-name">{{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程" min-width="160">
          <template #default="{ row }">
            <span class="course-name">{{ row.courseName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="主讲教师" min-width="120" />
        <el-table-column prop="semesterCode" label="学期编码" min-width="150">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ row.semesterCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
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
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 教学班级新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑教学班级' : '新增教学班级'"
      width="520px"
      destroy-on-close
      class="custom-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如 2025数据结构-A班" />
        </el-form-item>
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择课程" filterable style="width: 100%;">
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="`${c.code} - ${c.name}`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主讲教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择教师" filterable style="width: 100%;">
            <el-option
              v-for="t in teachers"
              :key="t.id"
              :label="t.realName || t.username"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="form.semesterId" placeholder="请选择学期" filterable style="width: 100%;">
            <el-option
              v-for="s in semesters"
              :key="s.id"
              :label="s.semesterCode"
              :value="s.id"
            />
          </el-select>
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

/* ===== Filter Section ===== */
.filter-section {
  background: #F8FAFC;
  border-radius: 10px;
  padding: 20px;
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
  width: 180px;
}

.filter-select {
  width: 180px;
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
  font-weight: 600;
  color: #1E293B;
}

.course-name {
  font-weight: 500;
  color: #059669;
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
  color: #059669;
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

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input,
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

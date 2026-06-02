<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getTeachingClasses, createTeachingClass, updateTeachingClass, deleteTeachingClass,
  getTeachingClassStudents, addTeachingClassStudent, removeTeachingClassStudent, getCourses, getStudents
} from '@/api/academic'
import { getUsers, getRoles, getSemesters } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

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
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>教学班级管理</h3>
          <el-button type="primary" @click="handleAdd">新增教学班级</el-button>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" style="margin-bottom:16px" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="班级名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="filters.courseId" placeholder="全部" clearable filterable style="width:180px">
            <el-option v-for="c in courses" :key="c.id" :label="`${c.code} - ${c.name}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主讲教师">
          <el-select v-model="filters.teacherId" placeholder="全部" clearable filterable style="width:130px">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName || t.username" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期编码">
          <el-select v-model="filters.semesterId" placeholder="全部" clearable filterable style="width:160px">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterCode" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="className" label="班级名称" min-width="160" />
        <el-table-column prop="courseName" label="课程" min-width="160" />
        <el-table-column prop="teacherName" label="主讲教师" min-width="120" />
        <el-table-column prop="semesterCode" label="学期编码" min-width="150" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleManageStudents(row)">学生管理</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
    >
      <div style="display:flex;gap:8px;margin-bottom:12px">
        <el-select
          v-model="addStudentId"
          filterable
          remote
          placeholder="输入学号或姓名搜索学生"
          :remote-method="onAddSelect"
          @focus="onAddSelect('')"
          style="flex:1"
        >
          <el-option
            v-for="s in availableStudents"
            :key="s.id"
            :label="`${s.studentNo} - ${s.name}`"
            :value="s.id"
          />
        </el-select>
        <el-button type="primary" @click="handleAddStudent">添加</el-button>
      </div>

      <el-table :data="classStudents" v-loading="studentLoading" stripe style="width:100%" max-height="400">
        <el-table-column prop="studentNo" label="学号" min-width="130" />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleRemoveStudent(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 {
  margin: 0;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-start;
}
</style>

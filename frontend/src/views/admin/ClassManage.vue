<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminClasses, createAdminClass, updateAdminClass, deleteAdminClass, getMajors, getAdminClassStudents, addStudentToAdminClass, removeStudentFromAdminClass } from '@/api/admin'
import { getStudents } from '@/api/academic'
import { ElMessage, ElMessageBox } from 'element-plus'

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
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>班级管理</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-input v-model="searchClassName" placeholder="班级名称" clearable style="width:160px" @keyup.enter="handleSearch" />
            <el-select v-model="searchMajor" placeholder="所属专业" clearable style="width:170px">
              <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.name" />
            </el-select>
            <el-select v-model="searchYear" placeholder="入学年份" clearable style="width:120px">
              <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleAdd">新增班级</el-button>
          </div>
        </div>
      </template>
      <el-table :data="pagedData" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="className" label="班级名称" min-width="200" />
        <el-table-column prop="majorName" label="所属专业" min-width="180" />
        <el-table-column prop="enrollmentYear" label="入学年份" width="100" align="center" />
        <el-table-column prop="studentCount" label="学生人数" width="100" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button link type="success" @click="handleManageStudents(row)">学生管理</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
    </el-card>

    <!-- 班级编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '新增班级'" width="480px" destroy-on-close>
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

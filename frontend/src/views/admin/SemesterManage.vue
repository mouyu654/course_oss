<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSemesters, createSemester, updateSemester, deleteSemester } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

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
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>学期管理</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-select v-model="searchCode" placeholder="学期编码" clearable style="width:170px">
              <el-option v-for="c in codeOptions" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="searchYear" placeholder="学年" clearable style="width:150px">
              <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleAdd">新增学期</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredData" v-loading="loading" stripe>
        <el-table-column prop="semesterCode" label="学期编码" min-width="160" />
        <el-table-column prop="academicYear" label="学年" min-width="130" />
        <el-table-column prop="semester" label="学期" width="110" align="center">
          <template #default="{ row }">{{ row.semester === 1 ? '第一学期' : '第二学期' }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" min-width="130" />
        <el-table-column prop="endDate" label="结束日期" min-width="130" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改学期' : '新增学期'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学期编码" prop="semesterCode">
          <el-input v-model="form.semesterCode" placeholder="如 2025-2026-1" />
        </el-form-item>
        <el-form-item label="学年" prop="academicYear">
          <el-input v-model="form.academicYear" placeholder="如 2025-2026" />
        </el-form-item>
        <el-form-item label="学期">
          <el-radio-group v-model="form.semester">
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

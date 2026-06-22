<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMajors, createMajor, updateMajor, deleteMajor, getColleges } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const colleges = ref([])
const searchCode = ref('')
const searchName = ref('')
const searchCollege = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({ id: null, name: '', code: '', collegeId: null })

const rules = {
  name: [{ required: true, message: '请输入专业名称', trigger: 'blur' }]
}

const collegeMap = computed(() => {
  const map = {}
  for (const c of colleges.value) map[c.id] = c.name
  return map
})

const filteredData = computed(() => {
  return tableData.value.filter(r => {
    if (searchCode.value && !(r.code && r.code.toLowerCase().includes(searchCode.value.toLowerCase()))) return false
    if (searchName.value && !(r.name && r.name.toLowerCase().includes(searchName.value.toLowerCase()))) return false
    if (searchCollege.value && collegeMap.value[r.collegeId] !== searchCollege.value) return false
    return true
  })
})

onMounted(() => {
  loadData()
  loadColleges()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMajors()
    const data = res.data || res
    tableData.value = Array.isArray(data) ? data : data?.records || []
  } finally {
    loading.value = false
  }
}

async function loadColleges() {
  try {
    const res = await getColleges()
    colleges.value = res.data || res || []
  } catch { /* ignore */ }
}

function handleSearch() {}
function handleReset() { searchCode.value = ''; searchName.value = ''; searchCollege.value = '' }

function handleAdd() {
  isEdit.value = false
  form.value = { id: null, name: '', code: '', collegeId: null }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除专业「${row.name}」？`, '提示', { type: 'warning' })
  await deleteMajor(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMajor(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createMajor(form.value)
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
          <h3>专业管理</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-input v-model="searchCode" placeholder="专业代码" clearable style="width:140px" @keyup.enter="handleSearch" />
            <el-input v-model="searchName" placeholder="专业名称" clearable style="width:150px" @keyup.enter="handleSearch" />
            <el-select v-model="searchCollege" placeholder="所属学院" clearable style="width:160px">
              <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.name" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleAdd">新增专业</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredData" v-loading="loading" stripe>
        <el-table-column prop="code" label="专业代码" min-width="130" />
        <el-table-column prop="name" label="专业名称" min-width="200" />
        <el-table-column label="所属学院" min-width="200">
          <template #default="{ row }">{{ collegeMap[row.collegeId] || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改专业' : '新增专业'" width="440px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="专业代码">
          <el-input v-model="form.code" placeholder="如 080901" />
        </el-form-item>
        <el-form-item label="专业名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="form.collegeId" placeholder="请选择" style="width:100%">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

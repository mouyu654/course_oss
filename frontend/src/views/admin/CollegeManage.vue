<script setup>
import { ref, computed, onMounted } from 'vue'
import { getColleges, createCollege, updateCollege, deleteCollege } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({ id: null, name: '' })

const rules = {
  name: [{ required: true, message: '请输入学院名称', trigger: 'blur' }]
}

const filteredData = computed(() => {
  if (!keyword.value) return tableData.value
  const kw = keyword.value.toLowerCase()
  return tableData.value.filter(r => r.name && r.name.toLowerCase().includes(kw))
})

onMounted(() => { loadData() })

async function loadData() {
  loading.value = true
  try {
    const res = await getColleges()
    tableData.value = res.data || res || []
  } finally {
    loading.value = false
  }
}

function handleSearch() {}
function handleReset() { keyword.value = '' }

function handleAdd() {
  isEdit.value = false
  form.value = { id: null, name: '' }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除学院「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCollege(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCollege(form.value.id, { name: form.value.name })
      ElMessage.success('修改成功')
    } else {
      await createCollege({ name: form.value.name })
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
          <h3>学院管理</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-input v-model="keyword" placeholder="学院名称" clearable style="width:200px" @keyup.enter="handleSearch" />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleAdd">新增学院</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredData" v-loading="loading" stripe>
        <el-table-column prop="name" label="学院名称" min-width="300" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">修改</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改学院' : '新增学院'" width="400px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学院名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入学院名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getColleges, createCollege, updateCollege, deleteCollege } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

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
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #7C3AED 0%, #8B5CF6 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">学院管理</h3>
              <p class="header-subtitle">管理学校学院信息</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="search-box">
              <el-input v-model="keyword" placeholder="搜索学院名称" clearable class="search-input" @keyup.enter="handleSearch">
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button @click="handleReset">
                <el-icon><RefreshRight /></el-icon>
                重置
              </el-button>
            </div>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增学院
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredData" v-loading="loading" stripe class="data-table">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="学院名称" min-width="300">
          <template #default="{ row }">
            <div class="college-name-cell">
              <div class="college-icon">
                <el-icon><School /></el-icon>
              </div>
              <span class="college-name">{{ row.name }}</span>
            </div>
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

      <div class="table-footer">
        <span class="total-text">共 {{ filteredData.length }} 个学院</span>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改学院' : '新增学院'" width="400px" destroy-on-close class="custom-dialog">
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

.search-input {
  width: 200px;
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

.college-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.college-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7C3AED;
  font-size: 16px;
  flex-shrink: 0;
}

.college-name {
  font-weight: 500;
  color: #1E293B;
}

.table-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #F1F5F9;
}

.total-text {
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

  .search-input {
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

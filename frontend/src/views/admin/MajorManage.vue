<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMajors, createMajor, updateMajor, deleteMajor, getColleges } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

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
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #059669 0%, #10B981 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">专业管理</h3>
              <p class="header-subtitle">管理专业信息和所属学院</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="search-box">
              <el-input v-model="searchCode" placeholder="专业代码" clearable class="filter-input" @keyup.enter="handleSearch">
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-input v-model="searchName" placeholder="专业名称" clearable class="filter-input" @keyup.enter="handleSearch" />
              <el-select v-model="searchCollege" placeholder="所属学院" clearable class="filter-select">
                <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.name" />
              </el-select>
              <el-button @click="handleReset">
                <el-icon><RefreshRight /></el-icon>
                重置
              </el-button>
            </div>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增专业
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="code" label="专业代码" min-width="130" align="center">
          <template #default="{ row }">
            <span class="code-text">{{ row.code || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="专业名称" min-width="200">
          <template #default="{ row }">
            <span class="major-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属学院" min-width="200" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ collegeMap[row.collegeId] || '-' }}</el-tag>
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
        <span class="total-text">共 {{ filteredData.length }} 个专业</span>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改专业' : '新增专业'" width="440px" destroy-on-close class="custom-dialog">
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
  width: 140px;
}

.filter-select {
  width: 160px;
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
  color: #059669;
}

.major-name {
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

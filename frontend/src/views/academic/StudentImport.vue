<script setup>
import { ref, onMounted } from 'vue'
import { getStudents, createStudent, updateStudent, deleteStudent, batchUpdateStudentStatus, downloadStudentTemplate, batchImportStudents } from '@/api/academic'
import { getMajors, getColleges, getAdminClasses } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, Plus, Search, RefreshRight } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const majors = ref([])
const colleges = ref([])
const adminClasses = ref([])
const selectedIds = ref([])
const batchStatus = ref('在读')

// 查询条件
const filters = ref({ keyword: '', majorId: null, collegeId: null, adminClassId: null, enrollmentYear: null, enrollmentStatus: '' })

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ studentNo: '', name: '', collegeId: null, majorId: null, enrollmentYear: null, adminClassId: null, enrollmentStatus: '在读' })

const statusOptions = ['在读', '毕业', '休学', '退学', '延毕']

// 导入相关
const importDialogVisible = ref(false)
const importUploading = ref(false)

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

onMounted(async () => {
  await Promise.all([loadData(), loadMajors(), loadColleges(), loadAdminClasses()])
})

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, ...filters.value }
    // 清除空值
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === null) delete params[k] })
    const res = await getStudents(params)
    tableData.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function loadMajors() {
  try { const res = await getMajors(); const d = res.data || res; majors.value = Array.isArray(d) ? d : d?.records || [] } catch {}
}
async function loadColleges() {
  try { const res = await getColleges(); colleges.value = res.data || res || [] } catch {}
}
async function loadAdminClasses() {
  try { const res = await getAdminClasses({ size: 999 }); const d = res.data || res; adminClasses.value = Array.isArray(d) ? d : d?.records || [] } catch {}
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() { filters.value = { keyword: '', majorId: null, collegeId: null, adminClassId: null, enrollmentYear: null, enrollmentStatus: '' }; handleSearch() }
function handlePageChange(p) { page.value = p; loadData() }
function handleSizeChange(s) { size.value = s; page.value = 1; loadData() }

function handleAdd() {
  isEdit.value = false
  form.value = { studentNo: '', name: '', collegeId: null, majorId: null, enrollmentYear: null, adminClassId: null, enrollmentStatus: '在读' }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除学生「${row.name}」（${row.studentNo}）？`, '提示', { type: 'warning' })
  await deleteStudent(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateStudent(form.value.id, form.value)
    ElMessage.success('修改成功')
  } else {
    await createStudent(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

// 批量更新学籍状态
async function handleBatchUpdate() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选要更新的学生')
    return
  }
  await ElMessageBox.confirm(`确认将 ${selectedIds.value.length} 名学生的学籍状态修改为「${batchStatus.value}」？`, '批量更新', { type: 'warning' })
  await batchUpdateStudentStatus({ ids: selectedIds.value, status: batchStatus.value })
  ElMessage.success('批量更新成功')
  selectedIds.value = []
  loadData()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

// ---- 导入 ----
function downloadFile(blob, filename) {
  const url = window.URL.createObjectURL(new Blob([blob]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function handleDownloadTemplate() {
  try {
    const res = await downloadStudentTemplate()
    downloadFile(res, '学生信息模板.xlsx')
    ElMessage.success('模板下载成功')
  } catch { /* interceptor handles error */ }
}

function showImportResult(res) {
  const d = res?.data || res
  if (d.imported !== undefined) {
    let msg = `导入完成：成功 ${d.imported} 条，跳过 ${d.skipped ?? 0} 条，共 ${d.total} 条`
    if (d.collegeMatched !== undefined) {
      msg += `；学院匹配 ${d.collegeMatched}，专业匹配 ${d.majorMatched}`
    }
    ElMessage.success(msg)
  } else {
    ElMessage.success('导入完成')
  }
}

async function handleImportStudents(options) {
  importUploading.value = true
  try {
    const res = await batchImportStudents(options.file)
    showImportResult(res)
    importDialogVisible.value = false
    loadData()
  } finally {
    importUploading.value = false
  }
}

function statusTagType(status) {
  const map = { '在读': 'success', '毕业': 'info', '休学': 'warning', '退学': 'danger', '延毕': 'warning' }
  return map[status] || 'info'
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
              <h3 class="header-title">学生信息管理</h3>
              <p class="header-subtitle">管理学生信息，支持批量导入和状态更新</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="action-buttons">
              <el-button type="primary" @click="handleAdd">
                <el-icon><Plus /></el-icon>
                新增学生
              </el-button>
              <el-button type="success" @click="importDialogVisible = true">
                <el-icon><Upload /></el-icon>
                导入学生
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="filter-section">
        <el-form :inline="true" @submit.prevent="handleSearch" class="filter-form">
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="学号/姓名" clearable class="filter-input">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="学院">
            <el-select v-model="filters.collegeId" placeholder="全部" clearable class="filter-select">
              <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业">
            <el-select v-model="filters.majorId" placeholder="全部" clearable filterable class="filter-select">
              <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="学籍状态">
            <el-select v-model="filters.enrollmentStatus" placeholder="全部" clearable class="filter-select-small">
              <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="入学年份">
            <el-input-number v-model="filters.enrollmentYear" :min="2000" :max="2030" controls-position="right" class="filter-number" />
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

      <!-- 批量操作 -->
      <div class="batch-bar" v-if="selectedIds.length > 0">
        <div class="batch-info">
          <el-icon><InfoFilled /></el-icon>
          <span>已选 <strong>{{ selectedIds.length }}</strong> 项</span>
        </div>
        <div class="batch-actions">
          <el-select v-model="batchStatus" class="batch-select">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button type="warning" @click="handleBatchUpdate">
            批量更新状态
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe class="data-table" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="studentNo" label="学号" min-width="120" align="center">
          <template #default="{ row }">
            <span class="student-no">{{ row.studentNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="90">
          <template #default="{ row }">
            <span class="student-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="collegeName" label="学院" min-width="180" show-overflow-tooltip />
        <el-table-column prop="majorName" label="专业" min-width="200" show-overflow-tooltip />
        <el-table-column prop="enrollmentYear" label="入学年份" width="100" align="center">
          <template #default="{ row }">
            <span class="year-text">{{ row.enrollmentYear }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学籍状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.enrollmentStatus)" size="small" effect="dark" class="status-tag">
              {{ row.enrollmentStatus || '在读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="520px" destroy-on-close class="custom-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" :disabled="isEdit" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="form.collegeId" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="form.majorId" placeholder="请选择" clearable filterable style="width:100%">
            <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入学年份">
          <el-input-number v-model="form.enrollmentYear" :min="2000" :max="2030" style="width:100%" />
        </el-form-item>
        <el-form-item label="行政班级">
          <el-select v-model="form.adminClassId" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="c in adminClasses" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学籍状态">
          <el-select v-model="form.enrollmentStatus" style="width:100%">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入学生弹窗 -->
    <el-dialog v-model="importDialogVisible" title="导入学生" width="480px" destroy-on-close class="custom-dialog">
      <div class="import-content">
        <p class="import-hint">请下载模板，按格式填写学生信息后上传 Excel 文件</p>
        <p class="import-hint-small">支持字段：学号、姓名、学院名称、专业名称、入学年份、学籍状态</p>
        <div class="import-actions">
          <el-button @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
          <el-upload :show-file-list="false" :http-request="handleImportStudents" accept=".xlsx,.xls">
            <el-button type="primary" :loading="importUploading">
              <el-icon><Upload /></el-icon>
              选择文件导入
            </el-button>
          </el-upload>
        </div>
      </div>
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

.action-buttons {
  display: flex;
  gap: 8px;
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
  width: 150px;
}

.filter-select-small {
  width: 110px;
}

.filter-number {
  width: 120px;
}

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  border: 1px solid #FCD34D;
  border-radius: 10px;
  padding: 12px 20px;
  margin-bottom: 16px;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #92400E;
  font-size: 14px;
}

.batch-info strong {
  font-weight: 700;
}

.batch-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.batch-select {
  width: 110px;
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

.student-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #7C3AED;
}

.student-name {
  font-weight: 500;
  color: #1E293B;
}

.year-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: #475569;
}

.status-tag {
  border-radius: 6px;
  font-weight: 600;
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

/* ===== Import Content ===== */
.import-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-hint {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.import-hint-small {
  margin: 0;
  color: #94A3B8;
  font-size: 13px;
}

.import-actions {
  display: flex;
  gap: 12px;
  align-items: center;
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

  .action-buttons {
    justify-content: flex-start;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input,
  .filter-select,
  .filter-select-small,
  .filter-number {
    width: 100%;
  }

  .batch-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .batch-actions {
    justify-content: flex-end;
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

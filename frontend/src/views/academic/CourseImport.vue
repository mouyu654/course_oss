<script setup>
import { ref, onMounted } from 'vue'
import { getCourses, createCourse, updateCourse, deleteCourse, downloadCourseTemplate, batchImportCourses } from '@/api/academic'
import { getMajors } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, Plus, Search, RefreshRight } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const majorOptions = ref([])
const form = ref({ code: '', name: '', credit: null, majorId: null })

const rules = {
  code: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  credit: [{ required: true, type: 'number', message: '请输入学分', trigger: 'blur' }]
}

const majorNameMap = ref({}) // id → name

// 导入相关
const importDialogVisible = ref(false)
const importUploading = ref(false)

onMounted(async () => {
  const [majRes] = await Promise.all([getMajors(), loadData()])
  const list = majRes.data?.records || majRes.data || []
  majorOptions.value = list
  for (const m of list) majorNameMap.value[m.id] = m.name
})

async function loadData() {
  loading.value = true
  try {
    const res = await getCourses({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    tableData.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() { keyword.value = ''; handleSearch() }

function handlePageChange(p) { page.value = p; loadData() }
function handleSizeChange(s) { size.value = s; page.value = 1; loadData() }

function handleAdd() {
  isEdit.value = false
  form.value = { code: '', name: '', credit: null, majorId: null }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = {
    id: row.id, code: row.code, name: row.name,
    credit: row.credit, majorId: row.majorId || null
  }
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除课程「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCourse(row.id)
  ElMessage.success('删除成功')
  if (tableData.value.length === 1 && page.value > 1) page.value--
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCourse(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createCourse(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
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
    const res = await downloadCourseTemplate()
    downloadFile(res, '课程信息模板.xlsx')
    ElMessage.success('模板下载成功')
  } catch { /* interceptor handles error */ }
}

function showImportResult(res) {
  const d = res?.data || res
  if (d.imported !== undefined) {
    let msg = `导入完成：成功 ${d.imported} 条，跳过 ${d.skipped ?? 0} 条，共 ${d.total} 条`
    if (d.majorMatched !== undefined) {
      msg += `；专业匹配 ${d.majorMatched}，未匹配 ${d.majorNotFound}`
    }
    ElMessage.success(msg)
  } else {
    ElMessage.success('导入完成')
  }
}

async function handleImportCourses(options) {
  importUploading.value = true
  try {
    const res = await batchImportCourses(options.file)
    showImportResult(res)
    importDialogVisible.value = false
    loadData()
  } finally {
    importUploading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #2563EB 0%, #3B82F6 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">课程管理</h3>
              <p class="header-subtitle">管理课程信息，支持批量导入</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="search-box">
              <el-input
                v-model="keyword"
                placeholder="搜索课程代码/名称"
                clearable
                class="search-input"
                @keyup.enter="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" @click="handleSearch">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
              <el-button @click="handleReset">
                <el-icon><RefreshRight /></el-icon>
                重置
              </el-button>
            </div>
            <div class="action-buttons">
              <el-button type="primary" @click="handleAdd">
                <el-icon><Plus /></el-icon>
                新增课程
              </el-button>
              <el-button type="success" @click="importDialogVisible = true">
                <el-icon><Upload /></el-icon>
                导入课程
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="code" label="课程代码" width="120" align="center">
          <template #default="{ row }">
            <span class="code-text">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="课程名称" min-width="160">
          <template #default="{ row }">
            <span class="name-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="学分" width="80" align="center">
          <template #default="{ row }">
            <span class="credit-text">{{ row.credit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属专业" width="160" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ majorNameMap[row.majorId] || '-' }}</el-tag>
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

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改课程' : '新增课程'" width="520px" destroy-on-close class="custom-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="课程代码" prop="code">
          <el-input v-model="form.code" placeholder="如 CS1001" />
        </el-form-item>
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="如 数据结构" />
        </el-form-item>
        <el-form-item label="学分" prop="credit">
          <el-input-number v-model="form.credit" :min="0.5" :max="20" :step="0.5" style="width:100%" />
        </el-form-item>
        <el-form-item label="所属专业">
          <el-select v-model="form.majorId" placeholder="请选择专业（可选）" clearable filterable style="width:100%">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入课程弹窗 -->
    <el-dialog v-model="importDialogVisible" title="导入课程" width="480px" destroy-on-close class="custom-dialog">
      <div class="import-content">
        <p class="import-hint">请下载模板，按格式填写课程信息后上传 Excel 文件</p>
        <div class="import-actions">
          <el-button @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
          <el-upload :show-file-list="false" :http-request="handleImportCourses" accept=".xlsx,.xls">
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
  gap: 16px;
  flex-shrink: 0;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 220px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  gap: 8px;
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
  color: #2563EB;
}

.name-text {
  font-weight: 500;
  color: #1E293B;
}

.credit-text {
  font-weight: 600;
  color: #059669;
  font-family: 'SF Mono', 'Consolas', monospace;
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
  gap: 20px;
}

.import-hint {
  margin: 0;
  color: #64748B;
  font-size: 14px;
  line-height: 1.6;
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

  .search-box {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }

  .action-buttons {
    justify-content: flex-start;
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

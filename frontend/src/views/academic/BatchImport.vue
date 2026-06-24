<script setup>
import { ref, onMounted } from 'vue'
import {
  downloadStudentTemplate, downloadCourseTemplate, downloadClassStudentTemplate,
  batchImportStudents, batchImportCourses, batchImportAdminClassStudents, batchImportTeachingClassStudents,
  getTeachingClasses
} from '@/api/academic'
import { getAdminClasses } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'

const adminClasses = ref([])
const teachingClasses = ref([])

const uploading = ref({
  students: false,
  courses: false,
  adminClass: false,
  teachingClass: false
})

const selectedAdminClassId = ref(null)
const selectedTeachingClassId = ref(null)

onMounted(async () => {
  await Promise.all([loadAdminClasses(), loadTeachingClasses()])
})

async function loadAdminClasses() {
  try {
    const res = await getAdminClasses({ size: 999 })
    const data = res.data || res
    adminClasses.value = Array.isArray(data) ? data : data?.records || []
  } catch { /* ignore */ }
}

async function loadTeachingClasses() {
  try {
    const res = await getTeachingClasses({ size: 999 })
    const data = res.data || res
    teachingClasses.value = Array.isArray(data) ? data : data?.records || []
  } catch { /* ignore */ }
}

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

async function handleDownload(fn, filename) {
  try {
    const res = await fn()
    downloadFile(res, filename)
    ElMessage.success('模板下载成功')
  } catch { /* interceptor handles error */ }
}

function showResult(res) {
  const d = res?.data || res
  if (d.imported !== undefined) {
    ElMessage.success(`导入完成：成功 ${d.imported} 条，跳过 ${d.skipped ?? 0} 条，共 ${d.total} 条`)
  } else if (d.added !== undefined) {
    ElMessage.success(`导入完成：添加 ${d.added} 人，未找到 ${d.notFound} 人，已存在 ${d.exists} 人，共 ${d.total} 条`)
  } else {
    ElMessage.success('导入完成')
  }
}

async function handleUploadStudents(options) {
  uploading.value.students = true
  try {
    const res = await batchImportStudents(options.file)
    showResult(res)
  } finally { uploading.value.students = false }
}

async function handleUploadCourses(options) {
  uploading.value.courses = true
  try {
    const res = await batchImportCourses(options.file)
    showResult(res)
  } finally { uploading.value.courses = false }
}

async function handleUploadAdminClass(options) {
  if (!selectedAdminClassId.value) {
    ElMessage.warning('请先选择行政班级')
    return
  }
  uploading.value.adminClass = true
  try {
    const res = await batchImportAdminClassStudents(selectedAdminClassId.value, options.file)
    showResult(res)
  } finally { uploading.value.adminClass = false }
}

async function handleUploadTeachingClass(options) {
  if (!selectedTeachingClassId.value) {
    ElMessage.warning('请先选择教学班级')
    return
  }
  uploading.value.teachingClass = true
  try {
    const res = await batchImportTeachingClassStudents(selectedTeachingClassId.value, options.file)
    showResult(res)
  } finally { uploading.value.teachingClass = false }
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #EA580C 0%, #F97316 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">批量数据导入</h3>
              <p class="header-subtitle">批量导入学生、课程和班级数据</p>
            </div>
          </div>
        </div>
      </template>

      <!-- 学生信息导入 -->
      <div class="import-section">
        <div class="section-header">
          <div class="section-icon" style="background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%); color: #059669;">
            <el-icon><User /></el-icon>
          </div>
          <div class="section-content">
            <h4 class="section-title">批量导入学生信息</h4>
            <p class="section-desc">Excel 列：学号、姓名、学院名称、专业名称、入学年份、学籍状态</p>
          </div>
          <el-button @click="handleDownload(downloadStudentTemplate, '学生信息模板.xlsx')">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
        <div class="section-action">
          <el-upload :show-file-list="false" :http-request="handleUploadStudents" accept=".xlsx,.xls">
            <el-button type="primary" :loading="uploading.students">
              <el-icon><Upload /></el-icon>
              选择文件导入
            </el-button>
          </el-upload>
        </div>
      </div>

      <el-divider class="custom-divider" />

      <!-- 课程信息导入 -->
      <div class="import-section">
        <div class="section-header">
          <div class="section-icon" style="background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%); color: #2563EB;">
            <el-icon><Document /></el-icon>
          </div>
          <div class="section-content">
            <h4 class="section-title">批量导入课程信息</h4>
            <p class="section-desc">Excel 列：课程代码、课程名称、学分</p>
          </div>
          <el-button @click="handleDownload(downloadCourseTemplate, '课程信息模板.xlsx')">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
        <div class="section-action">
          <el-upload :show-file-list="false" :http-request="handleUploadCourses" accept=".xlsx,.xls">
            <el-button type="primary" :loading="uploading.courses">
              <el-icon><Upload /></el-icon>
              选择文件导入
            </el-button>
          </el-upload>
        </div>
      </div>

      <el-divider class="custom-divider" />

      <!-- 行政班级学生导入 -->
      <div class="import-section">
        <div class="section-header">
          <div class="section-icon" style="background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%); color: #7C3AED;">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="section-content">
            <h4 class="section-title">批量导入行政班级学生</h4>
            <p class="section-desc">Excel 列：学号、姓名。系统按学号匹配学生并分配到所选行政班级</p>
          </div>
          <el-button @click="handleDownload(downloadClassStudentTemplate, '班级学生模板.xlsx')">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
        <div class="section-action">
          <el-select v-model="selectedAdminClassId" placeholder="请选择行政班级" filterable class="class-select">
            <el-option v-for="c in adminClasses" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
          <el-upload :show-file-list="false" :http-request="handleUploadAdminClass" accept=".xlsx,.xls">
            <el-button type="primary" :loading="uploading.adminClass">
              <el-icon><Upload /></el-icon>
              选择文件导入
            </el-button>
          </el-upload>
        </div>
      </div>

      <el-divider class="custom-divider" />

      <!-- 教学班级学生导入 -->
      <div class="import-section">
        <div class="section-header">
          <div class="section-icon" style="background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%); color: #D97706;">
            <el-icon><School /></el-icon>
          </div>
          <div class="section-content">
            <h4 class="section-title">批量导入教学班级学生</h4>
            <p class="section-desc">Excel 列：学号、姓名。系统按学号匹配学生并添加到所选教学班级</p>
          </div>
          <el-button @click="handleDownload(downloadClassStudentTemplate, '班级学生模板.xlsx')">
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
        <div class="section-action">
          <el-select v-model="selectedTeachingClassId" placeholder="请选择教学班级" filterable class="class-select">
            <el-option v-for="c in teachingClasses" :key="c.id" :label="`${c.className}（${c.courseName || ''}）`" :value="c.id" />
          </el-select>
          <el-upload :show-file-list="false" :http-request="handleUploadTeachingClass" accept=".xlsx,.xls">
            <el-button type="primary" :loading="uploading.teachingClass">
              <el-icon><Upload /></el-icon>
              选择文件导入
            </el-button>
          </el-upload>
        </div>
      </div>
    </el-card>
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

/* ===== Import Section ===== */
.import-section {
  padding: 8px 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.section-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.section-content {
  flex: 1;
}

.section-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.section-desc {
  margin: 0;
  font-size: 13px;
  color: #64748B;
  line-height: 1.5;
}

.section-action {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-left: 64px;
}

.class-select {
  width: 300px;
}

/* ===== Divider ===== */
.custom-divider {
  margin: 24px 0;
  border-color: #F1F5F9;
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .section-action {
    padding-left: 0;
    flex-wrap: wrap;
  }

  .class-select {
    width: 100%;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent {
    transition: none;
  }
}
</style>

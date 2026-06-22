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
    <el-card>
      <template #header><h3>批量数据导入</h3></template>

      <!-- 学生信息导入 -->
      <div class="import-section">
        <div class="section-header">
          <h4>批量导入学生信息</h4>
          <el-button :icon="Download" size="small" @click="handleDownload(downloadStudentTemplate, '学生信息模板.xlsx')">下载模板</el-button>
        </div>
        <p class="section-desc">Excel 列：学号、姓名、学院名称、专业名称、入学年份、学籍状态</p>
        <el-upload :show-file-list="false" :http-request="handleUploadStudents" accept=".xlsx,.xls">
          <el-button type="primary" :icon="Upload" :loading="uploading.students">选择文件导入</el-button>
        </el-upload>
      </div>

      <el-divider />

      <!-- 课程信息导入 -->
      <div class="import-section">
        <div class="section-header">
          <h4>批量导入课程信息</h4>
          <el-button :icon="Download" size="small" @click="handleDownload(downloadCourseTemplate, '课程信息模板.xlsx')">下载模板</el-button>
        </div>
        <p class="section-desc">Excel 列：课程代码、课程名称、学分</p>
        <el-upload :show-file-list="false" :http-request="handleUploadCourses" accept=".xlsx,.xls">
          <el-button type="primary" :icon="Upload" :loading="uploading.courses">选择文件导入</el-button>
        </el-upload>
      </div>

      <el-divider />

      <!-- 行政班级学生导入 -->
      <div class="import-section">
        <div class="section-header">
          <h4>批量导入行政班级学生</h4>
          <el-button :icon="Download" size="small" @click="handleDownload(downloadClassStudentTemplate, '班级学生模板.xlsx')">下载模板</el-button>
        </div>
        <p class="section-desc">Excel 列：学号、姓名。系统按学号匹配学生并分配到所选行政班级</p>
        <div style="display:flex;gap:12px;align-items:center;margin-bottom:12px">
          <el-select v-model="selectedAdminClassId" placeholder="请选择行政班级" filterable style="width:300px">
            <el-option v-for="c in adminClasses" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
          <el-upload :show-file-list="false" :http-request="handleUploadAdminClass" accept=".xlsx,.xls">
            <el-button type="primary" :icon="Upload" :loading="uploading.adminClass">选择文件导入</el-button>
          </el-upload>
        </div>
      </div>

      <el-divider />

      <!-- 教学班级学生导入 -->
      <div class="import-section">
        <div class="section-header">
          <h4>批量导入教学班级学生</h4>
          <el-button :icon="Download" size="small" @click="handleDownload(downloadClassStudentTemplate, '班级学生模板.xlsx')">下载模板</el-button>
        </div>
        <p class="section-desc">Excel 列：学号、姓名。系统按学号匹配学生并添加到所选教学班级</p>
        <div style="display:flex;gap:12px;align-items:center">
          <el-select v-model="selectedTeachingClassId" placeholder="请选择教学班级" filterable style="width:300px">
            <el-option v-for="c in teachingClasses" :key="c.id" :label="`${c.className}（${c.courseName || ''}）`" :value="c.id" />
          </el-select>
          <el-upload :show-file-list="false" :http-request="handleUploadTeachingClass" accept=".xlsx,.xls">
            <el-button type="primary" :icon="Upload" :loading="uploading.teachingClass">选择文件导入</el-button>
          </el-upload>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.import-section {
  padding: 8px 0;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}
.section-header h4 {
  margin: 0;
  font-size: 15px;
}
.section-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
}
</style>

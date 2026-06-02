<script setup>
import { ref, onMounted } from 'vue'
import { getStudents, createStudent, updateStudent, deleteStudent, batchUpdateStudentStatus } from '@/api/academic'
import { getMajors, getColleges, getAdminClasses } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

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

function statusTagType(status) {
  const map = { '在读': 'success', '毕业': 'info', '休学': 'warning', '退学': 'danger', '延毕': 'warning' }
  return map[status] || 'info'
}
</script>

<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>学生信息管理</h3>
          <el-button type="primary" @click="handleAdd">新增学生</el-button>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" style="margin-bottom:16px" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="学号/姓名" clearable style="width:150px" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="filters.collegeId" placeholder="全部" clearable style="width:150px">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="filters.majorId" placeholder="全部" clearable filterable style="width:160px">
            <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学籍状态">
          <el-select v-model="filters.enrollmentStatus" placeholder="全部" clearable style="width:110px">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="入学年份">
          <el-input-number v-model="filters.enrollmentYear" :min="2000" :max="2030" controls-position="right" style="width:120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作 -->
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:12px">
        <span style="font-size:13px;color:#606266">已选 {{ selectedIds.length }} 项</span>
        <el-select v-model="batchStatus" style="width:110px" size="small">
          <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button size="small" type="warning" :disabled="!selectedIds.length" @click="handleBatchUpdate">批量更新状态</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe style="width:100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="90" />
        <el-table-column prop="collegeName" label="学院" min-width="180" show-overflow-tooltip />
        <el-table-column prop="majorName" label="专业" min-width="200" show-overflow-tooltip />
        <el-table-column prop="enrollmentYear" label="入学年份" width="100" align="center" />
        <el-table-column label="学籍状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.enrollmentStatus)" size="small">{{ row.enrollmentStatus || '在读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="520px" destroy-on-close>
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
  </div>
</template>

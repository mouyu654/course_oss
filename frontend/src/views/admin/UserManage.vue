<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, createUser, updateUser, deleteUser, toggleUserStatus, getRoles } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const roles = ref([])
const filters = ref({ keyword: '', roleId: null, status: null })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ username: '', realName: '', password: '', roleId: null, collegeId: null })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(() => {
  loadData()
  loadRoles()
})

async function loadData() {
  loading.value = true
  try {
    const params = { ...filters.value }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === null) delete params[k] })
    const res = await getUsers(params)
    tableData.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await getRoles()
    roles.value = res.data || res || []
  } catch { /* ignore */ }
}

function getRoleName(roleId) {
  const role = roles.value.find(r => r.id === roleId)
  return role?.roleName || '-'
}

function getRoleColor(roleId) {
  const role = roles.value.find(r => r.id === roleId)
  const map = {
    'ADMIN': 'danger',
    'ACADEMIC': '',
    'DIRECTOR': 'warning',
    'TEACHER': 'success'
  }
  return map[role?.roleCode] || 'info'
}

function handleSearch() { loadData() }
function handleReset() { filters.value = { keyword: '', roleId: null, status: null }; handleSearch() }

function handleAdd() {
  isEdit.value = false
  form.value = { username: '', realName: '', password: '', roleId: null, collegeId: null }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { id: row.id, username: row.username, realName: row.realName, roleId: row.roleId, collegeId: row.collegeId, password: '' }
  dialogVisible.value = true
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户「${row.realName}」？`, '提示', { type: 'warning' })
  await toggleUserStatus(row.id)
  ElMessage.success(`${action}成功`)
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.realName}」？此操作不可恢复。`, '删除确认', { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    const payload = { realName: form.value.realName, roleId: form.value.roleId, collegeId: form.value.collegeId }
    if (form.value.password) payload.password = form.value.password
    await updateUser(form.value.id, payload)
    ElMessage.success('修改成功')
  } else {
    await createUser(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #DC2626 0%, #EF4444 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">用户管理</h3>
              <p class="header-subtitle">管理系统用户账号和权限</p>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增用户
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="filter-section">
        <el-form :inline="true" @submit.prevent="handleSearch" class="filter-form">
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="用户名/真实姓名" clearable class="filter-input">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="filters.roleId" placeholder="全部" clearable class="filter-select">
              <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filters.status" placeholder="全部" clearable class="filter-select-small">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
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

      <el-table :data="tableData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="username" label="用户名" min-width="150">
          <template #default="{ row }">
            <span class="username-text">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="130">
          <template #default="{ row }">
            <span class="name-text">{{ row.realName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleColor(row.roleId)" size="small" effect="dark" class="role-tag">
              {{ getRoleName(row.roleId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark" class="status-tag">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
              <el-icon><component :is="row.status === 1 ? 'Lock' : 'Unlock'" /></el-icon>
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px" destroy-on-close class="custom-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'" :rules="isEdit ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }]">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
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
  width: 200px;
}

.filter-select {
  width: 130px;
}

.filter-select-small {
  width: 100px;
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

.username-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #DC2626;
}

.name-text {
  font-weight: 500;
  color: #1E293B;
}

.role-tag {
  border-radius: 6px;
  font-weight: 600;
}

.status-tag {
  border-radius: 6px;
  font-weight: 600;
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

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input,
  .filter-select,
  .filter-select-small {
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

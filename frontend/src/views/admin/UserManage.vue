<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, createUser, updateUser, deleteUser, toggleUserStatus, getRoles } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

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
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>用户管理</h3>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>
      <!-- 查询条件 -->
      <el-form :inline="true" style="margin-bottom:16px" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="用户名/真实姓名" clearable style="width:170px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.roleId" placeholder="全部" clearable style="width:130px">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="realName" label="真实姓名" min-width="130" />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getRoleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
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

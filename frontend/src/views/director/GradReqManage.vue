<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { getMajors } from '@/api/admin'
import {
  getGradReqs,
  createGradReq,
  updateGradReq,
  deleteGradReq,
  createIndicator,
  updateIndicator,
  deleteIndicator
} from '@/api/director'
import { ElMessage, ElMessageBox } from 'element-plus'

/* ---------- 基础状态 ---------- */
const loading = ref(false)
const majorOptions = ref([])
const selectedMajorId = ref(null)
const gradReqs = ref([])

/* ---------- 毕业要求弹窗 ---------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, reqNo: null, title: '', content: '' })
const formRules = {
  reqNo: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
}

/* ---------- 指标点弹窗 ---------- */
const indicatorDialogVisible = ref(false)
const indicatorIsEdit = ref(false)
const indicatorFormRef = ref(null)
const indicatorForm = reactive({ id: null, indicatorNo: '', content: '' })
const indicatorRules = {
  indicatorNo: [{ required: true, message: '请输入指标点编号', trigger: 'blur' }],
  content: [{ required: true, message: '请输入描述', trigger: 'blur' }]
}
const currentReqId = ref(null)

/* ---------- 初始化 ---------- */
onMounted(() => { loadMajors() })
watch(selectedMajorId, (v) => { if (v) loadData(); else gradReqs.value = [] })

/* ---------- 加载专业列表 ---------- */
async function loadMajors() {
  try {
    const res = await getMajors()
    majorOptions.value = res.data?.records || res.data || []
  } catch { /* ignore */ }
}

/* ---------- 加载毕业要求 ---------- */
async function loadData() {
  if (!selectedMajorId.value) return
  loading.value = true
  try {
    const res = await getGradReqs(selectedMajorId.value)
    gradReqs.value = res.data || []
  } finally {
    loading.value = false
  }
}

/* ---------- 毕业要求 CRUD ---------- */
function handleAdd() {
  isEdit.value = false
  Object.assign(form, { id: null, reqNo: null, title: '', content: '' })
  dialogVisible.value = true
}

function handleEdit(req) {
  isEdit.value = true
  Object.assign(form, { id: req.id, reqNo: req.reqNo, title: req.title, content: req.content })
  dialogVisible.value = true
}

async function handleDelete(req) {
  await ElMessageBox.confirm(
    `确认删除毕业要求「${req.reqNo}. ${req.title}」及其所有指标点？`,
    '提示',
    { type: 'warning' }
  )
  await deleteGradReq(req.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const data = { majorId: selectedMajorId.value, reqNo: form.reqNo, title: form.title, content: form.content }
  if (isEdit.value) {
    await updateGradReq(form.id, data)
    ElMessage.success('修改成功')
  } else {
    await createGradReq(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

/* ---------- 指标点 CRUD ---------- */
function handleAddIndicator(reqId) {
  indicatorIsEdit.value = false
  currentReqId.value = reqId
  Object.assign(indicatorForm, { id: null, indicatorNo: '', content: '' })
  indicatorDialogVisible.value = true
}

function handleEditIndicator(reqId, ind) {
  indicatorIsEdit.value = true
  currentReqId.value = reqId
  Object.assign(indicatorForm, { id: ind.id, indicatorNo: ind.indicatorNo, content: ind.content })
  indicatorDialogVisible.value = true
}

async function handleDeleteIndicator(ind) {
  await ElMessageBox.confirm('确认删除该指标点？', '提示', { type: 'warning' })
  await deleteIndicator(ind.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmitIndicator() {
  const valid = await indicatorFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const data = { indicatorNo: indicatorForm.indicatorNo, content: indicatorForm.content }
  if (indicatorIsEdit.value) {
    await updateIndicator(indicatorForm.id, data)
    ElMessage.success('修改成功')
  } else {
    await createIndicator(currentReqId.value, data)
    ElMessage.success('新增成功')
  }
  indicatorDialogVisible.value = false
  loadData()
}
</script>

<template>
  <div class="page-container">
    <!-- 专业选择器 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <div style="display: flex; align-items: center; gap: 12px;">
        <span style="font-weight: 500; white-space: nowrap;">选择专业：</span>
        <el-select
          v-model="selectedMajorId"
          placeholder="请选择专业"
          filterable
          style="width: 320px;"
        >
          <el-option
            v-for="m in majorOptions"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>
      </div>
    </el-card>

    <!-- 毕业要求列表 -->
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <h3>毕业要求管理</h3>
          <el-button type="primary" :disabled="!selectedMajorId" @click="handleAdd">
            新增毕业要求
          </el-button>
        </div>
      </template>

      <template v-if="selectedMajorId">
        <!-- 空状态 -->
        <el-empty v-if="!loading && gradReqs.length === 0" description="暂无毕业要求，请点击上方按钮新增" />

        <!-- 折叠面板 -->
        <el-collapse v-else>
          <el-collapse-item v-for="req in gradReqs" :key="req.id" :name="req.id">
            <template #title>
              <div class="req-header">
                <span class="req-title">{{ req.reqNo }}. {{ req.title }}</span>
                <el-button link type="primary" size="small" @click.stop="handleEdit(req)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click.stop="handleDelete(req)">
                  删除
                </el-button>
              </div>
            </template>

            <!-- 毕业要求描述 -->
            <p class="req-content">{{ req.content }}</p>

            <!-- 指标点列表 -->
            <div style="margin-bottom: 8px;">
              <el-button size="small" type="primary" @click="handleAddIndicator(req.id)">
                新增指标点
              </el-button>
            </div>

            <el-table :data="req.indicators || []" size="small" stripe border>
              <el-table-column prop="indicatorNo" label="编号" width="100" />
              <el-table-column prop="content" label="指标点描述" />
              <el-table-column label="操作" width="140" align="center">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="handleEditIndicator(req.id, row)"
                  >
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteIndicator(row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </template>

      <el-empty v-else description="请先选择专业" />
    </el-card>

    <!-- 毕业要求弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑毕业要求' : '新增毕业要求'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="编号" prop="reqNo">
          <el-input-number v-model="form.reqNo" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：工程知识" />
        </el-form-item>
        <el-form-item label="详细描述" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入毕业要求的详细描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 指标点弹窗 -->
    <el-dialog
      v-model="indicatorDialogVisible"
      :title="indicatorIsEdit ? '编辑指标点' : '新增指标点'"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="indicatorFormRef"
        :model="indicatorForm"
        :rules="indicatorRules"
        label-width="80px"
      >
        <el-form-item label="编号" prop="indicatorNo">
          <el-input v-model="indicatorForm.indicatorNo" placeholder="如 1-1" />
        </el-form-item>
        <el-form-item label="描述" prop="content">
          <el-input
            v-model="indicatorForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入指标点描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="indicatorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitIndicator">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.req-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.req-title {
  font-weight: 600;
  font-size: 14px;
}

.req-content {
  color: #606266;
  margin: 0 0 16px 0;
  line-height: 1.6;
}
</style>

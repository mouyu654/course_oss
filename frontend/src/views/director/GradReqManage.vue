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
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'

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
    <el-card class="main-card filter-card">
      <div class="filter-section">
        <div class="filter-item">
          <span class="filter-label">选择专业：</span>
          <el-select
            v-model="selectedMajorId"
            placeholder="请选择专业"
            filterable
            class="major-select"
          >
            <el-option
              v-for="m in majorOptions"
              :key="m.id"
              :label="m.name"
              :value="m.id"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 毕业要求列表 -->
    <el-card v-loading="loading" class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #7C3AED 0%, #8B5CF6 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">毕业要求管理</h3>
              <p class="header-subtitle">管理毕业要求和指标点配置</p>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" :disabled="!selectedMajorId" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增毕业要求
            </el-button>
          </div>
        </div>
      </template>

      <template v-if="selectedMajorId">
        <!-- 空状态 -->
        <el-empty v-if="!loading && gradReqs.length === 0" description="暂无毕业要求，请点击上方按钮新增" class="empty-state" />

        <!-- 折叠面板 -->
        <el-collapse v-else class="req-collapse">
          <el-collapse-item v-for="req in gradReqs" :key="req.id" :name="req.id" class="req-item">
            <template #title>
              <div class="req-header">
                <div class="req-number">{{ req.reqNo }}</div>
                <div class="req-info">
                  <span class="req-title">{{ req.title }}</span>
                  <span class="req-indicator-count" v-if="req.indicators?.length">
                    {{ req.indicators.length }} 个指标点
                  </span>
                </div>
                <div class="req-actions">
                  <el-button link type="primary" size="small" @click.stop="handleEdit(req)">
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click.stop="handleDelete(req)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </div>
            </template>

            <!-- 毕业要求描述 -->
            <p class="req-content">{{ req.content }}</p>

            <!-- 指标点列表 -->
            <div class="indicator-header">
              <h4 class="indicator-title">
                <el-icon><Aim /></el-icon>
                指标点列表
              </h4>
              <el-button size="small" type="primary" @click="handleAddIndicator(req.id)">
                <el-icon><Plus /></el-icon>
                新增指标点
              </el-button>
            </div>

            <el-table :data="req.indicators || []" size="small" stripe border class="indicator-table">
              <el-table-column prop="indicatorNo" label="编号" width="100" align="center">
                <template #default="{ row }">
                  <span class="indicator-no">{{ row.indicatorNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="指标点描述">
                <template #default="{ row }">
                  <span class="indicator-content">{{ row.content }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEditIndicator(req.id, row)">
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteIndicator(row)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </template>

      <el-empty v-else description="请先选择专业" class="empty-state" />
    </el-card>

    <!-- 毕业要求弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑毕业要求' : '新增毕业要求'"
      width="560px"
      destroy-on-close
      class="custom-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="编号" prop="reqNo">
          <el-input-number v-model="form.reqNo" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：工程知识" />
        </el-form-item>
        <el-form-item label="详细描述" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入毕业要求的详细描述" />
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
      class="custom-dialog"
    >
      <el-form ref="indicatorFormRef" :model="indicatorForm" :rules="indicatorRules" label-width="80px">
        <el-form-item label="编号" prop="indicatorNo">
          <el-input v-model="indicatorForm.indicatorNo" placeholder="如 1-1" />
        </el-form-item>
        <el-form-item label="描述" prop="content">
          <el-input v-model="indicatorForm.content" type="textarea" :rows="3" placeholder="请输入指标点描述" />
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
/* ===== Filter Card ===== */
.filter-card :deep(.el-card__body) {
  padding: 16px 24px;
}

.filter-section {
  background: #F8FAFC;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #F1F5F9;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-weight: 500;
  color: #1E293B;
  white-space: nowrap;
}

.major-select {
  width: 320px;
}

/* ===== Card Styles ===== */
.main-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.main-card :deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #F1F5F9;
}

.main-card :deep(.el-card__body) {
  padding: 24px;
}

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

/* ===== Collapse ===== */
.req-collapse {
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
}

.req-item :deep(.el-collapse-item__header) {
  background: #F8FAFC;
  padding: 0 20px;
}

.req-item :deep(.el-collapse-item__content) {
  padding: 20px;
}

.req-header {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.req-number {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7C3AED;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.req-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.req-title {
  font-weight: 600;
  font-size: 15px;
  color: #1E293B;
}

.req-indicator-count {
  font-size: 12px;
  color: #64748B;
}

.req-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.req-content {
  color: #475569;
  margin: 0 0 20px 0;
  line-height: 1.7;
  padding: 16px;
  background: #F8FAFC;
  border-radius: 8px;
  border: 1px solid #F1F5F9;
}

/* ===== Indicator ===== */
.indicator-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.indicator-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}

.indicator-table {
  border-radius: 8px;
  overflow: hidden;
}

.indicator-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
}

.indicator-no {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #7C3AED;
}

.indicator-content {
  color: #1E293B;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 40px 0;
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

  .filter-item {
    flex-direction: column;
    align-items: stretch;
  }

  .major-select {
    width: 100%;
  }

  .req-header {
    flex-wrap: wrap;
  }

  .req-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .req-item :deep(.el-collapse-item__header) {
    transition: none;
  }
}
</style>

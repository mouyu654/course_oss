<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #DC2626 0%, #EF4444 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">成绩勘误申请</h3>
              <p class="header-subtitle">提交成绩修改申请，等待教务管理员审批</p>
            </div>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" class="class-selector" @change="onClassChange">
              <el-option v-for="c in myClasses" :key="c.id" :label="`${c.courseName} - ${c.className}`" :value="c.id" />
            </el-select>
            <el-tag
              v-if="scoreStatus"
              :type="scoreStatus === 'LOCKED' ? 'success' : scoreStatus === 'IMPORTED' ? 'warning' : 'info'"
              size="default"
              class="status-tag"
              effect="dark"
            >
              <el-icon class="status-icon"><component :is="statusIcon" /></el-icon>
              {{ { EMPTY: '未录入', IMPORTED: '已导入', LOCKED: '已锁定' }[scoreStatus] || scoreStatus }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-alert
        v-if="scoreStatus && scoreStatus !== 'LOCKED'"
        title="成绩尚未锁定，无需勘误。请先在成绩录入页面完成导入和计算。"
        type="info"
        show-icon
        :closable="false"
        class="info-alert"
      />

      <!-- 提交勘误申请 -->
      <div v-if="scoreStatus === 'LOCKED'" class="request-form-card">
        <div class="form-card-header">
          <div class="form-card-icon">
            <el-icon><EditPen /></el-icon>
          </div>
          <div class="form-card-content">
            <h4 class="form-card-title">提交勘误申请</h4>
            <p class="form-card-desc">填写成绩修改原因，提交后等待审批</p>
          </div>
        </div>
        <el-form :model="form" label-width="100px" class="request-form">
          <el-form-item label="教学班级">
            <span class="class-name">{{ selectedClassName }}</span>
          </el-form-item>
          <el-form-item label="申请原因" required>
            <el-input
              v-model="form.reason"
              type="textarea"
              :rows="3"
              placeholder="请说明需要修改成绩的原因，例如：某某同学的期末大题为录入错误，正确得分为XX分。"
              maxlength="500"
              show-word-limit
              class="reason-textarea"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!form.reason.trim()"
              @click="handleSubmit"
              class="submit-button"
            >
              <el-icon><Promotion /></el-icon>
              提交申请
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 申请记录 -->
      <div v-if="selectedClassId" class="records-section">
        <div class="section-header">
          <div class="section-icon">
            <el-icon><Document /></el-icon>
          </div>
          <h4 class="section-title">申请记录</h4>
          <el-tag type="info" size="small" effect="plain" class="count-tag">{{ requests.length }}</el-tag>
        </div>
        <el-table :data="requests" v-loading="loadingRequests" stripe border size="small" class="records-table">
          <el-table-column prop="id" label="编号" width="70" align="center">
            <template #default="{ row }">
              <span class="request-id">#{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="申请原因" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="reason-text">{{ row.reason }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small" effect="dark" class="status-badge">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ row.createdAt }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewedAt" label="审批时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ row.reviewedAt || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                link
                type="danger"
                @click="handleCancel(row)"
                class="cancel-button"
              >
                <el-icon><CircleClose /></el-icon>
                撤销
              </el-button>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loadingRequests && requests.length === 0" description="暂无勘误申请" class="empty-records" />
      </div>

      <div v-if="!selectedClassId" class="empty-state">
        <el-empty description="请先选择教学班级" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyClasses, getScoreStatus, requestUnlock, getMyUnlockRequests, cancelUnlockRequest } from '@/api/teacher'
import { CircleCheck, Clock, Warning, EditPen, Document, Promotion, CircleClose } from '@element-plus/icons-vue'

const myClasses = ref([])
const selectedClassId = ref(null)
const scoreStatus = ref('')
const loadingRequests = ref(false)
const submitting = ref(false)
const requests = ref([])

const form = ref({ reason: '' })

const selectedClassName = computed(() => {
  const c = myClasses.value.find(c => c.id === selectedClassId.value)
  return c ? `${c.courseName} - ${c.className}` : ''
})

const statusIcon = computed(() => {
  const map = { EMPTY: Clock, IMPORTED: Warning, LOCKED: CircleCheck }
  return map[scoreStatus.value] || Clock
})

onMounted(async () => {
  const res = await getMyClasses()
  myClasses.value = res.data || res || []
})

async function onClassChange() {
  form.value.reason = ''
  requests.value = []
  if (!selectedClassId.value) return
  try {
    const s = await getScoreStatus(selectedClassId.value)
    scoreStatus.value = s?.data?.status || s?.status || 'EMPTY'
  } catch {
    scoreStatus.value = ''
  }
  loadRequests()
}

async function loadRequests() {
  if (!selectedClassId.value) return
  loadingRequests.value = true
  try {
    const res = await getMyUnlockRequests(selectedClassId.value)
    requests.value = res?.data || res || []
  } catch {
    requests.value = []
  } finally {
    loadingRequests.value = false
  }
}

async function handleSubmit() {
  if (!form.value.reason.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }
  await ElMessageBox.confirm('确认提交勘误申请？提交后需等待教务管理员审批。', '确认', { type: 'warning' })
  submitting.value = true
  try {
    await requestUnlock(selectedClassId.value, form.value.reason.trim())
    ElMessage.success('勘误申请已提交，请等待审批')
    form.value.reason = ''
    loadRequests()
  } finally {
    submitting.value = false
  }
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确认撤销该勘误申请？', '确认', { type: 'warning' })
  try {
    await cancelUnlockRequest(selectedClassId.value, row.id)
    ElMessage.success('已撤销')
    loadRequests()
  } catch { /* handled */ }
}

function statusLabel(status) {
  const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', UNLOCKED: '已解锁' }
  return map[status] || status
}
function statusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', UNLOCKED: '' }
  return map[status] || 'info'
}
</script>

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

.class-selector {
  width: 280px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
}

.status-icon {
  font-size: 16px;
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

/* ===== Info Alert ===== */
.info-alert {
  margin-bottom: 24px;
  border-radius: 8px;
}

/* ===== Request Form Card ===== */
.request-form-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.form-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F1F5F9;
}

.form-card-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563EB;
  font-size: 24px;
}

.form-card-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1E293B;
}

.form-card-desc {
  margin: 0;
  font-size: 13px;
  color: #64748B;
}

.request-form {
  max-width: 600px;
}

.class-name {
  font-weight: 500;
  color: #1E293B;
}

.reason-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.submit-button {
  background: linear-gradient(135deg, #2563EB 0%, #3B82F6 100%);
  border: none;
  padding: 10px 24px;
}

.submit-button:hover {
  background: linear-gradient(135deg, #1D4ED8 0%, #2563EB 100%);
}

/* ===== Records Section ===== */
.records-section {
  margin-top: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-icon {
  width: 32px;
  height: 32px;
  background: #F1F5F9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748B;
  font-size: 18px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.count-tag {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.records-table {
  border-radius: 8px;
  overflow: hidden;
}

.records-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}

.request-id {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #64748B;
}

.reason-text {
  color: #1E293B;
  line-height: 1.5;
}

.status-badge {
  border-radius: 6px;
  font-weight: 600;
}

.time-text {
  font-size: 13px;
  color: #64748B;
}

.cancel-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-action {
  color: #CBD5E1;
}

.empty-records {
  padding: 40px 0;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 60px 0;
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

  .class-selector {
    width: 100%;
  }

  .status-tag {
    justify-content: center;
  }

  .form-card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .submit-button {
    transition: none;
  }
}
</style>

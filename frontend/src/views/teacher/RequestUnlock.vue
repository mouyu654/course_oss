<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px">
          <h3 style="margin:0;font-size:16px">成绩勘误申请</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" style="width:280px" @change="onClassChange">
              <el-option v-for="c in myClasses" :key="c.id" :label="`${c.courseName} - ${c.className}`" :value="c.id" />
            </el-select>
            <el-tag v-if="scoreStatus" :type="scoreStatus === 'LOCKED' ? 'success' : scoreStatus === 'IMPORTED' ? 'warning' : 'info'" size="default">
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
        style="margin-bottom:16px"
      />

      <!-- 提交勘误申请 -->
      <el-card v-if="scoreStatus === 'LOCKED'" shadow="never" style="margin-bottom:16px;border:1px solid #e4e7ed">
        <template #header><span style="font-weight:600">提交勘误申请</span></template>
        <el-form :model="form" label-width="100px" style="max-width:600px">
          <el-form-item label="教学班级">
            <span>{{ selectedClassName }}</span>
          </el-form-item>
          <el-form-item label="申请原因" required>
            <el-input
              v-model="form.reason"
              type="textarea"
              :rows="3"
              placeholder="请说明需要修改成绩的原因，例如：某某同学的期末大题为录入错误，正确得分为XX分。"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" :disabled="!form.reason.trim()" @click="handleSubmit">
              提交申请
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 申请记录 -->
      <div v-if="selectedClassId">
        <h4 style="margin:16px 0 12px;font-size:14px;color:#303133">申请记录</h4>
        <el-table :data="requests" v-loading="loadingRequests" stripe border size="small" style="width:100%">
          <el-table-column prop="id" label="编号" width="70" align="center" />
          <el-table-column prop="reason" label="申请原因" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="170" />
          <el-table-column prop="reviewedAt" label="审批时间" width="170">
            <template #default="{ row }">{{ row.reviewedAt || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                link
                type="danger"
                @click="handleCancel(row)"
              >
                撤销
              </el-button>
              <span v-else style="color:#909399;font-size:13px">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loadingRequests && requests.length === 0" description="暂无勘误申请" />
      </div>

      <el-empty v-if="!selectedClassId" description="请先选择教学班级" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyClasses, getScoreStatus, requestUnlock, getMyUnlockRequests, cancelUnlockRequest } from '@/api/teacher'

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

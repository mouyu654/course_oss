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
            @change="loadData"
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

    <!-- 课程关联表格 -->
    <el-card class="main-card">
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <div class="header-accent" style="background: linear-gradient(180deg, #2563EB 0%, #3B82F6 100%);"></div>
            <div class="header-content">
              <h3 class="header-title">专业课程关联</h3>
              <p class="header-subtitle">配置专业与课程的关联关系</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="selected-info" v-if="selectedMajorId">
              <el-icon><InfoFilled /></el-icon>
              已选 <strong>{{ selectedCount }}</strong> 门课程
            </div>
            <el-button
              type="primary"
              :disabled="!selectedMajorId"
              :loading="saving"
              @click="handleSave"
            >
              <el-icon><Check /></el-icon>
              保存关联
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="selectedMajorId">
        <el-table
          ref="tableRef"
          :data="courseList"
          v-loading="loading"
          stripe
          border
          class="course-table"
          max-height="550"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column prop="code" label="课程代码" width="120" align="center">
            <template #default="{ row }">
              <span class="code-text">{{ row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="课程名称" min-width="180">
            <template #default="{ row }">
              <span class="course-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="credit" label="学分" width="80" align="center">
            <template #default="{ row }">
              <span class="credit-text">{{ row.credit }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-empty v-else description="请先选择专业" class="empty-state" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getMajors } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const majorOptions = ref([])
const selectedMajorId = ref(null)
const courseList = ref([])
const loading = ref(false)
const saving = ref(false)
const tableRef = ref(null)
const selectedCourses = ref([])

const selectedCount = ref(0)

onMounted(async () => {
  const res = await getMajors()
  majorOptions.value = res.data?.records || res.data || []
})

async function loadData() {
  if (!selectedMajorId.value) return
  loading.value = true
  try {
    const res = await request({ url: '/courses/major-courses', method: 'get', params: { majorId: selectedMajorId.value } })
    const data = res.data || res
    courseList.value = data.courses || []
    await nextTick()
    // Toggle selection for already-linked courses
    setTimeout(() => {
      courseList.value.forEach(c => {
        if (c.linked) tableRef.value?.toggleRowSelection(c, true)
      })
      selectedCount.value = courseList.value.filter(c => c.linked).length
    }, 100)
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val) {
  selectedCourses.value = val
  selectedCount.value = val.length
}

async function handleSave() {
  saving.value = true
  try {
    const courseIds = selectedCourses.value.map(c => c.id)
    await request({
      url: '/courses/major-courses',
      method: 'put',
      params: { majorId: selectedMajorId.value },
      data: courseIds
    })
    ElMessage.success(`已关联 ${courseIds.length} 门课程`)
    loadData()
  } finally {
    saving.value = false
  }
}
</script>

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
  gap: 16px;
  flex-shrink: 0;
}

.selected-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2563EB;
  font-size: 14px;
}

.selected-info strong {
  font-weight: 700;
}

/* ===== Table ===== */
.course-table {
  border-radius: 8px;
  overflow: hidden;
}

.course-table :deep(.el-table__header th) {
  background: #F8FAFC;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.course-table :deep(.el-table__row:hover > td) {
  background: #F8FAFC !important;
}

.code-text {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
  color: #2563EB;
}

.course-name {
  font-weight: 500;
  color: #1E293B;
}

.credit-text {
  font-weight: 600;
  color: #059669;
  font-family: 'SF Mono', 'Consolas', monospace;
}

/* ===== Empty State ===== */
.empty-state {
  padding: 40px 0;
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
}

/* ===== Reduced Motion ===== */
@media (prefers-reduced-motion: reduce) {
  .header-accent,
  .course-table :deep(.el-table__row) {
    transition: none;
  }
}
</style>

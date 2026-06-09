<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px">
          <h3 style="margin:0;font-size:16px">专业课程关联</h3>
          <div style="display:flex;gap:12px;align-items:center">
            <el-select v-model="selectedMajorId" placeholder="选择专业" filterable style="width:240px" @change="loadData">
              <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedMajorId" :loading="saving" @click="handleSave">保存关联</el-button>
          </div>
        </div>
      </template>

      <div v-if="selectedMajorId">
        <div style="margin-bottom:12px;font-size:14px;color:#606266">
          已选 <b style="color:#409eff">{{ selectedCount }}</b> 门课程
        </div>
        <el-table ref="tableRef" :data="courseList" v-loading="loading" stripe border size="small" style="width:100%" max-height="550"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column prop="code" label="课程代码" width="120" />
          <el-table-column prop="name" label="课程名称" min-width="180" />
          <el-table-column prop="credit" label="学分" width="80" align="center" />
        </el-table>
      </div>

      <el-empty v-else description="请先选择专业" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getMajors } from '@/api/admin'
import { ElMessage } from 'element-plus'
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

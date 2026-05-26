<template>
  <div class="my-courses">
    <div class="page-header">
      <h2>我的教学空间</h2>
      <el-tag type="info" size="small">{{ auth.displayName }}</el-tag>
    </div>

    <el-alert
 v-if="!loading && classes.length === 0"
      title="暂无教学班"
      description="您当前没有被分配到任何教学班，请联系教务管理员进行分配。"
      type="warning"
      show-icon
      :closable="false"
    />

    <div v-loading="loading" class="class-grid">
      <el-card
        v-for="cls in classes"
        :key="cls.id"
        class="class-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <span class="course-name">{{ cls.course_name }}</span>
            <el-tag :type="cls.status === 'locked' ? 'success' : 'warning'" size="small">
              {{ cls.status === 'locked' ? '已锁定' : '待计算' }}
            </el-tag>
          </div>
        </template>

        <div class="card-body">
          <div class="info-row">
            <span class="label">班级</span>
            <span>{{ cls.class_name }}</span>
          </div>
          <div class="info-row">
            <span class="label">课程代码</span>
            <span>{{ cls.course_code }}</span>
          </div>
          <div class="info-row">
            <span class="label">学年学期</span>
            <span>{{ cls.academic_year }} - 第{{ cls.semester }}学期</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button
            type="primary"
            size="small"
            :disabled="cls.status === 'locked'"
            @click="goSyllabus(cls)"
          >
            课程目标与权重
          </el-button>
          <el-button
            type="default"
            size="small"
            :disabled="cls.status === 'locked'"
            @click="goAssessment(cls)"
          >
            考核点配置
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { getClasses } from '@/api/academic.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

const classes = ref([])
const loading = ref(false)

onMounted(() => {
  fetchClasses()
})

async function fetchClasses() {
  loading.value = true
  try {
    const res = await getClasses()
    const raw = Array.isArray(res.data) ? res.data : []
    classes.value = raw.map((c) => ({
      ...c,
      status: c.locked ? 'locked' : 'pending',
    }))
  } catch {
    ElMessage.error('获取教学班列表失败')
  } finally {
    loading.value = false
  }
}

function goSyllabus(cls) {
  router.push({
    path: '/syllabus-config',
    query: { course_id: cls.course_id, class_id: cls.id },
  })
}

function goAssessment(cls) {
  router.push({
    path: '/assessment-mapping',
    query: { course_id: cls.course_id, class_id: cls.id },
  })
}
</script>

<style scoped>
.my-courses {
  max-width: 960px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 600;
}
.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}
.class-card {
  border: 1px solid #e5e6eb;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.course-name {
  font-weight: 600;
  font-size: 15px;
}
.card-body {
  margin-bottom: 16px;
}
.info-row {
  display: flex;
  margin-bottom: 6px;
  font-size: 13px;
  color: #4e5969;
}
.info-row .label {
  width: 70px;
  flex-shrink: 0;
  color: #86909c;
}
.card-actions {
  display: flex;
  gap: 12px;
}
</style>

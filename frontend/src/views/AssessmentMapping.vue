<template>
  <div class="assessment-mapping">
    <div class="page-header">
      <h2>考核点追溯板</h2>
    </div>

    <!-- 课程选择 -->
    <div class="course-bar">
      <span class="bar-label">当前课程：</span>
      <el-select v-model="courseId" placeholder="请选择课程" @change="onCourseChange">
        <el-option
          v-for="c in courseOptions"
          :key="c.course_id"
          :label="`${c.course_name} (${c.class_name})`"
          :value="c.course_id"
        />
      </el-select>
    </div>

    <template v-if="courseId">
      <div class="tab-toolbar">
        <el-button type="primary" size="small" :disabled="objectives.length === 0" @click="openItemDialog()">
          + 新增考核点
        </el-button>
        <span v-if="objectives.length === 0" class="hint-text">
          请先在「大纲映射器」中设定课程目标
        </span>
      </div>

      <el-table :data="items" border stripe v-loading="loading" empty-text="暂无考核点">
        <el-table-column prop="name" label="考核点名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="max_score" label="满分分值" width="100" align="center" />
        <el-table-column label="绑定课程目标" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ objectiveLabel(row.objective_id) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="deleteItem(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-empty v-if="!courseId" description="请先选择一门课程" />

    <!-- 考核点弹窗 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="editingItem ? '编辑考核点' : '新增考核点'"
      width="500px"
      @closed="resetItemForm"
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="110px">
        <el-form-item label="考核点名称" prop="name">
          <el-input v-model="itemForm.name" placeholder="如 在线作业(目标1-1)" />
        </el-form-item>
        <el-form-item label="满分分值" prop="max_score">
          <el-input-number
            v-model="itemForm.max_score"
            :min="0.01"
            :max="9999"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="绑定课程目标" prop="objective_id">
          <el-select v-model="itemForm.objective_id" placeholder="请选择课程目标" style="width: 100%">
            <el-option
              v-for="obj in objectives"
              :key="obj.id"
              :label="`${obj.code} - ${obj.description}`"
              :value="obj.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClasses } from '@/api/academic.js'
import {
  getObjectives,
  getAssessmentItems,
  createAssessmentItem,
  deleteAssessmentItem,
} from '@/api/teacher.js'

const route = useRoute()
const courseId = ref(route.query.course_id || '')

// ---- 课程选择 ----
const courseOptions = ref([])

async function loadCourseOptions() {
  try {
    const res = await getClasses()
    courseOptions.value = Array.isArray(res.data) ? res.data : []
    if (!courseId.value && courseOptions.value.length > 0) {
      courseId.value = courseOptions.value[0].course_id
    }
  } catch {
    courseOptions.value = []
  }
}

onMounted(() => {
  loadCourseOptions().then(() => {
    if (courseId.value) {
      loadObjectives()
      loadItems()
    }
  })
})

function onCourseChange() {
  loadObjectives()
  loadItems()
}

// ---- 课程目标（用于下拉绑定） ----
const objectives = ref([])

async function loadObjectives() {
  if (!courseId.value) return
  try {
    const res = await getObjectives(courseId.value)
    objectives.value = Array.isArray(res.data) ? res.data : []
  } catch {
    objectives.value = []
  }
}

function objectiveLabel(id) {
  const obj = objectives.value.find((o) => o.id === id)
  return obj ? `${obj.code} ${obj.description}` : id
}

// ---- 考核点 ----
const items = ref([])
const loading = ref(false)
const itemSubmitting = ref(false)
const itemDialogVisible = ref(false)
const editingItem = ref(null)
const itemFormRef = ref(null)

const itemForm = reactive({
  name: '',
  max_score: 100,
  objective_id: '',
})

const itemRules = {
  name: [{ required: true, message: '请输入考核点名称', trigger: 'blur' }],
  max_score: [{ required: true, message: '请输入满分分值', trigger: 'blur' }],
  objective_id: [{ required: true, message: '请选择课程目标', trigger: 'change' }],
}

async function loadItems() {
  if (!courseId.value) return
  loading.value = true
  try {
    const res = await getAssessmentItems(courseId.value)
    items.value = Array.isArray(res.data) ? res.data : []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function openItemDialog() {
  editingItem.value = null
  itemForm.name = ''
  itemForm.max_score = 100
  itemForm.objective_id = objectives.value.length === 1 ? objectives.value[0].id : ''
  itemDialogVisible.value = true
}

function resetItemForm() {
  itemFormRef.value?.resetFields()
}

async function saveItem() {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSubmitting.value = true
  try {
    await createAssessmentItem({
      course_id: courseId.value,
      name: itemForm.name,
      max_score: itemForm.max_score,
      objective_id: itemForm.objective_id,
    })
    ElMessage.success('考核点创建成功')
    itemDialogVisible.value = false
    await loadItems()
  } catch {
    // handled by interceptor
  } finally {
    itemSubmitting.value = false
  }
}

async function deleteItem(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除考核点「${row.name}」？若已有学生成绩将无法删除。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteAssessmentItem(row.id)
    ElMessage.success('删除成功')
    await loadItems()
  } catch {
    // handled by interceptor
  }
}
</script>

<style scoped>
.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}
.course-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8f9fb;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
}
.bar-label {
  font-weight: 500;
  color: #4e5969;
  white-space: nowrap;
}
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.hint-text {
  color: #86909c;
  font-size: 13px;
}
</style>

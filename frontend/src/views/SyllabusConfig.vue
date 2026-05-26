<template>
  <div class="syllabus-config">
    <div class="page-header">
      <h2>大纲微观映射器</h2>
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
      <span v-if="courseId" class="class-hint">班级：{{ selectedClass?.class_name }}</span>
    </div>

    <el-tabs v-model="activeTab" type="border-card" v-if="courseId">
      <!-- Tab 1: 课程目标 -->
      <el-tab-pane label="课程目标设定" name="objectives">
        <div class="tab-toolbar">
          <el-button type="primary" size="small" @click="openObjDialog()">
            + 新增课程目标
          </el-button>
        </div>

        <el-table :data="objectives" border stripe v-loading="objLoading" empty-text="暂无课程目标">
          <el-table-column prop="code" label="编号" width="80" />
          <el-table-column prop="description" label="目标描述" min-width="260" show-overflow-tooltip />
          <el-table-column prop="dimension" label="维度" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.dimension }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" size="small" text @click="deleteObjective(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Tab 2: 内部权重矩阵 -->
      <el-tab-pane label="内部权重矩阵" name="weights">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="weight-hint"
        >
          本课程支撑的指标点来自宏观矩阵配置。请确保每个指标点列的权重之和等于 1.0（容差 &plusmn;0.001），否则无法提交。
        </el-alert>

        <div v-if="weightIndicators.length === 0" class="empty-hint">
          该课程暂未在宏观矩阵中配置支撑指标点。请联系专业负责人先完成宏观支撑矩阵配置。
        </div>

        <div v-else class="matrix-wrap">
          <el-table :data="weightRows" border stripe v-loading="weightLoading" class="weight-table">
            <el-table-column label="课程目标" width="140" fixed="left">
              <template #default="{ row }">
                <span class="obj-label">{{ row.objectiveCode }}</span>
              </template>
            </el-table-column>
            <el-table-column
              v-for="ind in weightIndicators"
              :key="ind.id"
              :label="ind.code"
              width="120"
              align="center"
            >
              <template #header>
                <div :class="['col-header', indicatorError(ind.id) ? 'col-error' : '']">
                  <span>{{ ind.code }}</span>
                </div>
              </template>
              <template #default="{ row }">
                <el-input
                  v-model="weightModel[row.objectiveId][ind.id]"
                  size="small"
                  placeholder="0.00"
                  @input="onWeightInput"
                />
              </template>
            </el-table-column>
          </el-table>

          <!-- 列合计行 -->
          <div class="sum-row">
            <span class="sum-label">列合计</span>
            <div class="sum-cells">
              <div
                v-for="ind in weightIndicators"
                :key="ind.id"
                :class="['sum-cell', indicatorError(ind.id) ? 'sum-error' : 'sum-ok']"
              >
                {{ columnSum(ind.id).toFixed(4) }}
              </div>
            </div>
          </div>

          <div class="weight-actions">
            <el-button
              type="primary"
              :disabled="!weightValid || weightSubmitting"
              :loading="weightSubmitting"
              @click="submitWeights"
            >
              提交权重矩阵
            </el-button>
            <span v-if="!weightValid" class="error-text">请确保每列权重之和为 1.0</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 空状态 -->
    <el-empty v-if="!courseId" description="请先选择一门课程" />

    <!-- 课程目标弹窗 -->
    <el-dialog
      v-model="objDialogVisible"
      :title="editingObjective ? '编辑课程目标' : '新增课程目标'"
      width="520px"
      @closed="resetObjForm"
    >
      <el-form ref="objFormRef" :model="objForm" :rules="objRules" label-width="100px">
        <el-form-item label="目标编号" prop="code">
          <el-input v-model="objForm.code" placeholder="如 1-1" />
        </el-form-item>
        <el-form-item label="目标描述" prop="description">
          <el-input v-model="objForm.description" type="textarea" :rows="3" placeholder="请描述课程目标" />
        </el-form-item>
        <el-form-item label="目标维度" prop="dimension">
          <el-select v-model="objForm.dimension" placeholder="请选择">
            <el-option label="知识维度" value="知识维度" />
            <el-option label="能力维度" value="能力维度" />
            <el-option label="价值维度" value="价值维度" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="objDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="objSubmitting" @click="saveObjective">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClasses } from '@/api/academic.js'
import { getMacroMatrix } from '@/api/director.js'
import {
  getObjectives,
  createObjective,
  deleteObjective,
  getInternalWeights,
  submitInternalWeights,
} from '@/api/teacher.js'

const TOLERANCE = 0.001

const route = useRoute()
const courseId = ref(route.query.course_id || '')
const activeTab = ref('objectives')

// ---- 课程选择 ----
const courseOptions = ref([])

const selectedClass = computed(() => {
  return courseOptions.value.find((c) => c.course_id === courseId.value) || null
})

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
      loadWeightData()
    }
  })
})

function onCourseChange() {
  loadObjectives()
  loadWeightData()
}

// ---- 课程目标 ----
const objectives = ref([])
const objLoading = ref(false)
const objSubmitting = ref(false)
const objDialogVisible = ref(false)
const editingObjective = ref(null)
const objFormRef = ref(null)

const objForm = reactive({
  code: '',
  description: '',
  dimension: '',
})

const objRules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  dimension: [{ required: true, message: '请选择维度', trigger: 'change' }],
}

async function loadObjectives() {
  if (!courseId.value) return
  objLoading.value = true
  try {
    const res = await getObjectives(courseId.value)
    objectives.value = Array.isArray(res.data) ? res.data : []
  } catch {
    objectives.value = []
  } finally {
    objLoading.value = false
  }
}

function openObjDialog() {
  editingObjective.value = null
  objForm.code = ''
  objForm.description = ''
  objForm.dimension = ''
  objDialogVisible.value = true
}

function resetObjForm() {
  objFormRef.value?.resetFields()
}

async function saveObjective() {
  const valid = await objFormRef.value.validate().catch(() => false)
  if (!valid) return
  objSubmitting.value = true
  try {
    await createObjective({
      course_id: courseId.value,
      code: objForm.code,
      description: objForm.description,
      dimension: objForm.dimension,
    })
    ElMessage.success('课程目标创建成功')
    objDialogVisible.value = false
    await loadObjectives()
    await loadWeightData() // refresh weight matrix rows as well
  } catch {
    // handled by interceptor
  } finally {
    objSubmitting.value = false
  }
}

async function deleteObjective(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除课程目标「${row.code}」？若存在关联考核点或内部权重将无法删除。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteObjective(row.id)
    ElMessage.success('删除成功')
    await loadObjectives()
    await loadWeightData()
  } catch {
    // handled by interceptor
  }
}

// ---- 内部权重矩阵 ----
const weightIndicators = ref([])
const weightModel = reactive({})
const weightLoading = ref(false)
const weightSubmitting = ref(false)
const weightDirty = ref(false)

const weightRows = computed(() => {
  return objectives.value.map((o) => ({
    objectiveId: o.id,
    objectiveCode: o.code,
  }))
})

function indicatorError(indicatorId) {
  if (!weightDirty.value) return false
  return Math.abs(columnSum(indicatorId) - 1.0) > TOLERANCE
}

const weightValid = computed(() => {
  if (weightIndicators.value.length === 0) return false
  return weightIndicators.value.every((ind) => !indicatorError(ind.id))
})

function columnSum(indicatorId) {
  let sum = 0
  for (const obj of objectives.value) {
    const val = parseFloat(weightModel[obj.id]?.[indicatorId])
    if (!isNaN(val)) sum += val
  }
  return sum
}

function onWeightInput() {
  weightDirty.value = true
}

async function loadWeightData() {
  if (!courseId.value) return
  weightLoading.value = true
  weightDirty.value = false
  try {
    // 取该课程支撑的指标点
    const matrixRes = await getMacroMatrix()
    const matrixData = Array.isArray(matrixRes.data?.matrix) ? matrixRes.data.matrix : Array.isArray(matrixRes.data) ? matrixRes.data : []
    const supportedIndicators = matrixData
      .filter((m) => m.course_id === courseId.value)
      .map((m) => ({ id: m.indicator_id, code: m.indicator_code || m.indicator_id }))

    weightIndicators.value = supportedIndicators

    // 初始化权重模型
    for (const obj of objectives.value) {
      if (!weightModel[obj.id]) {
        weightModel[obj.id] = reactive({})
      }
      for (const ind of supportedIndicators) {
        if (!(ind.id in weightModel[obj.id])) {
          weightModel[obj.id][ind.id] = ''
        }
      }
    }

    // 预填已有权重
    const wRes = await getInternalWeights(courseId.value)
    const wData = Array.isArray(wRes.data) ? wRes.data : []
    for (const w of wData) {
      if (weightModel[w.objective_id]) {
        weightModel[w.objective_id][w.indicator_id] = String(w.weight_w)
      }
    }
  } catch {
    weightIndicators.value = []
  } finally {
    weightLoading.value = false
  }
}

async function submitWeights() {
  if (!weightValid.value) return
  weightSubmitting.value = true
  try {
    const payload = []
    for (const obj of objectives.value) {
      for (const ind of weightIndicators.value) {
        const val = parseFloat(weightModel[obj.id]?.[ind.id])
        if (!isNaN(val) && val > 0) {
          payload.push({
            objective_id: obj.id,
            indicator_id: ind.id,
            weight_w: val,
          })
        }
      }
    }
    await submitInternalWeights(courseId.value, payload)
    ElMessage.success('内部权重矩阵提交成功')
    weightDirty.value = false
  } catch {
    // handled by interceptor
  } finally {
    weightSubmitting.value = false
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
.class-hint {
  color: #86909c;
  font-size: 13px;
}
.tab-toolbar {
  margin-bottom: 12px;
}
.weight-hint {
  margin-bottom: 16px;
}
.empty-hint {
  color: #86909c;
  padding: 40px 0;
  text-align: center;
}
.matrix-wrap {
  overflow-x: auto;
}
.weight-table {
  margin-bottom: 0;
}
.obj-label {
  font-weight: 500;
  font-size: 13px;
}
.col-header {
  font-size: 12px;
}
.col-error {
  color: #f53f3f;
}
.sum-row {
  display: flex;
  align-items: center;
  margin-top: 8px;
  padding: 4px 0;
  border-top: 2px solid #e5e6eb;
}
.sum-label {
  width: 140px;
  flex-shrink: 0;
  padding-left: 12px;
  font-weight: 600;
  font-size: 13px;
  color: #1d2129;
}
.sum-cells {
  display: flex;
  gap: 0;
}
.sum-cell {
  width: 120px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 0;
}
.sum-ok {
  color: #00b42a;
}
.sum-error {
  color: #f53f3f;
}
.weight-actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.error-text {
  color: #f53f3f;
  font-size: 13px;
}
.el-tabs {
  min-height: 360px;
}
</style>

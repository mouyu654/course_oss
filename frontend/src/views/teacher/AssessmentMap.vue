<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import {
  getMyClasses,
  getAssessments,
  createAssessment,
  updateAssessment,
  deleteAssessment,
  getObjectives
} from '@/api/teacher'
import { ElMessage, ElMessageBox } from 'element-plus'

/* ---- state ---- */
const myClasses = ref([])
const selectedClassId = ref(null)
const loading = ref(false)
const assessments = ref([])
const objectives = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', maxScore: null, objectiveId: null, sortOrder: 0 })

const formRules = {
  name: [{ required: true, message: '请输入考核点名称', trigger: 'blur' }],
  maxScore: [{ required: true, type: 'number', min: 0.01, message: '满分必须大于 0', trigger: 'blur' }],
  objectiveId: [{ required: true, message: '请选择绑定的课程目标', trigger: 'change' }],
  sortOrder: [{ required: true, type: 'number', min: 0, message: '排序号不能为负', trigger: 'blur' }]
}

/* ---- lifecycle ---- */
onMounted(async () => {
  const res = await getMyClasses()
  myClasses.value = res.data || []
  if (myClasses.value.length) {
    selectedClassId.value = myClasses.value[0].id
  }
})

watch(selectedClassId, (v) => { if (v) loadData() })

/* ---- data loading ---- */
async function loadData() {
  if (!selectedClassId.value) return
  loading.value = true
  try {
    const [assessRes, objRes] = await Promise.all([
      getAssessments(selectedClassId.value),
      getObjectives(selectedClassId.value)
    ])
    assessments.value = (assessRes.data || []).sort((a, b) => a.sortOrder - b.sortOrder)
    objectives.value = objRes.data || []
  } finally {
    loading.value = false
  }
}

/* ---- helpers ---- */
function objectiveLabel(objectiveId) {
  const obj = objectives.value.find(o => o.id === objectiveId)
  return obj ? obj.objNo : '-'
}

/* ---- dialog controls ---- */
function handleAdd() {
  isEdit.value = false
  form.value = { name: '', maxScore: null, objectiveId: null, sortOrder: assessments.value.length }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该考核点？', '提示', { type: 'warning' })
  await deleteAssessment(selectedClassId.value, row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  const payload = {
    name: form.value.name,
    maxScore: form.value.maxScore,
    objectiveId: form.value.objectiveId,
    sortOrder: form.value.sortOrder
  }
  if (isEdit.value) {
    await updateAssessment(selectedClassId.value, form.value.id, payload)
    ElMessage.success('修改成功')
  } else {
    await createAssessment(selectedClassId.value, payload)
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
          <h3>考核点细分与映射</h3>
          <div style="display: flex; gap: 12px; align-items: center;">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" style="width: 280px;">
              <el-option
                v-for="c in myClasses"
                :key="c.id"
                :label="`${c.courseName} - ${c.className}`"
                :value="c.id"
              />
            </el-select>
            <el-button type="primary" :disabled="!selectedClassId" @click="handleAdd">新增考核点</el-button>
          </div>
        </div>
      </template>

      <el-table :data="assessments" v-loading="loading" stripe>
        <el-table-column prop="sortOrder" label="序号" width="80" align="center" />
        <el-table-column prop="name" label="考核点名称" show-overflow-tooltip />
        <el-table-column prop="maxScore" label="满分" width="100" align="center" />
        <el-table-column label="绑定课程目标" width="140" align="center">
          <template #default="{ row }">
            {{ objectiveLabel(row.objectiveId) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- add / edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑考核点' : '新增考核点'"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 期末大题1" maxlength="100" />
        </el-form-item>
        <el-form-item label="满分分值" prop="maxScore">
          <el-input-number v-model="form.maxScore" :min="0.01" :max="1000" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="绑定目标" prop="objectiveId">
          <el-select v-model="form.objectiveId" placeholder="选择课程目标" style="width: 100%;">
            <el-option
              v-for="obj in objectives"
              :key="obj.id"
              :label="`${obj.objNo} ${obj.description}`"
              :value="obj.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 {
  margin: 0;
  font-size: 16px;
}
</style>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { getMyClasses, getObjectives, createObjective, updateObjective, deleteObjective } from '@/api/teacher'
import { ElMessage, ElMessageBox } from 'element-plus'

/* ---- state ---- */
const myClasses = ref([])
const selectedClassId = ref(null)
const loading = ref(false)
const objectives = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ objNo: '', dimension: '', description: '' })

const formRules = {
  objNo: [{ required: true, message: '请输入目标编号', trigger: 'blur' }],
  dimension: [{ required: true, message: '请选择维度', trigger: 'change' }],
  description: [{ required: true, message: '请输入目标描述', trigger: 'blur' }]
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
    const res = await getObjectives(selectedClassId.value)
    objectives.value = res.data || []
  } finally {
    loading.value = false
  }
}

/* ---- dialog controls ---- */
function handleAdd() {
  isEdit.value = false
  form.value = { objNo: '', dimension: '', description: '' }
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
  await ElMessageBox.confirm('确认删除该课程目标？', '提示', { type: 'warning' })
  await deleteObjective(selectedClassId.value, row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (isEdit.value) {
    await updateObjective(selectedClassId.value, form.value.id, {
      objNo: form.value.objNo,
      dimension: form.value.dimension,
      description: form.value.description
    })
    ElMessage.success('修改成功')
  } else {
    await createObjective(selectedClassId.value, {
      objNo: form.value.objNo,
      dimension: form.value.dimension,
      description: form.value.description
    })
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
          <h3>课程目标设定</h3>
          <div style="display: flex; gap: 12px; align-items: center;">
            <el-select v-model="selectedClassId" placeholder="选择教学班级" style="width: 280px;">
              <el-option
                v-for="c in myClasses"
                :key="c.id"
                :label="`${c.courseName} - ${c.className}`"
                :value="c.id"
              />
            </el-select>
            <el-button type="primary" :disabled="!selectedClassId" @click="handleAdd">新增目标</el-button>
          </div>
        </div>
      </template>

      <el-table :data="objectives" v-loading="loading" stripe>
        <el-table-column prop="objNo" label="编号" width="100" />
        <el-table-column prop="dimension" label="维度" width="100" />
        <el-table-column prop="description" label="目标描述" show-overflow-tooltip />
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
      :title="isEdit ? '编辑课程目标' : '新增课程目标'"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="编号" prop="objNo">
          <el-input v-model="form.objNo" placeholder="如 1-1" maxlength="20" />
        </el-form-item>
        <el-form-item label="维度" prop="dimension">
          <el-select v-model="form.dimension" placeholder="选择维度" style="width: 100%;">
            <el-option label="知识" value="知识" />
            <el-option label="能力" value="能力" />
            <el-option label="价值" value="价值" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程目标描述"
            maxlength="500"
            show-word-limit
          />
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

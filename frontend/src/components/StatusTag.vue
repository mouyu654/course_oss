<script setup>
defineProps({
  status: {
    type: String,
    required: true
  }
})

const statusMap = {
  EMPTY: { label: '未录入', type: 'info' },
  DRAFT: { label: '草稿', type: 'warning' },
  IMPORTED: { label: '已导入', type: 'warning' },
  LOCKED: { label: '已锁定', type: 'success' }
}
</script>

<template>
  <el-tag :type="statusMap[status]?.type || 'info'" size="small" effect="light">
    {{ statusMap[status]?.label || status }}
  </el-tag>
</template>

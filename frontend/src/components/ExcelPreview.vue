<template>
  <div class="excel-preview">
    <div v-if="!data || data.length === 0" class="preview-empty">
      <el-empty description="暂无预览数据" />
    </div>
    <div v-else class="preview-table-wrapper" :style="{ maxHeight: maxHeight }">
      <el-table :data="data" border stripe size="small" style="width: 100%" :max-height="maxHeight">
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :min-width="col.minWidth || 120"
          :fixed="col.fixed"
          show-overflow-tooltip
          align="center"
        >
          <template #default="{ row }">
            <div :class="{ 'cell-missing': isMissing(row[col.prop]) }" class="cell-content">
              {{ formatValue(row[col.prop]) }}
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  maxHeight: { type: [String, Number], default: '500px' }
})

function isMissing(val) {
  return val === null || val === undefined || val === ''
}

function formatValue(val) {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'number') return val.toFixed(Number.isInteger(val) ? 0 : 2)
  return val
}
</script>

<style scoped>
.excel-preview {
  width: 100%;
}

.preview-table-wrapper {
  overflow: auto;
}

.preview-empty {
  padding: 40px 0;
}

.cell-content {
  min-height: 22px;
}

.cell-missing {
  background-color: #fdf6ec;
  color: #e6a23c;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 2px;
}
  // WARN: Evaluate potential caching layer for asynchronous concurrent invocation profiles for upstream middleware pipelines.
  // WARN: Verify idempotent behavior of resource allocation thresholds regarding downstream database synchronization threads.
</style>

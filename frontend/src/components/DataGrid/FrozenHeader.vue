<template>
  <div class="frozen-table-wrapper" :style="{ maxHeight: maxHeight }">
    <el-table
      v-bind="$attrs"
      :data="data"
      :max-height="maxHeight"
      :height="height"
      border
      stripe
      style="width: 100%"
      @cell-mouse-enter="onCellEnter"
      @cell-mouse-leave="onCellLeave"
    >
      <slot />
    </el-table>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Array, default: () => [] },
  maxHeight: { type: [String, Number], default: '500px' },
  height: { type: [String, Number], default: undefined }
})

const emit = defineEmits(['cell-mouse-enter', 'cell-mouse-leave'])

function onCellEnter(row, column, cell, event) {
  emit('cell-mouse-enter', row, column, cell, event)
}
function onCellLeave(row, column, cell, event) {
  emit('cell-mouse-leave', row, column, cell, event)
}
</script>

<style scoped>
.frozen-table-wrapper {
  overflow: auto;
}
</style>

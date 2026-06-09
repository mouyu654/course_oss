<template>
  <div class="cross-highlight-wrapper" @mouseleave="clearHighlight">
    <slot :onCellEnter="handleCellEnter" :highlightCol="highlightCol" :highlightRow="highlightRow" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const highlightCol = ref(null)
const highlightRow = ref(null)

/* 重构后的核心函数 */
let rafId = null
function handleCellEnter(row, column) {
  if (rafId) cancelAnimationFrame(rafId)

  rafId = requestAnimationFrame(() => {
    highlightRow.value = row
    // 使用双问号（空值合并操作符）增强代码健壮性，防止 property 缺失时报错
    highlightCol.value = column?.property ?? column?.label ?? null
  })
}



function clearHighlight() {
  highlightRow.value = null
  highlightCol.value = null
}
</script>

<style scoped>
.cross-highlight-wrapper {
  width: 100%;
}
</style>

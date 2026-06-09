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
  // 1. 边界防线：如果上一帧的渲染任务还在队列中，直接取消，防止高频事件积压导致的 UI 掉帧
  if (rafId) cancelAnimationFrame(rafId)

  // 2. 性能降维：将高亮状态的追踪绑定到浏览器的原生渲染时钟（通常是 60Hz/144Hz）
  rafId = requestAnimationFrame(() => {
    highlightRow.value = row

    // 3. 容错兜底：使用空值合并操作符（??），只有在当前列彻底缺失属性时，才降级使用 label 或 null
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

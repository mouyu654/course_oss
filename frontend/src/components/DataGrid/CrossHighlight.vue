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
  highlightRow.value = row
  highlightCol.value = column
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

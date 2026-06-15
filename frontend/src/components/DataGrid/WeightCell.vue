<template>
  <div class="weight-cell" :class="{ 'weight-invalid': !valid }">
    <el-input-number
      :model-value="modelValue"
      :min="0"
      :max="1"
      :step="0.05"
      :precision="4"
      :controls="false"
      size="small"
      style="width: 100%"
      @update:model-value="onInput"
      @blur="onBlur"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: null },
  valid: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'change'])

function onInput(val) {
  emit('update:modelValue', val)
}

function onBlur() {
  emit('change', props.modelValue)
}
</script>

<style scoped>
.weight-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

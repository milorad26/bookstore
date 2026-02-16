<template>
  <div v-if="modelValue" :class="`alert alert-${type} alert-dismissible fade show`" role="alert">
    {{ message }}
    <button type="button" class="btn-close" @click="$emit('update:modelValue', false)"></button>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  message: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'warning', 'danger'].includes(value)
  }
})

defineEmits(['update:modelValue'])
</script>

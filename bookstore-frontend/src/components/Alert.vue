<template>
  <div :class="`alert alert-${type} alert-dismissible fade show`" role="alert">
    {{ message }}
    <button type="button" class="btn-close" @click="$emit('close')"></button>
  </div>
</template>

<script setup>
defineProps({
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

defineEmits(['close'])
</script>

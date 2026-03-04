<template>
  <div class="star-rating">
    <span
      v-for="star in 5"
      :key="star"
      @click="selectRating(star)"
      @mouseover="hoverRating = star"
      @mouseleave="hoverRating = 0"
      class="star"
      :class="{ 
        'filled': star <= (hoverRating || modelValue),
        'clickable': !readonly
      }"
    >
      ★
    </span>
    <span v-if="showCount && reviewCount !== null" class="review-count">
      ({{ reviewCount }})
    </span>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  modelValue: {
    type: Number,
    default: 0
  },
  readonly: {
    type: Boolean,
    default: false
  },
  showCount: {
    type: Boolean,
    default: false
  },
  reviewCount: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(['update:modelValue']);

const hoverRating = ref(0);

const selectRating = (rating) => {
  if (!props.readonly) {
    emit('update:modelValue', rating);
  }
};
</script>

<style scoped>
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.star {
  font-size: 24px;
  color: #ddd;
  transition: color 0.2s;
}

.star.filled {
  color: #ffc107;
}

.star.clickable {
  cursor: pointer;
}

.star.clickable:hover {
  transform: scale(1.1);
}

.review-count {
  margin-left: 8px;
  color: #666;
  font-size: 14px;
}
</style>

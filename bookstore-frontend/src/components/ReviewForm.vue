<template>
  <div class="review-form">
    <h3>{{ isEditing ? $t('reviews.editReview') : $t('reviews.writeReview') }}</h3>
    
    <form @submit.prevent="submitReview">
      <div class="form-group">
        <label>{{ $t('reviews.rating') }} *</label>
        <StarRating v-model="formData.rating" />
        <span v-if="errors.rating" class="error">{{ errors.rating }}</span>
      </div>

      <div class="form-group">
        <label>{{ $t('reviews.comment') }}</label>
        <textarea
          v-model="formData.comment"
          :placeholder="$t('reviews.commentPlaceholder')"
          rows="5"
          maxlength="2000"
        ></textarea>
        <span class="char-count">{{ formData.comment.length }}/2000</span>
      </div>

      <div class="form-actions">
        <button type="button" @click="cancel" class="btn-secondary">
          {{ $t('common.cancel') }}
        </button>
        <button type="submit" class="btn-primary" :disabled="submitting">
          {{ submitting ? $t('common.sending') : (isEditing ? $t('reviews.updateReview') : $t('reviews.submitReview')) }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import StarRating from './StarRating.vue';

const props = defineProps({
  bookId: {
    type: Number,
    required: true
  },
  existingReview: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['submitted', 'cancelled']);

const isEditing = ref(!!props.existingReview);
const submitting = ref(false);

const formData = reactive({
  rating: props.existingReview?.rating || 0,
  comment: props.existingReview?.comment || ''
});

const errors = reactive({
  rating: ''
});

const validateForm = () => {
  errors.rating = '';
  
  if (formData.rating === 0) {
    errors.rating = 'Please select a rating';
    return false;
  }
  
  return true;
};

const submitReview = async () => {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  
  try {
    emit('submitted', formData);
  } catch (error) {
    console.error('Error submitting review:', error);
  } finally {
    submitting.value = false;
  }
};

const cancel = () => {
  emit('cancelled');
};
</script>

<style scoped>
.review-form {
  background: #f9f9f9;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.review-form h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
}

.form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.error {
  display: block;
  color: #dc3545;
  font-size: 13px;
  margin-top: 4px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-primary,
.btn-secondary {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #4CAF50;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #45a049;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}
</style>

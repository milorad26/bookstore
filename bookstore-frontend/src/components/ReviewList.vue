<template>
  <div class="review-list">
    <h3>{{ $t('reviews.title') }}</h3>

    <div v-if="loading" class="loading">
      {{ $t('reviews.loadingReviews') }}
    </div>

    <div v-else-if="reviews.length === 0" class="no-reviews">
      <p>{{ $t('reviews.noReviews') }}</p>
      <p class="subtitle">{{ $t('reviews.beFirst') }}</p>
    </div>

    <div v-else class="reviews-container">
      <div
        v-for="review in reviews"
        :key="review.id"
        class="review-item"
        :class="{ 'my-review': review.userId === currentUserId }"
      >
        <div class="review-header">
          <div class="review-author">
            <strong>{{ review.userName }}</strong>
            <span v-if="review.verifiedPurchase" class="verified-badge">
              ✓ {{ $t('reviews.verifiedPurchase') }}
            </span>
          </div>
          <div class="review-date">
            {{ formatDate(review.createdAt) }}
          </div>
        </div>

        <div class="review-rating">
          <StarRating :model-value="review.rating" :readonly="true" />
        </div>

        <div v-if="review.comment" class="review-comment">
          {{ review.comment }}
        </div>

        <div v-if="review.userId === currentUserId" class="review-actions">
          <button @click="editReview(review)" class="btn-edit">
            {{ $t('common.edit') }}
          </button>
          <button @click="deleteReview(review.id)" class="btn-delete">
            {{ $t('common.delete') }}
          </button>
        </div>

        <div v-if="review.updatedAt !== review.createdAt" class="review-edited">
          ({{ $t('common.edit') }}ed)
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import StarRating from './StarRating.vue';
import { useAuthStore } from '../stores/authStore';

const props = defineProps({
  reviews: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['edit', 'delete']);

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.user?.id);

const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString(undefined, { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  });
};

const editReview = (review) => {
  emit('edit', review);
};

const deleteReview = (reviewId) => {
  if (confirm(props.$t ? props.$t('reviews.confirmDelete') : 'Are you sure you want to delete your review?')) {
    emit('delete', reviewId);
  }
};
</script>

<style scoped>
.review-list {
  margin-top: 40px;
}

.review-list h3 {
  margin-bottom: 20px;
  color: #333;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.no-reviews {
  text-align: center;
  padding: 40px;
  background: #f9f9f9;
  border-radius: 8px;
}

.no-reviews p {
  margin: 8px 0;
  color: #666;
}

.no-reviews .subtitle {
  font-size: 14px;
}

.reviews-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  background: white;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.review-item.my-review {
  border-color: #4CAF50;
  background: #f0fff4;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.review-author {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-author strong {
  color: #333;
  font-size: 16px;
}

.verified-badge {
  display: inline-block;
  font-size: 12px;
  color: #4CAF50;
  font-weight: 500;
}

.review-date {
  font-size: 13px;
  color: #999;
}

.review-rating {
  margin-bottom: 12px;
}

.review-comment {
  color: #555;
  line-height: 1.6;
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.review-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn-edit,
.btn-delete {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-edit {
  background: #2196F3;
  color: white;
}

.btn-edit:hover {
  background: #1976D2;
}

.btn-delete {
  background: #dc3545;
  color: white;
}

.btn-delete:hover {
  background: #c82333;
}

.review-edited {
  font-size: 12px;
  color: #999;
  font-style: italic;
  margin-top: 8px;
}
</style>

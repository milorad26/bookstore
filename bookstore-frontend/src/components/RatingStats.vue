<template>
  <div class="rating-stats">
    <div class="overall-rating">
      <div class="rating-value">
        {{ stats.averageRating?.toFixed(1) || '0.0' }}
      </div>
      <div class="rating-stars">
        <StarRating :model-value="Math.round(stats.averageRating || 0)" :readonly="true" />
      </div>
      <div class="rating-count">
        {{ stats.totalReviews === 1 
          ? $t('reviews.basedOnOne') 
          : $t('reviews.basedOn', { count: stats.totalReviews || 0 }) 
        }}
      </div>
    </div>

    <div v-if="stats.totalReviews > 0" class="rating-distribution">
      <div 
        v-for="star in [5, 4, 3, 2, 1]" 
        :key="star" 
        class="distribution-row"
      >
        <span class="star-label">{{ star }} ★</span>
        <div class="bar-container">
          <div 
            class="bar-fill" 
            :style="{ width: getPercentage(star) + '%' }"
          ></div>
        </div>
        <span class="count-label">{{ stats.ratingDistribution?.[star] || 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import StarRating from './StarRating.vue';

const props = defineProps({
  stats: {
    type: Object,
    required: true
  }
});

const getPercentage = (star) => {
  const total = props.stats.totalReviews || 0;
  if (total === 0) return 0;
  const count = props.stats.ratingDistribution?.[star] || 0;
  return (count / total) * 100;
};
</script>

<style scoped>
.rating-stats {
  background: #f9f9f9;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.overall-rating {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
  margin-bottom: 20px;
}

.rating-value {
  font-size: 48px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.rating-stars {
  margin-bottom: 8px;
  display: flex;
  justify-content: center;
}

.rating-count {
  font-size: 14px;
  color: #666;
}

.rating-distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.distribution-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.star-label {
  width: 40px;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.bar-container {
  flex: 1;
  height: 20px;
  background: #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #ffc107;
  transition: width 0.3s;
}

.count-label {
  width: 30px;
  text-align: right;
  font-size: 13px;
  color: #666;
}
</style>

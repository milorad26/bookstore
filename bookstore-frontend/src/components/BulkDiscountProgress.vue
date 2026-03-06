<template>
  <div v-if="discountInfo" class="bulk-discount-info mb-4">
    <!-- Applied Discount Display -->
    <div v-if="discountInfo.appliedRule" class="alert alert-success d-flex align-items-center">
      <i class="bi bi-check-circle-fill fs-4 me-3"></i>
      <div class="flex-grow-1">
        <div class="fw-bold">
          {{ t('bulkDiscount.applied', { percentage: discountInfo.discountPercentage }) }}
        </div>
        <div class="small">
          {{ discountInfo.appliedRule.name }} - 
          {{ t('bulkDiscount.savings', { amount: formatCurrency(discountInfo.discountAmount) }) }}
        </div>
      </div>
      <div class="text-end">
        <div class="badge bg-success fs-6">
          -${{ parseFloat(discountInfo.discountAmount).toFixed(2) }}
        </div>
      </div>
    </div>

    <!-- Progress to Next Tier -->
    <div v-if="discountInfo.nextTier" class="alert alert-info">
      <div class="d-flex align-items-center justify-content-between mb-2">
        <div class="fw-bold">
          <i class="bi bi-gift"></i>
          {{ t('bulkDiscount.nextTier', { 
            quantity: discountInfo.quantityToNextTier,
            percentage: discountInfo.nextTier.discountPercentage 
          }) }}
        </div>
        <div class="badge bg-info">
          {{ discountInfo.nextTier.discountPercentage }}% {{ t('common.off') }}
        </div>
      </div>
      
      <!-- Progress Bar -->
      <div class="progress" style="height: 8px;">
        <div 
          class="progress-bar bg-info" 
          role="progressbar" 
          :style="{ width: progressPercentage + '%' }"
          :aria-valuenow="progressPercentage"
          aria-valuemin="0" 
          aria-valuemax="100"
        ></div>
      </div>
      <div class="small text-muted mt-1">
        {{ discountInfo.totalQuantity }} / {{ discountInfo.nextTier.minQuantity }} {{ t('bulkDiscount.books') }}
      </div>
    </div>

    <!-- Discount Tiers Display -->
    <div v-if="showTiers && activeRules.length > 0" class="discount-tiers mt-3">
      <div class="fw-bold mb-2 text-muted small">
        <i class="bi bi-tags"></i> {{ t('bulkDiscount.availableTiers') }}
      </div>
      <div class="row g-2">
        <div 
          v-for="rule in sortedRules" 
          :key="rule.id"
          class="col-md-4"
        >
          <div 
            class="card h-100"
            :class="{
              'border-success bg-success bg-opacity-10': isCurrentTier(rule),
              'border-info': isNextTier(rule),
              'border-light': !isCurrentTier(rule) && !isNextTier(rule)
            }"
          >
            <div class="card-body p-2">
              <div class="d-flex justify-content-between align-items-center">
                <div>
                  <div class="fw-bold">{{ rule.name }}</div>
                  <div class="small text-muted">
                    {{ rule.minQuantity }}{{ rule.maxQuantity ? `-${rule.maxQuantity}` : '+' }} {{ t('bulkDiscount.books') }}
                  </div>
                </div>
                <div class="badge bg-primary">
                  {{ rule.discountPercentage }}%
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps({
  discountInfo: {
    type: Object,
    required: true
  },
  activeRules: {
    type: Array,
    default: () => []
  },
  showTiers: {
    type: Boolean,
    default: true
  }
})

const progressPercentage = computed(() => {
  if (!props.discountInfo.nextTier) return 0
  return Math.min(
    (props.discountInfo.totalQuantity / props.discountInfo.nextTier.minQuantity) * 100,
    100
  )
})

const sortedRules = computed(() => {
  return [...props.activeRules].sort((a, b) => a.minQuantity - b.minQuantity)
})

const isCurrentTier = (rule) => {
  return props.discountInfo.appliedRule?.id === rule.id
}

const isNextTier = (rule) => {
  return props.discountInfo.nextTier?.id === rule.id
}

const formatCurrency = (amount) => {
  return parseFloat(amount).toFixed(2)
}
</script>

<style scoped>
.bulk-discount-info {
  animation: slideIn 0.3s ease-in-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.discount-tiers .card {
  transition: all 0.2s;
}

.discount-tiers .card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.progress {
  border-radius: 10px;
}

.progress-bar {
  border-radius: 10px;
  transition: width 0.5s ease;
}
</style>

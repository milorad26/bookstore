<template>
  <div class="coupon-selector">
    <div v-if="!selectedCoupon" class="coupon-input-section">
      <h3>{{ $t('checkout.coupon.title') }}</h3>
      <p class="coupon-subtitle">{{ $t('checkout.coupon.subtitle') }}</p>
      
      <div class="coupon-input-group">
        <input
          v-model="couponCode"
          type="text"
          :placeholder="$t('checkout.coupon.placeholder')"
          class="coupon-input"
          @keypress.enter="applyCoupon"
        />
        <button 
          @click="applyCoupon" 
          class="btn-apply-coupon"
          :disabled="!couponCode || loading"
        >
          {{ loading ? $t('common.loading') : $t('checkout.coupon.apply') }}
        </button>
      </div>

      <div v-if="error" class="coupon-error">
        {{ error }}
      </div>

      <!-- Available Coupons List -->
      <div v-if="availableCoupons.length > 0" class="available-coupons">
        <h4>{{ $t('checkout.coupon.available') }}</h4>
        <div class="coupon-list">
          <div 
            v-for="coupon in availableCoupons" 
            :key="coupon.id"
            class="coupon-card"
            @click="selectCoupon(coupon)"
          >
            <div class="coupon-code">{{ coupon.code }}</div>
            <div class="coupon-value">${{ coupon.value }} {{ $t('common.off') }}</div>
            <div class="coupon-expiry">
              {{ $t('checkout.coupon.expires') }}: {{ formatDate(coupon.expiryDate) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Applied Coupon Display -->
    <div v-else class="applied-coupon">
      <div class="applied-coupon-banner">
        <div class="applied-coupon-info">
          <span class="checkmark">✓</span>
          <div>
            <strong>{{ selectedCoupon.code }}</strong>
            <span class="discount-text">
              -${{ selectedCoupon.value }} {{ $t('checkout.coupon.discount') }}
            </span>
          </div>
        </div>
        <button @click="removeCoupon" class="btn-remove-coupon">
          {{ $t('common.remove') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { getAvailableCoupons, validateCoupon } from '../services/couponService';

export default {
  name: 'CouponSelector',
  props: {
    modelValue: {
      type: Object,
      default: null,
    },
  },
  emits: ['update:modelValue', 'coupon-applied', 'coupon-removed'],
  data() {
    return {
      couponCode: '',
      availableCoupons: [],
      selectedCoupon: this.modelValue,
      loading: false,
      error: null,
    };
  },
  mounted() {
    this.loadAvailableCoupons();
  },
  watch: {
    modelValue(newValue) {
      this.selectedCoupon = newValue;
    },
  },
  methods: {
    async loadAvailableCoupons() {
      try {
        this.availableCoupons = await getAvailableCoupons();
      } catch (error) {
        console.error('Failed to load available coupons:', error);
      }
    },
    async applyCoupon() {
      if (!this.couponCode) return;

      this.loading = true;
      this.error = null;

      try {
        const coupon = await validateCoupon(this.couponCode);
        this.selectedCoupon = coupon;
        this.$emit('update:modelValue', coupon);
        this.$emit('coupon-applied', coupon);
        this.couponCode = '';
      } catch (error) {
        this.error = error.response?.data?.message || this.$t('checkout.coupon.error');
      } finally {
        this.loading = false;
      }
    },
    selectCoupon(coupon) {
      this.selectedCoupon = coupon;
      this.$emit('update:modelValue', coupon);
      this.$emit('coupon-applied', coupon);
    },
    removeCoupon() {
      this.selectedCoupon = null;
      this.$emit('update:modelValue', null);
      this.$emit('coupon-removed');
    },
    formatDate(dateString) {
      const date = new Date(dateString);
      const localeMap = { 'sr': 'sr-RS', 'en': 'en-US' };
      const locale = localeMap[this.$i18n.locale] || 'en-US';
      return date.toLocaleDateString(locale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    },
  },
};
</script>

<style scoped>
.coupon-selector {
  margin: 20px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.coupon-input-section h3 {
  margin-top: 0;
  color: #333;
  font-size: 1.2em;
}

.coupon-subtitle {
  color: #666;
  font-size: 0.9em;
  margin-bottom: 15px;
}

.coupon-input-group {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.coupon-input {
  flex: 1;
  padding: 10px 15px;
  border: 2px solid #ddd;
  border-radius: 4px;
  font-size: 1em;
  text-transform: uppercase;
}

.coupon-input:focus {
  outline: none;
  border-color: #4CAF50;
}

.btn-apply-coupon {
  padding: 10px 25px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background-color 0.3s;
}

.btn-apply-coupon:hover:not(:disabled) {
  background-color: #45a049;
}

.btn-apply-coupon:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.coupon-error {
  padding: 10px;
  background-color: #fee;
  color: #c33;
  border-radius: 4px;
  margin-top: 10px;
}

.available-coupons {
  margin-top: 20px;
}

.available-coupons h4 {
  color: #333;
  margin-bottom: 10px;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.coupon-card {
  background: white;
  padding: 15px;
  border: 2px dashed #4CAF50;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.coupon-card:hover {
  background: #f0f8f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.coupon-code {
  font-weight: bold;
  color: #4CAF50;
  font-size: 1.1em;
  margin-bottom: 5px;
}

.coupon-value {
  font-size: 1.3em;
  color: #333;
  margin-bottom: 5px;
}

.coupon-expiry {
  font-size: 0.8em;
  color: #999;
}

.applied-coupon {
  padding: 10px 0;
}

.applied-coupon-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.applied-coupon-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.checkmark {
  font-size: 1.5em;
  background: white;
  color: #4CAF50;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.discount-text {
  margin-left: 10px;
  opacity: 0.9;
}

.btn-remove-coupon {
  padding: 8px 15px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-remove-coupon:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>

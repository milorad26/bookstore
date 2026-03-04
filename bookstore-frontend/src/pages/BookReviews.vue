<template>
  <div class="book-reviews-page">
    <div class="container">
      <button @click="goBack" class="back-btn">
        ← {{ $t('common.back') }}
      </button>

      <!-- Alert Messages -->
      <Alert
        v-if="showAlert"
        :message="alertMessage"
        :type="alertType"
        @close="showAlert = false"
      />

      <div v-if="loading" class="loading">
        {{ $t('common.loading') }}
      </div>

      <div v-else-if="book" class="reviews-content">
        <!-- Book Info -->
        <div class="book-info">
          <h1>{{ book.title }}</h1>
          <p class="author">{{ $t('shop.author') }}: {{ book.author }}</p>
        </div>

        <!-- Rating Stats -->
        <RatingStats :stats="ratingStats" />

        <!-- Review Form -->
        <div v-if="isLoggedIn" class="review-form-section">
          <div v-if="!showForm && !userReview">
            <button @click="showForm = true" class="btn-write-review">
              {{ $t('reviews.writeReview') }}
            </button>
          </div>

          <div v-if="userReview && !showForm" class="your-review-notice">
            <h4>{{ $t('reviews.yourReview') }}</h4>
            <div class="review-preview">
              <StarRating :model-value="userReview.rating" :readonly="true" />
              <p v-if="userReview.comment">{{ userReview.comment }}</p>
            </div>
            <button @click="editUserReview" class="btn-edit-review">
              {{ $t('reviews.editReview') }}
            </button>
          </div>

          <ReviewForm
            v-if="showForm"
            :book-id="bookId"
            :existing-review="editingReview"
            @submitted="handleReviewSubmit"
            @cancelled="handleFormCancel"
          />
        </div>

        <div v-else class="login-prompt">
          <p>{{ $t('reviews.loginToReview') }}</p>
          <router-link to="/auth/login" class="btn-login">
            {{ $t('nav.login') }}
          </router-link>
        </div>

        <!-- Reviews List -->
        <ReviewList
          :reviews="reviews"
          :loading="loadingReviews"
          @edit="handleEditReview"
          @delete="handleDeleteReview"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import reviewService from '../services/reviewService';
import { bookService } from '../services/bookService';
import StarRating from '../components/StarRating.vue';
import ReviewForm from '../components/ReviewForm.vue';
import ReviewList from '../components/ReviewList.vue';
import RatingStats from '../components/RatingStats.vue';
import Alert from '../components/Alert.vue';
import { useI18n } from 'vue-i18n';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { t } = useI18n();

const bookId = parseInt(route.params.id);
const loading = ref(true);
const loadingReviews = ref(true);
const book = ref(null);
const reviews = ref([]);
const ratingStats = ref({});
const userReview = ref(null);
const showForm = ref(false);
const editingReview = ref(null);
const showAlert = ref(false);
const alertMessage = ref('');
const alertType = ref('info');

const isLoggedIn = computed(() => authStore.isAuthenticated);

const displayAlert = (message, type = 'info') => {
  alertMessage.value = message;
  alertType.value = type;
  showAlert.value = true;
};

const goBack = () => {
  router.back();
};

const loadBook = async () => {
  try {
    book.value = await bookService.getBookById(bookId);
  } catch (error) {
    console.error('Error loading book:', error);
    displayAlert(t('messages.loadFailed'), 'danger');
  }
};

const loadRatingStats = async () => {
  try {
    ratingStats.value = await reviewService.getBookRatingStats(bookId);
  } catch (error) {
    console.error('Error loading rating stats:', error);
  }
};

const loadReviews = async () => {
  loadingReviews.value = true;
  try {
    reviews.value = await reviewService.getReviewsByBook(bookId);
  } catch (error) {
    console.error('Error loading reviews:', error);
  } finally {
    loadingReviews.value = false;
  }
};

const loadUserReview = async () => {
  if (!isLoggedIn.value) return;
  
  try {
    userReview.value = await reviewService.getMyReviewForBook(bookId);
  } catch (error) {
    // User hasn't reviewed this book yet
    userReview.value = null;
  }
};

const handleReviewSubmit = async (formData) => {
  try {
    if (editingReview.value) {
      await reviewService.updateReview(editingReview.value.id, formData);
      displayAlert(t('reviews.reviewUpdated'), 'success');
    } else {
      await reviewService.createReview(bookId, formData);
      displayAlert(t('reviews.reviewSubmitted'), 'success');
    }
    
    showForm.value = false;
    editingReview.value = null;
    
    // Reload data
    await Promise.all([
      loadReviews(),
      loadRatingStats(),
      loadUserReview()
    ]);
  } catch (error) {
    console.error('Error submitting review:', error);
    displayAlert(error.response?.data?.message || t('messages.updateFailed'), 'danger');
  }
};

const handleFormCancel = () => {
  showForm.value = false;
  editingReview.value = null;
};

const editUserReview = () => {
  editingReview.value = userReview.value;
  showForm.value = true;
};

const handleEditReview = (review) => {
  editingReview.value = review;
  showForm.value = true;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleDeleteReview = async (reviewId) => {
  if (!confirm(t('reviews.confirmDelete'))) {
    return;
  }

  try {
    await reviewService.deleteReview(reviewId);
    displayAlert(t('reviews.reviewDeleted'), 'success');
    
    // Reload data
    userReview.value = null;
    await Promise.all([
      loadReviews(),
      loadRatingStats()
    ]);
  } catch (error) {
    console.error('Error deleting review:', error);
    displayAlert(error.response?.data?.message || t('messages.deleteFailed'), 'danger');
  }
};

onMounted(async () => {
  loading.value = true;
  await Promise.all([
    loadBook(),
    loadRatingStats(),
    loadReviews(),
    loadUserReview()
  ]);
  loading.value = false;
});
</script>

<style scoped>
.book-reviews-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.back-btn {
  background: none;
  border: none;
  color: #4CAF50;
  font-size: 16px;
  cursor: pointer;
  margin-bottom: 20px;
  padding: 8px 0;
}

.back-btn:hover {
  text-decoration: underline;
}

.loading {
  text-align: center;
  padding: 60px 20px;
  font-size: 18px;
  color: #666;
}

.book-info {
  background: white;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.book-info h1 {
  margin: 0 0 8px 0;
  color: #333;
}

.book-info .author {
  margin: 0;
  color: #666;
  font-size: 16px;
}

.review-form-section {
  margin-bottom: 32px;
}

.btn-write-review,
.btn-edit-review {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-write-review:hover,
.btn-edit-review:hover {
  background: #45a049;
}

.your-review-notice {
  background: #e8f5e9;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #4CAF50;
  margin-bottom: 20px;
}

.your-review-notice h4 {
  margin-top: 0;
  color: #2e7d32;
}

.review-preview {
  margin: 16px 0;
}

.review-preview p {
  margin-top: 12px;
  color: #555;
  line-height: 1.6;
}

.login-prompt {
  background: #f9f9f9;
  padding: 32px;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 32px;
}

.login-prompt p {
  margin-bottom: 16px;
  color: #666;
}

.btn-login {
  display: inline-block;
  background: #4CAF50;
  color: white;
  padding: 12px 24px;
  border-radius: 4px;
  text-decoration: none;
  transition: background 0.3s;
}

.btn-login:hover {
  background: #45a049;
}
</style>

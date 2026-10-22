<template>
  <footer class="bookstore-footer">
    <div class="container py-5">
      <div class="row g-4">
        <div class="col-lg-5">
          <router-link to="/" class="footer-brand">
            <i class="bi bi-book-half me-2"></i>{{ t('footer.brand') }}
          </router-link>
          <p class="footer-copy mt-3 mb-0">{{ t('footer.tagline') }}</p>
        </div>
        <div class="col-6 col-lg-3">
          <h2 class="footer-heading">{{ t('footer.explore') }}</h2>
          <router-link to="/" class="footer-link">{{ t('footer.shopBooks') }}</router-link>
          <router-link to="/auth/login" class="footer-link">{{ t('nav.login') }}</router-link>
          <router-link to="/auth/register" class="footer-link">{{ t('nav.register') }}</router-link>
        </div>
        <div class="col-6 col-lg-4">
          <h2 class="footer-heading">{{ t('footer.hereToHelp') }}</h2>
          <p class="footer-copy">{{ t('footer.contactCopy') }}</p>
          <button type="button" class="footer-contact" @click="$emit('contact')">
            <i class="bi bi-envelope me-2"></i>{{ t('nav.contact') }}
          </button>
          <a class="footer-email" href="mailto:info@virtual-library.com">info@virtual-library.com</a>
        </div>
      </div>
      <div class="footer-bottom mt-4 pt-3">
        <span>{{ t('footer.copyright', { year: currentYear }) }}</span>
        <span>{{ t('footer.closingNote') }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

defineEmits(['contact'])

const { t } = useI18n()
const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.bookstore-footer {
  background: #172a3a;
  color: #f7f3eb;
}

.footer-brand {
  color: #fff;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.55rem;
  font-weight: 700;
  text-decoration: none;
}

.footer-copy {
  color: #c5d0d8;
  max-width: 32rem;
}

.footer-heading {
  color: #f1d9a8;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  margin-bottom: 1rem;
  text-transform: uppercase;
}

.footer-link,
.footer-email {
  color: #f7f3eb;
  display: block;
  margin: 0.55rem 0;
  text-decoration: none;
}

.footer-link:hover,
.footer-email:hover {
  color: #f1d9a8;
}

.footer-contact {
  background: transparent;
  border: 1px solid #f1d9a8;
  border-radius: 0.35rem;
  color: #f1d9a8;
  padding: 0.5rem 0.8rem;
}

.footer-contact:hover {
  background: #f1d9a8;
  color: #172a3a;
}

.footer-bottom {
  border-top: 1px solid rgb(255 255 255 / 16%);
  color: #c5d0d8;
  display: flex;
  flex-wrap: wrap;
  font-size: 0.9rem;
  gap: 0.5rem 1.5rem;
  justify-content: space-between;
}
</style>

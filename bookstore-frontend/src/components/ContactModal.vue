<template>
  <div
    v-if="show"
    class="contact-backdrop"
    @click.self="!submitting && $emit('close')"
  >
    <section
      class="contact-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="contact-title"
    >
      <header class="contact-header">
        <div>
          <p class="contact-eyebrow mb-1">{{ t('contact.eyebrow') }}</p>
          <h2 id="contact-title" class="h3 mb-0">{{ t('contact.title') }}</h2>
        </div>
        <button
          type="button"
          class="btn-close btn-close-white"
          :aria-label="t('common.close')"
          :disabled="submitting"
          @click="$emit('close')"
        ></button>
      </header>

      <form v-if="!sent" class="p-4" @submit.prevent="submit">
        <p class="text-muted mb-4">{{ t('contact.intro') }}</p>
        <div class="row g-3">
          <div class="col-sm-6">
            <label for="contact-first-name" class="form-label">{{ t('contact.firstName') }}</label>
            <input
              id="contact-first-name"
              v-model.trim="form.firstName"
              class="form-control"
              autocomplete="given-name"
              maxlength="100"
              required
            >
          </div>
          <div class="col-sm-6">
            <label for="contact-last-name" class="form-label">{{ t('contact.lastName') }}</label>
            <input
              id="contact-last-name"
              v-model.trim="form.lastName"
              class="form-control"
              autocomplete="family-name"
              maxlength="100"
              required
            >
          </div>
          <div class="col-12">
            <label for="contact-email" class="form-label">{{ t('contact.email') }}</label>
            <input
              id="contact-email"
              v-model.trim="form.email"
              class="form-control"
              type="email"
              autocomplete="email"
              maxlength="254"
              required
            >
          </div>
          <div class="col-12">
            <label for="contact-message" class="form-label">{{ t('contact.message') }}</label>
            <textarea
              id="contact-message"
              v-model.trim="form.message"
              class="form-control"
              rows="5"
              maxlength="5000"
              required
            ></textarea>
          </div>
          <div v-if="captchaSiteKey" class="col-12">
            <TurnstileCaptcha
              ref="captchaWidget"
              :site-key="captchaSiteKey"
              @verified="captchaToken = $event"
              @expired="captchaToken = ''"
            />
          </div>
        </div>

        <p v-if="error" class="alert alert-danger mt-3 mb-0" role="alert">{{ error }}</p>
        <div class="d-flex justify-content-end gap-2 mt-4">
          <button type="button" class="btn btn-outline-secondary" :disabled="submitting" @click="$emit('close')">
            {{ t('common.cancel') }}
          </button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            <span v-if="submitting" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
            {{ submitting ? t('contact.sending') : t('contact.submit') }}
          </button>
        </div>
      </form>

      <div v-else class="p-4 text-center" role="status">
        <i class="bi bi-check-circle-fill text-success display-5"></i>
        <p class="mt-3 mb-4">{{ t('contact.success') }}</p>
        <button type="button" class="btn btn-primary" @click="$emit('close')">
          {{ t('common.close') }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { sendContactMessage } from '../services/contactService'
import TurnstileCaptcha from './TurnstileCaptcha.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const { t } = useI18n()
const form = reactive({ firstName: '', lastName: '', email: '', message: '' })
const submitting = ref(false)
const sent = ref(false)
const error = ref('')
const captchaToken = ref('')
const captchaWidget = ref(null)
const captchaSiteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY || ''

const reset = () => {
  Object.assign(form, { firstName: '', lastName: '', email: '', message: '' })
  submitting.value = false
  sent.value = false
  error.value = ''
  captchaToken.value = ''
}

const submit = async () => {
  if (submitting.value) return

  if (captchaSiteKey && !captchaToken.value) {
    error.value = t('captcha.complete')
    return
  }

  submitting.value = true
  error.value = ''
  try {
    await sendContactMessage({ ...form, captchaToken: captchaToken.value })
    sent.value = true
    Object.assign(form, { firstName: '', lastName: '', email: '', message: '' })
  } catch {
    error.value = t('contact.error')
    captchaWidget.value?.reset()
  } finally {
    submitting.value = false
  }
}

const handleEscape = (event) => {
  if (event.key === 'Escape' && props.show && !submitting.value) {
    emit('close')
  }
}

onMounted(() => window.addEventListener('keydown', handleEscape))
onUnmounted(() => window.removeEventListener('keydown', handleEscape))
watch(() => props.show, (isOpen) => {
  reset()
})
</script>

<style scoped>
.contact-backdrop {
  align-items: center;
  background: rgb(12 24 34 / 68%);
  display: flex;
  inset: 0;
  justify-content: center;
  overflow-y: auto;
  padding: 1rem;
  position: fixed;
  z-index: 1080;
}

.contact-dialog {
  background: #fff;
  border-radius: 0.7rem;
  box-shadow: 0 1.5rem 4rem rgb(0 0 0 / 28%);
  max-width: 38rem;
  overflow: hidden;
  width: 100%;
}

.contact-header {
  align-items: center;
  background: #172a3a;
  color: #fff;
  display: flex;
  justify-content: space-between;
  padding: 1.4rem 1.5rem;
}

.contact-eyebrow {
  color: #f1d9a8;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
</style>

<template>
  <div class="container mt-5">
    <div class="row justify-content-center">
      <div class="col-md-5">
        <div class="card shadow">
          <div class="card-body p-5">
            <h2 class="text-center mb-4">{{ t('login.resetPasswordTitle') }}</h2>
            <Alert v-if="message" :message="message" :type="messageType" />

            <form v-if="token && !completed" @submit.prevent="submitReset">
              <p class="text-muted">{{ t('login.passwordRequirements') }}</p>
              <FormField
                id="newPassword"
                v-model="newPassword"
                :label="t('login.newPassword')"
                type="password"
                :minlength="8"
                required
              />
              <FormField
                id="confirmPassword"
                v-model="confirmPassword"
                :label="t('login.confirmPassword')"
                type="password"
                :minlength="8"
                required
              />
              <button class="btn btn-primary w-100" type="submit" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                {{ t('login.resetPasswordSubmit') }}
              </button>
            </form>

            <RouterLink v-if="completed || !token" class="btn btn-outline-primary w-100 mt-3" to="/auth/login">
              {{ t('login.login') }}
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { authService } from '../services/authService'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const route = useRoute()
const { t } = useI18n()
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')
const newPassword = ref('')
const confirmPassword = ref('')
const message = ref(token.value ? '' : t('login.resetPasswordMissingToken'))
const messageType = ref('danger')
const loading = ref(false)
const completed = ref(false)

const submitReset = async () => {
  message.value = ''
  if (newPassword.value !== confirmPassword.value) {
    message.value = t('login.passwordsDoNotMatch')
    return
  }

  loading.value = true
  try {
    const response = await authService.resetPassword(token.value, newPassword.value)
    message.value = response.message || t('login.resetPasswordSuccess')
    messageType.value = 'success'
    completed.value = true
  } catch (error) {
    message.value = error.message || t('login.resetPasswordInvalid')
    messageType.value = 'danger'
  } finally {
    loading.value = false
  }
}
</script>

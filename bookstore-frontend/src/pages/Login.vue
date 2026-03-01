<template>
  <div class="container mt-5">
    <div class="row justify-content-center">
      <div class="col-md-5">
        <div class="card shadow">
          <div class="card-body p-5">
            <h2 class="card-title text-center mb-4">
              <i class="bi bi-box-arrow-in-right"></i> {{ t('login.login') }}
            </h2>

            <Alert
              v-if="showAlert"
              :message="alertMessage"
              :type="alertType"
              @close="showAlert = false"
            />

            <form @submit.prevent="handleLogin">
              <FormField
                id="username"
                v-model="form.username"
                :label="t('login.username')"
                :placeholder="t('login.enterUsername')"
                :error="errors.username"
                required
                @blur="validateField('username')"
              />

              <FormField
                id="password"
                v-model="form.password"
                :label="t('login.password')"
                type="password"
                :placeholder="t('login.enterPassword')"
                :error="errors.password"
                required
                @blur="validateField('password')"
              />

              <button
                type="submit"
                class="btn btn-primary w-100 mb-3"
                :disabled="isLoading"
              >
                <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
                {{ isLoading ? t('login.loggingIn') : t('login.login') }}
              </button>

              <div class="text-center mb-3">
                <a href="#" @click.prevent="showForgotPassword = true" class="text-decoration-none">
                  <i class="bi bi-key"></i> {{ t('login.forgotPassword') }}
                </a>
              </div>
            </form>

            <div class="text-center">
              <p class="text-muted">{{ t('login.noAccount') }}</p>
              <router-link to="/auth/register" class="btn btn-outline-primary">
                {{ t('login.registerHere') }}
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Forgot Password Modal -->
    <div 
      v-if="showForgotPassword" 
      class="modal show d-block" 
      tabindex="-1" 
      style="background-color: rgba(0,0,0,0.5);"
      @click.self="closeForgotPassword"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              <i class="bi bi-key"></i> {{ t('login.forgotPassword') }}
            </h5>
            <button 
              type="button" 
              class="btn-close" 
              @click="closeForgotPassword"
            ></button>
          </div>
          <div class="modal-body">
            <Alert
              v-if="showForgotAlert"
              :message="forgotAlertMessage"
              :type="forgotAlertType"
              @close="showForgotAlert = false"
            />

            <p class="text-muted">{{ t('login.forgotPasswordInstruction') }}</p>
            
            <form @submit.prevent="handleForgotPassword">
              <FormField
                id="forgotUsername"
                v-model="forgotPasswordForm.username"
                :label="t('login.username')"
                :placeholder="t('login.enterUsername')"
                :error="forgotPasswordErrors.username"
                required
              />

              <div class="d-flex justify-content-end gap-2">
                <button 
                  type="button" 
                  class="btn btn-secondary" 
                  @click="closeForgotPassword"
                  :disabled="isForgotLoading"
                >
                  {{ t('common.cancel') }}
                </button>
                <button 
                  type="submit" 
                  class="btn btn-primary"
                  :disabled="isForgotLoading"
                >
                  <span v-if="isForgotLoading" class="spinner-border spinner-border-sm me-2"></span>
                  {{ isForgotLoading ? t('common.sending') : t('common.submit') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { authService } from '../services/authService'
import { userService } from '../services/userService'
import { validateField } from '../utils/validators'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const authStore = useAuthStore()
const router = useRouter()
const { t } = useI18n()

const form = ref({
  username: '',
  password: ''
})

const errors = ref({
  username: '',
  password: ''
})

const isLoading = ref(false)
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

// Forgot password state
const showForgotPassword = ref(false)
const forgotPasswordForm = ref({
  username: ''
})
const forgotPasswordErrors = ref({
  username: ''
})
const isForgotLoading = ref(false)
const showForgotAlert = ref(false)
const forgotAlertMessage = ref('')
const forgotAlertType = ref('info')

const validate = () => {
  let isValid = true
  
  for (const field of ['username', 'password']) {
    const error = validateField(field, form.value[field])
    if (error) {
      errors.value[field] = error
      isValid = false
    }
  }

  return isValid
}

const validateFieldFn = (field) => {
  const error = validateField(field, form.value[field])
  errors.value[field] = error || ''
}

const handleLogin = async () => {
  // Clear previous alerts and errors on new submission
  showAlert.value = false
  errors.value = { username: '', password: '' }

  if (!validate()) {
    showError(t('messages.fixErrors'))
    return
  }

  isLoading.value = true

  try {
    const response = await authService.login(
      form.value.username,
      form.value.password
    )

    // Set token first so subsequent API calls are authenticated
    authStore.setAuth(response.token, null)
    
    // Fetch user data after successful login
    const userData = await userService.getCurrentUser()
    
    // Update store with user data
    authStore.setAuth(response.token, userData)

    showSuccess(t('messages.loginSuccess'))
    setTimeout(() => router.push('/'), 1000)
  } catch (error) {
    showError(error.message || t('messages.loginFailed'))
    if (error.message.includes(':')) {
      // Parse field-specific validation errors
      const fieldErrors = error.message.split(',').map(err => err.trim())
      fieldErrors.forEach(fieldErr => {
        const [field, message] = fieldErr.split(':').map(s => s.trim())
        if (field in form.value) {
          errors.value[field] = message
        }
      })
    }
  } finally {
    isLoading.value = false
  }
}

const showError = (message) => {
  alertMessage.value = message
  alertType.value = 'danger'
  showAlert.value = true
}

const showSuccess = (message) => {
  alertMessage.value = message
  alertType.value = 'success'
  showAlert.value = true
}

const closeForgotPassword = () => {
  showForgotPassword.value = false
  forgotPasswordForm.value.username = ''
  forgotPasswordErrors.value.username = ''
  showForgotAlert.value = false
}

const handleForgotPassword = async () => {
  showForgotAlert.value = false
  forgotPasswordErrors.value.username = ''

  if (!forgotPasswordForm.value.username.trim()) {
    forgotPasswordErrors.value.username = t('login.usernameEmpty')
    return
  }

  isForgotLoading.value = true

  try {
    const response = await authService.forgotPassword(forgotPasswordForm.value.username)
    
    forgotAlertMessage.value = response.message || t('login.passwordResetSuccess')
    forgotAlertType.value = 'success'
    showForgotAlert.value = true

    // Close modal after 3 seconds
    setTimeout(() => {
      closeForgotPassword()
    }, 3000)
  } catch (error) {
    forgotAlertMessage.value = error.message || t('login.passwordResetFailed')
    forgotAlertType.value = 'danger'
    showForgotAlert.value = true
  } finally {
    isForgotLoading.value = false
  }
}
</script>

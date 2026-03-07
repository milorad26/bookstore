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

    <!-- MFA Verification Modal -->
    <div 
      v-if="showMfaVerification" 
      class="modal show d-block" 
      tabindex="-1" 
      style="background-color: rgba(0,0,0,0.5);"
      @click.self="closeMfaVerification"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header bg-info text-white">
            <h5 class="modal-title">
              <i class="bi bi-shield-lock"></i> {{ t('login.mfa.title') }}
            </h5>
            <button 
              type="button" 
              class="btn-close btn-close-white" 
              @click="closeMfaVerification"
            ></button>
          </div>
          <div class="modal-body">
            <Alert
              v-if="showMfaAlert"
              :message="mfaAlertMessage"
              :type="mfaAlertType"
              @close="showMfaAlert = false"
            />

            <p class="text-muted mb-3">{{ t('login.mfa.instruction') }}</p>
            
            <form @submit.prevent="handleMfaVerification">
              <div class="mb-3">
                <label for="mfaCode" class="form-label">{{ t('login.mfa.code') }}</label>
                <input 
                  type="text" 
                  class="form-control text-center font-monospace fs-4" 
                  id="mfaCode"
                  v-model="mfaForm.code"
                  :placeholder="t('login.mfa.placeholder')"
                  maxlength="6"
                  pattern="[0-9]{6}"
                  required
                  autofocus
                />
                <div class="form-text">{{ t('login.mfa.help') }}</div>
              </div>
              
              <div class="d-flex gap-2">
                <button 
                  type="submit" 
                  class="btn btn-info flex-grow-1"
                  :disabled="isMfaLoading || mfaForm.code.length !== 6"
                >
                  <span v-if="isMfaLoading" class="spinner-border spinner-border-sm me-2"></span>
                  <i v-else class="bi bi-check-circle"></i>
                  {{ isMfaLoading ? t('login.mfa.verifying') : t('login.mfa.verify') }}
                </button>
                <button 
                  type="button" 
                  class="btn btn-secondary"
                  @click="closeMfaVerification"
                  :disabled="isMfaLoading"
                >
                  {{ t('common.cancel') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- MFA Setup Prompt Modal -->
    <div 
      v-if="showMfaPrompt" 
      class="modal show d-block" 
      tabindex="-1" 
      style="background-color: rgba(0,0,0,0.5); z-index: 2000; position: fixed; top: 0; left: 0; right: 0; bottom: 0;"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header bg-success text-white">
            <h5 class="modal-title">
              <i class="bi bi-shield-check"></i> {{ t('login.mfaPrompt.title') }}
            </h5>
          </div>
          <div class="modal-body">
            <div class="text-center mb-3">
              <i class="bi bi-shield-lock text-success" style="font-size: 3rem;"></i>
            </div>
            <p class="lead">{{ t('login.mfaPrompt.message') }}</p>
            <ul class="mb-3">
              <li>{{ t('login.mfaPrompt.benefit1') }}</li>
              <li>{{ t('login.mfaPrompt.benefit2') }}</li>
              <li>{{ t('login.mfaPrompt.benefit3') }}</li>
            </ul>
            <p class="text-muted small">{{ t('login.mfaPrompt.description') }}</p>
          </div>
          <div class="modal-footer">
            <button 
              type="button" 
              class="btn btn-outline-secondary"
              @click="dismissMfaPrompt"
            >
              <i class="bi bi-x-circle"></i>
              {{ t('login.mfaPrompt.notNow') }}
            </button>
            <button 
              type="button" 
              class="btn btn-success"
              @click="setupMfaNow"
            >
              <i class="bi bi-shield-check"></i>
              {{ t('login.mfaPrompt.enableNow') }}
            </button>
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
import { verifyMfaLogin } from '../services/mfaService'
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

// MFA verification state
const showMfaVerification = ref(false)
const mfaForm = ref({
  code: ''
})
const mfaToken = ref(null)
const isMfaLoading = ref(false)
const showMfaAlert = ref(false)
const mfaAlertMessage = ref('')
const mfaAlertType = ref('info')

// MFA prompt state
const showMfaPrompt = ref(false)
const shouldPromptMfaUser = ref(false)

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

    // Check if MFA is required
    if (response.mfaRequired) {
      // Store temporary MFA token
      mfaToken.value = response.mfaToken
      showMfaVerification.value = true
      showInfo(t('login.mfa.required'))
      return
    }

    // No MFA required - proceed with normal login
    // Set token first so subsequent API calls are authenticated
    authStore.setAuth(response.token, null)
    
    // Fetch user data after successful login
    const userData = await userService.getCurrentUser()
    
    // Update store with user data
    authStore.setAuth(response.token, userData)

    // Show MFA prompt if backend suggests it (user hasn't enabled MFA)
    if (response.shouldPromptMfa) {
      showMfaPrompt.value = true
      // Don't redirect - wait for user to make a choice
    } else {
      showSuccess(t('messages.loginSuccess'))
      setTimeout(() => router.push('/'), 1000)
    }
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

const showInfo = (message) => {
  alertMessage.value = message
  alertType.value = 'info'
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

// MFA Functions
const closeMfaVerification = () => {
  showMfaVerification.value = false
  mfaForm.value.code = ''
  mfaToken.value = null
  showMfaAlert.value = false
  isLoading.value = false
}

const handleMfaVerification = async () => {
  showMfaAlert.value = false

  if (mfaForm.value.code.length !== 6) {
    mfaAlertMessage.value = t('login.mfa.invalidLength')
    mfaAlertType.value = 'danger'
    showMfaAlert.value = true
    return
  }

  isMfaLoading.value = true

  try {
    const response = await verifyMfaLogin(mfaToken.value, mfaForm.value.code)

    // Set final token
    authStore.setAuth(response.token, null)
    
    // Fetch user data
    const userData = await userService.getCurrentUser()
    
    // Update store with user data
    authStore.setAuth(response.token, userData)

    // Close MFA modal and show success
    closeMfaVerification()
    showSuccess(t('login.mfa.success'))
    setTimeout(() => router.push('/'), 1000)
  } catch (error) {
    mfaAlertMessage.value = error.message || t('login.mfa.failed')
    mfaAlertType.value = 'danger'
    showMfaAlert.value = true
  } finally {
    isMfaLoading.value = false
  }
}

// MFA Prompt Functions
const dismissMfaPrompt = () => {
  showMfaPrompt.value = false
  showSuccess(t('messages.loginSuccess'))
  setTimeout(() => router.push('/'), 500)
}

const setupMfaNow = () => {
  showMfaPrompt.value = false
  router.push('/profile')
}
</script>

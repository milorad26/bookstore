<template>
  <div class="container mt-5">
    <div class="row justify-content-center">
      <div class="col-md-6">
        <div class="card shadow">
          <div class="card-body p-5">
            <h2 class="card-title text-center mb-4">
              <i class="bi bi-person-plus"></i> Create Account
            </h2>

            <Alert
              v-if="showAlert"
              :message="alertMessage"
              :type="alertType"
              @close="showAlert = false"
            />

            <form @submit.prevent="handleRegister">
              <div class="row">
                <div class="col-md-6">
                  <FormField
                    id="firstName"
                    v-model="form.firstName"
                    label="First Name"
                    placeholder="John"
                    :error="errors.firstName"
                    required
                    @blur="validateFieldFn('firstName')"
                  />
                </div>
                <div class="col-md-6">
                  <FormField
                    id="lastName"
                    v-model="form.lastName"
                    label="Last Name"
                    placeholder="Doe"
                    :error="errors.lastName"
                    required
                    @blur="validateFieldFn('lastName')"
                  />
                </div>
              </div>

              <FormField
                id="username"
                v-model="form.username"
                label="Username"
                placeholder="johndoe"
                :error="errors.username"
                required
                @blur="validateFieldFn('username')"
              />

              <FormField
                id="email"
                v-model="form.email"
                label="Email"
                type="email"
                placeholder="john@example.com"
                :error="errors.email"
                required
                @blur="validateFieldFn('email')"
              />

              <!-- Password Requirements Info -->
              <div class="alert alert-info mb-3 py-2">
                <small>
                  <strong><i class="bi bi-info-circle"></i> Password must have:</strong>
                  8+ chars, uppercase, lowercase & special character (!@#$%...)
                </small>
              </div>

              <FormField
                id="password"
                v-model="form.password"
                label="Password"
                type="password"
                placeholder="e.g., MyP@ssw0rd"
                :error="errors.password"
                required
                @blur="validateFieldFn('password')"
              />
              <small class="text-muted d-block mb-3">Min. 8 characters with uppercase, lowercase & special character</small>

              <FormField
                id="phoneNumber"
                v-model="form.phoneNumber"
                label="Phone Number"
                placeholder="+1 (555) 000-0000"
                :error="errors.phoneNumber"
              />

              <FormField
                id="address"
                v-model="form.address"
                label="Address"
                placeholder="123 Main St, City, State 12345"
                :error="errors.address"
              />

              <button
                type="submit"
                class="btn btn-primary w-100 mb-3"
                :disabled="isLoading"
              >
                <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
                {{ isLoading ? 'Creating Account...' : 'Register' }}
              </button>
            </form>

            <div class="text-center">
              <p class="text-muted">Already have an account?</p>
              <router-link to="/auth/login" class="btn btn-outline-primary">
                Login here
              </router-link>
            </div>
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
import { authService } from '../services/authService'
import { userService } from '../services/userService'
import { validateField } from '../utils/validators'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const authStore = useAuthStore()
const router = useRouter()

const form = ref({
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  phoneNumber: '',
  address: ''
})

const errors = ref({
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  phoneNumber: '',
  address: ''
})

const isLoading = ref(false)
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

const validate = () => {
  let isValid = true
  
  const requiredFields = ['firstName', 'lastName', 'username', 'email']
  for (const field of requiredFields) {
    const error = validateField(field, form.value[field])
    if (error) {
      errors.value[field] = error
      isValid = false
    }
  }
  
  // Validate password with strong validation rules
  const passwordError = validateField('registerPassword', form.value.password)
  if (passwordError) {
    errors.value.password = passwordError
    isValid = false
  }

  return isValid
}

const validateFieldFn = (field) => {
  // Use registerPassword validator for password field
  const validationField = field === 'password' ? 'registerPassword' : field
  const error = validateField(validationField, form.value[field])
  errors.value[field] = error || ''
}

const handleRegister = async () => {
  // Clear previous alerts and errors on new submission
  showAlert.value = false
  errors.value = {
    firstName: '', lastName: '', username: '', email: '', password: '', phoneNumber: '', address: ''
  }

  if (!validate()) {
    showError('Please fix the errors above')
    return
  }

  isLoading.value = true

  try {
    const response = await authService.register(form.value)

    // Set token first so subsequent API calls are authenticated
    authStore.setAuth(response.token, null)
    
    // Fetch user data after successful registration
    const userData = await userService.getCurrentUser()
    
    // Update store with user data
    authStore.setAuth(response.token, userData)

    showSuccess('Registration successful!')
    setTimeout(() => router.push('/'), 1000)
  } catch (error) {
    showError(error.message || 'Registration failed')
    
    // Parse field-specific validation errors
    if (error.message.includes(':')) {
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
</script>

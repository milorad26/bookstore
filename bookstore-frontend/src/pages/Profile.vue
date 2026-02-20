<template>
  <div class="profile-page">
    <div class="container py-5">
      <div class="row justify-content-center">
        <div class="col-lg-10">
          <!-- Alert Messages -->
          <Alert v-if="alert.show" :type="alert.type" :message="alert.message" @close="alert.show = false" />
          
          <!-- User Profile Header -->
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <div class="d-flex align-items-center mb-3">
                <div class="avatar-circle me-3">
                  <i class="bi bi-person-fill"></i>
                </div>
                <div>
                  <h3 class="mb-0">{{ profile.firstName }} {{ profile.lastName }}</h3>
                  <p class="text-muted mb-0">@{{ profile.username }}</p>
                  <span :class="getRoleBadgeClass(profile.userType)">
                    {{ formatRole(profile.userType) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Profile Information -->
          <div class="card shadow-sm mb-4">
            <div class="card-header bg-primary text-white">
              <h5 class="mb-0"><i class="bi bi-person-lines-fill"></i> {{ t('profile.personalInformation') }}</h5>
            </div>
            <div class="card-body">
              <div v-if="!editMode">
                <div class="row mb-3">
                  <div class="col-md-6">
                    <label class="fw-bold">{{ t('profile.email') }}:</label>
                    <p>{{ profile.email || t('common.notProvided') }}</p>
                  </div>
                  <div class="col-md-6">
                    <label class="fw-bold">{{ t('register.phoneNumber') }}:</label>
                    <p>{{ profile.phoneNumber || t('common.notProvided') }}</p>
                  </div>
                </div>
                <div class="row mb-3">
                  <div class="col-12">
                    <label class="fw-bold">{{ t('register.address') }}:</label>
                    <p>{{ profile.address || t('common.notProvided') }}</p>
                  </div>
                </div>
                <button @click="editMode = true" class="btn btn-primary">
                  <i class="bi bi-pencil"></i> {{ t('profile.updateProfile') }}
                </button>
              </div>

              <!-- Edit Form -->
              <form v-else @submit.prevent="updateProfile">
                <div class="row mb-3">
                  <div class="col-md-6">
                    <FormField
                      id="profile-firstName"
                      v-model="editForm.firstName"
                      :label="t('profile.firstName')"
                      type="text"
                      :placeholder="t('register.enterFirstName')"
                      required
                    />
                  </div>
                  <div class="col-md-6">
                    <FormField
                      id="profile-lastName"
                      v-model="editForm.lastName"
                      :label="t('profile.lastName')"
                      type="text"
                      :placeholder="t('register.enterLastName')"
                      required
                    />
                  </div>
                </div>
                <div class="row mb-3">
                  <div class="col-md-6">
                    <FormField
                      id="profile-email"
                      v-model="editForm.email"
                      :label="t('profile.email')"
                      type="email"
                      :placeholder="t('register.enterEmail')"
                    />
                  </div>
                  <div class="col-md-6">
                    <FormField
                      id="profile-phoneNumber"
                      v-model="editForm.phoneNumber"
                      :label="t('register.phoneNumber')"
                      type="text"
                      :placeholder="t('register.enterPhoneNumber')"
                    />
                  </div>
                </div>
                <div class="mb-3">
                  <FormField
                    id="profile-address"
                    v-model="editForm.address"
                    :label="t('register.address')"
                    type="text"
                    :placeholder="t('register.enterAddress')"
                  />
                </div>
                <div class="d-flex gap-2">
                  <button type="submit" class="btn btn-success" :disabled="loading">
                    <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                    <i v-else class="bi bi-check-lg"></i>
                    {{ t('common.save') }} {{ t('common.changes') }}
                  </button>
                  <button type="button" @click="cancelEdit" class="btn btn-secondary" :disabled="loading">
                    <i class="bi bi-x-lg"></i> {{ t('common.cancel') }}
                  </button>
                </div>
              </form>
            </div>
          </div>

          <!-- Change Password -->
          <div class="card shadow-sm mb-4">
            <div class="card-header bg-warning text-dark">
              <h5 class="mb-0"><i class="bi bi-key-fill"></i> {{ t('profile.changePassword') }}</h5>
            </div>
            <div class="card-body">
              <div v-if="!changePasswordMode">
                <p class="text-muted">{{ t('profile.updatePasswordMessage') }}</p>
                <button @click="changePasswordMode = true" class="btn btn-warning">
                  <i class="bi bi-key"></i> {{ t('profile.changePassword') }}
                </button>
              </div>

              <!-- Change Password Form -->
              <form v-else @submit.prevent="changePassword">
                <div class="mb-3">
                  <FormField
                    id="password-current"
                    v-model="passwordForm.currentPassword"
                    :label="t('profile.currentPassword')"
                    type="password"
                    :placeholder="t('profile.enterCurrentPassword')"
                    required
                  />
                </div>
                <div class="mb-3">
                  <FormField
                    id="password-new"
                    v-model="passwordForm.newPassword"
                    :label="t('profile.newPassword')"
                    type="password"
                    :placeholder="t('register.enterPassword')"
                    required
                  />
                </div>
                <div class="mb-3">
                  <FormField
                    id="password-confirm"
                    v-model="passwordForm.confirmPassword"
                    :label="t('profile.confirmPassword')"
                    type="password"
                    :placeholder="t('profile.confirmNewPassword')"
                    required
                  />
                </div>
                <div class="d-flex gap-2">
                  <button type="submit" class="btn btn-success" :disabled="loading">
                    <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                    <i v-else class="bi bi-check-lg"></i>
                    {{ t('profile.changePassword') }}
                  </button>
                  <button type="button" @click="cancelPasswordChange" class="btn btn-secondary" :disabled="loading">
                    <i class="bi bi-x-lg"></i> {{ t('common.cancel') }}
                  </button>
                </div>
              </form>
            </div>
          </div>

          <!-- Quick Actions -->
          <div class="card shadow-sm">
            <div class="card-header bg-success text-white">
              <h5 class="mb-0"><i class="bi bi-lightning-charge-fill"></i> {{ t('profile.quickLinks') }}</h5>
            </div>
            <div class="card-body">
              
              <div class="actions-grid">
                <!-- User Management Actions (ADMIN & SUPER_USER) -->
                <div v-if="canManageUsers" class="action-card">
                  <div class="action-icon bg-primary">
                    <i class="bi bi-people-fill"></i>
                  </div>
                  <h6>{{ t('admin.userManagement') }}</h6>
                  <p class="text-muted small">{{ t('profile.userManagementDesc') }}</p>
                  <button @click="viewAllUsers" class="btn btn-primary btn-sm w-100">
                    <i class="bi bi-eye"></i> {{ t('admin.viewAllUsers') }}
                  </button>
                </div>

                <!-- Book Management Actions (ADMIN) -->
                <div v-if="canManageBooks" class="action-card">
                  <div class="action-icon bg-warning">
                    <i class="bi bi-book-fill"></i>
                  </div>
                  <h6>{{ t('admin.bookManagement') }}</h6>
                  <p class="text-muted small">{{ t('profile.bookManagementDesc') }}</p>
                  <button @click="manageBooks" class="btn btn-warning btn-sm w-100">
                    <i class="bi bi-pencil-square"></i> {{ t('profile.manageBooks') }}
                  </button>
                </div>

                <!-- Order Management Actions (ADMIN & SUPER_USER) -->
                <div v-if="canManageOrders" class="action-card">
                  <div class="action-icon bg-info">
                    <i class="bi bi-cart-check-fill"></i>
                  </div>
                  <h6>{{ t('admin.orderManagement') }}</h6>
                  <p class="text-muted small">{{ t('profile.orderManagementDesc') }}</p>
                  <button @click="viewAllOrders" class="btn btn-info btn-sm w-100">
                    <i class="bi bi-list-ul"></i> {{ t('admin.viewAllOrders') }}
                  </button>
                </div>

                <!-- My Orders (ALL USERS) -->
                <div class="action-card">
                  <div class="action-icon bg-secondary">
                    <i class="bi bi-bag-check-fill"></i>
                  </div>
                  <h6>{{ t('orders.myOrders') }}</h6>
                  <p class="text-muted small">{{ t('orders.viewHistory') }}</p>
                  <button @click="viewMyOrders" class="btn btn-secondary btn-sm w-100">
                    <i class="bi bi-clock-history"></i> {{ t('profile.viewOrders') }}
                  </button>
                </div>

                <!-- Browse Books (ALL USERS) -->
                <div class="action-card">
                  <div class="action-icon bg-success">
                    <i class="bi bi-shop"></i>
                  </div>
                  <h6>{{ t('profile.browseBooks') }}</h6>
                  <p class="text-muted small">{{ t('profile.browseBooksDesc') }}</p>
                  <button @click="browseBooks" class="btn btn-success btn-sm w-100">
                    <i class="bi bi-search"></i> {{ t('profile.browseBooks') }}
                  </button>
                </div>

                <!-- Confirm Orders (ADMIN & SUPER_USER) -->
                <div v-if="canConfirmOrders" class="action-card">
                  <div class="action-icon bg-primary">
                    <i class="bi bi-check-circle-fill"></i>
                  </div>
                  <h6>{{ t('profile.confirmOrders') }}</h6>
                  <p class="text-muted small">{{ t('profile.confirmOrdersDesc') }}</p>
                  <button @click="confirmOrders" class="btn btn-primary btn-sm w-100">
                    <i class="bi bi-check2-all"></i> {{ t('profile.reviewOrders') }}
                  </button>
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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { userService } from '../services/userService'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()
const { t } = useI18n()

const profile = ref({
  id: null,
  username: '',
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  address: '',
  userType: '',
  permissions: [],
  availableEndpoints: []
})

const editMode = ref(false)
const changePasswordMode = ref(false)
const loading = ref(false)

const editForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  address: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const alert = reactive({
  show: false,
  type: 'success',
  message: ''
})

// Ensure alert is hidden on component mount
watch(() => alert.show, (newVal) => {
  if (newVal) {
    setTimeout(() => {
      alert.show = false
    }, 5000)
  }
})

// Computed permissions based on user role
const canManageUsers = computed(() => 
  profile.value.userType === 'ADMIN' || profile.value.userType === 'SUPER_USER'
)

const canManageBooks = computed(() => 
  profile.value.userType === 'ADMIN'
)

const canManageOrders = computed(() => 
  profile.value.userType === 'ADMIN' || profile.value.userType === 'SUPER_USER'
)

const canConfirmOrders = computed(() => 
  profile.value.userType === 'ADMIN' || profile.value.userType === 'SUPER_USER'
)

const showAlert = (type, message) => {
  alert.type = type
  alert.message = message
  alert.show = true
  
  // Scroll to top to ensure user sees the notification
  window.scrollTo({ top: 0, behavior: 'smooth' })
  
  setTimeout(() => {
    alert.show = false
  }, 5000)
}

const loadProfile = async () => {
  loading.value = true
  try {
    const data = await userService.getCurrentUser()
    profile.value = data
  } catch (error) {
    showAlert('danger', error.message || t('profile.loadProfileFailed'))
  } finally {
    loading.value = false
  }
}

const updateProfile = async () => {
  loading.value = true
  try {
    const updatedData = await userService.updateCurrentUser({
      firstName: editForm.firstName,
      lastName: editForm.lastName,
      email: editForm.email,
      phoneNumber: editForm.phoneNumber,
      address: editForm.address,
      enabled: profile.value.enabled,
      userType: profile.value.userType
    })
    profile.value = updatedData
    editMode.value = false
    showAlert('success', t('profile.profileUpdateSuccess'))
  } catch (error) {
    showAlert('danger', error.message || t('profile.profileUpdateFailed'))
  } finally {
    loading.value = false
  }
}

const cancelEdit = () => {
  editMode.value = false
  // Reset form with current profile data
  editForm.firstName = profile.value.firstName
  editForm.lastName = profile.value.lastName
  editForm.email = profile.value.email
  editForm.phoneNumber = profile.value.phoneNumber
  editForm.address = profile.value.address
}

const changePassword = async () => {
  // Client-side validation
  if (!passwordForm.currentPassword) {
    showAlert('danger', t('profile.enterCurrentPassword'))
    return
  }

  if (!passwordForm.newPassword) {
    showAlert('danger', t('profile.enterNewPassword'))
    return
  }

  if (!passwordForm.confirmPassword) {
    showAlert('danger', t('profile.confirmNewPasswordMessage'))
    return
  }

  // Validate passwords match
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showAlert('danger', t('profile.passwordsDoNotMatch'))
    return
  }

  // Validate password length
  if (passwordForm.newPassword.length < 8) {
    showAlert('danger', t('profile.passwordTooShort'))
    return
  }

  // Validate uppercase letter
  if (!/[A-Z]/.test(passwordForm.newPassword)) {
    showAlert('danger', t('profile.passwordNeedsUppercase'))
    return
  }

  // Validate lowercase letter
  if (!/[a-z]/.test(passwordForm.newPassword)) {
    showAlert('danger', t('profile.passwordNeedsLowercase'))
    return
  }

  // Validate special character
  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(passwordForm.newPassword)) {
    showAlert('danger', t('profile.passwordNeedsSpecialChar'))
    return
  }

  // Check if new password is same as current
  if (passwordForm.currentPassword === passwordForm.newPassword) {
    showAlert('warning', t('profile.passwordMustBeDifferent'))
    return
  }

  loading.value = true
  try {
    await userService.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
    showAlert('success', t('profile.passwordChangeSuccess'))
    cancelPasswordChange()
  } catch (error) {
    // Provide specific error messages
    let errorMessage = t('profile.passwordChangeFailed') + ' '
    
    if (error.message && error.message.includes('incorrect')) {
      errorMessage = t('profile.currentPasswordIncorrect')
    } else if (error.message) {
      errorMessage = error.message
    } else {
      errorMessage += t('profile.tryAgainOrContact')
    }
    
    showAlert('danger', errorMessage)
  } finally {
    loading.value = false
  }
}

const cancelPasswordChange = () => {
  changePasswordMode.value = false
  // Reset password form
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const formatRole = (role) => {
  if (!role) return ''
  return role.replace('_', ' ')
}

const getRoleBadgeClass = (role) => {
  const baseClass = 'badge ms-2 '
  switch (role) {
    case 'ADMIN':
      return baseClass + 'bg-danger'
    case 'SUPER_USER':
      return baseClass + 'bg-warning text-dark'
    case 'USER':
      return baseClass + 'bg-secondary'
    default:
      return baseClass + 'bg-secondary'
  }
}

// Action handlers
const viewAllUsers = () => {
  router.push('/users')
}

const manageBooks = () => {
  router.push('/books')
}

const viewAllOrders = () => {
  router.push('/admin/orders')
}

const viewMyOrders = () => {
  router.push('/orders')
}

const browseBooks = () => {
  router.push('/')
}

const confirmOrders = () => {
  showAlert('info', t('profile.orderConfirmationSoon'))
  // TODO: Navigate to order confirmation page
  // router.push('/orders/confirm')
}

onMounted(async () => {
  await loadProfile()
  // Initialize edit form with current profile data
  editForm.firstName = profile.value.firstName
  editForm.lastName = profile.value.lastName
  editForm.email = profile.value.email
  editForm.phoneNumber = profile.value.phoneNumber
  editForm.address = profile.value.address
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  color: white;
}

.card {
  border: none;
  border-radius: 10px;
}

.card-header {
  border-radius: 10px 10px 0 0 !important;
  padding: 1rem 1.5rem;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
}

.action-card {
  background: white;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  padding: 1.5rem;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.action-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.1);
  border-color: #0d6efd;
}

.action-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 1.8rem;
  color: white;
}

.action-card h6 {
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #2c3e50;
}

.action-card p {
  margin-bottom: 1rem;
  min-height: 40px;
}

.action-card .btn {
  transition: all 0.2s ease;
}

.action-card .btn:hover {
  transform: scale(1.05);
}
</style>

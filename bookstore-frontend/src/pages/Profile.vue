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

          <!-- Multi-Factor Authentication (2FA) -->
          <div class="card shadow-sm mb-4">
            <div class="card-header bg-info text-white">
              <h5 class="mb-0"><i class="bi bi-shield-lock-fill"></i> {{ t('profile.mfa.title') }}</h5>
            </div>
            <div class="card-body">
              <!-- MFA Status Display -->
              <div v-if="!mfaSetupMode">
                <div class="d-flex align-items-center mb-3">
                  <div :class="['mfa-status-icon', mfaEnabled ? 'enabled' : 'disabled']">
                    <i :class="['bi', mfaEnabled ? 'bi-shield-check' : 'bi-shield-x']"></i>
                  </div>
                  <div class="ms-3">
                    <h6 class="mb-0">
                      {{ mfaEnabled ? t('profile.mfa.enabled') : t('profile.mfa.disabled') }}
                    </h6>
                    <p class="text-muted small mb-0">
                      {{ mfaEnabled ? t('profile.mfa.statusActive') : t('profile.mfa.statusInactive') }}
                    </p>
                  </div>
                </div>
                
                <p class="text-muted mb-3">{{ t('profile.mfa.description') }}</p>
                
                <!-- Enable MFA -->
                <button 
                  v-if="!mfaEnabled" 
                  @click="startMfaSetup" 
                  class="btn btn-info"
                  :disabled="loading"
                >
                  <i class="bi bi-shield-plus"></i> {{ t('profile.mfa.enable') }}
                </button>
                
                <!-- Disable MFA -->
                <button 
                  v-else 
                  @click="mfaDisableMode = true" 
                  class="btn btn-warning"
                  :disabled="loading"
                >
                  <i class="bi bi-shield-minus"></i> {{ t('profile.mfa.disable') }}
                </button>
              </div>

              <!-- MFA Setup/Enable Form -->
              <div v-else>
                <div v-if="!mfaQrCode">
                  <div class="text-center py-3">
                    <div class="spinner-border text-info" role="status">
                      <span class="visually-hidden">{{ t('common.loading') }}</span>
                    </div>
                    <p class="mt-2 text-muted">{{ t('profile.mfa.generating') }}</p>
                  </div>
                </div>
                
                <div v-else>
                  <div class="alert alert-info">
                    <i class="bi bi-info-circle"></i> {{ t('profile.mfa.setupInstructions') }}
                  </div>
                  
                  <!-- Step 1: Scan QR Code -->
                  <div class="mfa-setup-step mb-4">
                    <h6><span class="badge bg-info me-2">1</span>{{ t('profile.mfa.step1') }}</h6>
                    <div class="text-center my-3">
                      <img :src="mfaQrCode" alt="QR Code" class="mfa-qr-code" />
                    </div>
                    <p class="small text-muted text-center">
                      {{ t('profile.mfa.scanWithApp') }}
                    </p>
                  </div>

                  <!-- Manual Entry Option -->
                  <div class="mfa-setup-step mb-4">
                    <h6><i class="bi bi-keyboard"></i> {{ t('profile.mfa.manualEntry') }}</h6>
                    <div class="input-group">
                      <input 
                        type="text" 
                        class="form-control font-monospace" 
                        :value="mfaManualKey" 
                        readonly
                      />
                      <button 
                        class="btn btn-outline-secondary" 
                        type="button"
                        @click="copyToClipboard(mfaManualKey)"
                      >
                        <i class="bi bi-clipboard"></i>
                      </button>
                    </div>
                  </div>

                  <!-- Step 2: Verify Code -->
                  <div class="mfa-setup-step mb-4">
                    <h6><span class="badge bg-info me-2">2</span>{{ t('profile.mfa.step2') }}</h6>
                    <form @submit.prevent="completeMfaSetup">
                      <div class="mb-3">
                        <label for="mfa-code" class="form-label">{{ t('profile.mfa.enterCode') }}</label>
                        <input 
                          type="text" 
                          class="form-control text-center font-monospace fs-4" 
                          id="mfa-code"
                          v-model="mfaVerificationCode"
                          :placeholder="t('profile.mfa.codePlaceholder')"
                          maxlength="6"
                          pattern="[0-9]{6}"
                          required
                        />
                        <div class="form-text">{{ t('profile.mfa.codeHelp') }}</div>
                      </div>
                      
                      <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-success" :disabled="loading || mfaVerificationCode.length !== 6">
                          <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                          <i v-else class="bi bi-check-circle"></i>
                          {{ t('profile.mfa.verify') }}
                        </button>
                        <button type="button" @click="cancelMfaSetup" class="btn btn-secondary" :disabled="loading">
                          <i class="bi bi-x-circle"></i> {{ t('common.cancel') }}
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              </div>

              <!-- MFA Disable Mode -->
              <div v-if="mfaDisableMode && mfaEnabled" class="mt-3">
                <div class="alert alert-warning">
                  <i class="bi bi-exclamation-triangle"></i> {{ t('profile.mfa.disableWarning') }}
                </div>
                <form @submit.prevent="disableMfaAuth">
                  <div class="mb-3">
                    <label for="disable-mfa-code" class="form-label">{{ t('profile.mfa.enterCodeToDisable') }}</label>
                    <input 
                      type="text" 
                      class="form-control text-center font-monospace" 
                      id="disable-mfa-code"
                      v-model="mfaDisableCode"
                      :placeholder="t('profile.mfa.codePlaceholder')"
                      maxlength="6"
                      pattern="[0-9]{6}"
                      required
                    />
                  </div>
                  <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-warning" :disabled="loading || mfaDisableCode.length !== 6">
                      <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                      <i v-else class="bi bi-shield-x"></i>
                      {{ t('profile.mfa.confirmDisable') }}
                    </button>
                    <button type="button" @click="mfaDisableMode = false; mfaDisableCode = ''" class="btn btn-secondary" :disabled="loading">
                      <i class="bi bi-x-circle"></i> {{ t('common.cancel') }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <!-- My Coupons -->
          <div class="card shadow-sm mb-4">
            <div class="card-header bg-success text-white">
              <h5 class="mb-0">
                <i class="bi bi-ticket-perforated-fill"></i> {{ t('profile.myCoupons.title') }}
              </h5>
            </div>
            <div class="card-body">
              <p class="text-muted mb-3">{{ t('profile.myCoupons.subtitle') }}</p>
              
              <div v-if="loadingCoupons" class="text-center py-4">
                <div class="spinner-border text-success" role="status">
                  <span class="visually-hidden">{{ t('common.loading') }}</span>
                </div>
              </div>

              <div v-else-if="availableCoupons.length === 0 && usedCoupons.length === 0" class="text-center py-4">
                <i class="bi bi-ticket-perforated" style="font-size: 3rem; color: #ccc;"></i>
                <p class="text-muted mt-3">{{ t('profile.myCoupons.noCoupons') }}</p>
                <p class="small text-muted">{{ t('profile.myCoupons.earnInfo') }}</p>
              </div>

              <div v-else>
                <!-- Available Coupons -->
                <div v-if="availableCoupons.length > 0" class="mb-4">
                  <h6 class="text-success mb-3">
                    <i class="bi bi-check-circle-fill"></i> {{ t('profile.myCoupons.available') }} ({{ availableCoupons.length }})
                  </h6>
                  <div class="coupons-grid">
                    <div 
                      v-for="coupon in availableCoupons" 
                      :key="coupon.id"
                      class="coupon-card available"
                    >
                      <div class="coupon-header">
                        <div class="coupon-code">{{ coupon.code }}</div>
                        <div class="coupon-badge">{{ t('profile.myCoupons.active') }}</div>
                      </div>
                      <div class="coupon-value">${{ coupon.value }}</div>
                      <div class="coupon-details">
                        <div class="coupon-detail">
                          <i class="bi bi-calendar-check"></i>
                          {{ t('profile.myCoupons.expires') }}: {{ formatDate(coupon.expiryDate) }}
                        </div>
                        <div v-if="coupon.earnedFromOrderId" class="coupon-detail">
                          <i class="bi bi-bag-check"></i>
                          {{ t('profile.myCoupons.earnedFrom') }} #{{ coupon.earnedFromOrderId }}
                        </div>
                      </div>
                      <button 
                        @click="copyCouponCode(coupon.code)" 
                        class="btn btn-sm btn-outline-success w-100 mt-2"
                      >
                        <i class="bi bi-clipboard"></i> {{ t('profile.myCoupons.copyCode') }}
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Used Coupons -->
                <div v-if="usedCoupons.length > 0">
                  <h6 class="text-muted mb-3">
                    <i class="bi bi-check2-circle"></i> {{ t('profile.myCoupons.used') }} ({{ usedCoupons.length }})
                  </h6>
                  <div class="coupons-grid">
                    <div 
                      v-for="coupon in usedCoupons" 
                      :key="coupon.id"
                      class="coupon-card used"
                    >
                      <div class="coupon-header">
                        <div class="coupon-code text-muted">{{ coupon.code }}</div>
                        <div class="coupon-badge used-badge">{{ t('profile.myCoupons.usedBadge') }}</div>
                      </div>
                      <div class="coupon-value text-muted">${{ coupon.value }}</div>
                      <div class="coupon-details">
                        <div v-if="coupon.usedInOrderId" class="coupon-detail">
                          <i class="bi bi-cart-check"></i>
                          {{ t('profile.myCoupons.usedIn') }} #{{ coupon.usedInOrderId }}
                        </div>
                        <div class="coupon-detail">
                          <i class="bi bi-clock-history"></i>
                          {{ t('profile.myCoupons.usedOn') }}: {{ formatDate(coupon.usedAt) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
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
import { getUserCoupons } from '../services/couponService'
import { setupMfa, enableMfa, disableMfa, getMfaStatus } from '../services/mfaService'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()
const { t, locale } = useI18n()

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

// MFA state
const mfaEnabled = ref(false)
const mfaSetupMode = ref(false)
const mfaQrCode = ref(null)
const mfaManualKey = ref('')
const mfaSecret = ref('')
const mfaVerificationCode = ref('')
const mfaDisableMode = ref(false)
const mfaDisableCode = ref('')

const alert = reactive({
  show: false,
  type: 'success',
  message: ''
})

// Coupons state
const coupons = ref([])
const loadingCoupons = ref(false)

const availableCoupons = computed(() => 
  coupons.value.filter(c => c.canBeUsed)
)

const usedCoupons = computed(() => 
  coupons.value.filter(c => c.used)
)

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
    // Check MFA status
    await checkMfaStatus()
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

// MFA Functions
const checkMfaStatus = async () => {
  try {
    const status = await getMfaStatus()
    mfaEnabled.value = status.mfaEnabled
  } catch (error) {
    console.error('Failed to check MFA status:', error)
  }
}

const startMfaSetup = async () => {
  mfaSetupMode.value = true
  loading.value = true
  
  try {
    const setupData = await setupMfa()
    mfaQrCode.value = setupData.qrCodeDataUri
    mfaManualKey.value = setupData.manualEntryKey
    mfaSecret.value = setupData.secret
    showAlert('info', t('profile.mfa.setupStarted'))
  } catch (error) {
    showAlert('danger', error.message || t('profile.mfa.setupFailed'))
    mfaSetupMode.value = false
  } finally {
    loading.value = false
  }
}

const completeMfaSetup = async () => {
  if (mfaVerificationCode.value.length !== 6) {
    showAlert('danger', t('profile.mfa.invalidCodeLength'))
    return
  }
  
  loading.value = true
  try {
    await enableMfa(mfaSecret.value, mfaVerificationCode.value)
    mfaEnabled.value = true
    showAlert('success', t('profile.mfa.enableSuccess'))
    cancelMfaSetup()
  } catch (error) {
    showAlert('danger', error.message || t('profile.mfa.enableFailed'))
  } finally {
    loading.value = false
  }
}

const cancelMfaSetup = () => {
  mfaSetupMode.value = false
  mfaQrCode.value = null
  mfaManualKey.value = ''
  mfaSecret.value = ''
  mfaVerificationCode.value = ''
}

const disableMfaAuth = async () => {
  if (mfaDisableCode.value.length !== 6) {
    showAlert('danger', t('profile.mfa.invalidCodeLength'))
    return
  }
  
  loading.value = true
  try {
    await disableMfa(mfaDisableCode.value)
    mfaEnabled.value = false
    mfaDisableMode.value = false
    mfaDisableCode.value = ''
    showAlert('success', t('profile.mfa.disableSuccess'))
  } catch (error) {
    showAlert('danger', error.message || t('profile.mfa.disableFailed'))
  } finally {
    loading.value = false
  }
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    showAlert('success', t('profile.mfa.copiedToClipboard'))
  } catch (error) {
    showAlert('danger', t('profile.mfa.copyFailed'))
  }
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

const loadCoupons = async () => {
  loadingCoupons.value = true
  try {
    coupons.value = await getUserCoupons()
  } catch (error) {
    showAlert('danger', t('profile.myCoupons.loadFailed'))
  } finally {
    loadingCoupons.value = false
  }
}

const copyCouponCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    showAlert('success', t('profile.myCoupons.codeCopied'))
  } catch (error) {
    showAlert('info', `${t('profile.myCoupons.code')}: ${code}`)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString(locale.value === 'sr' ? 'sr-RS' : 'en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

onMounted(async () => {
  await loadProfile()
  await loadCoupons()
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

/* Coupons Styles */
.coupons-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.coupon-card {
  background: white;
  border-radius: 10px;
  padding: 1.25rem;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.coupon-card.available {
  border: 2px solid #28a745;
  background: linear-gradient(135deg, #ffffff 0%, #f0f8f0 100%);
}

.coupon-card.available:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(40, 167, 69, 0.2);
}

.coupon-card.used {
  border: 2px solid #dee2e6;
  background: #f8f9fa;
  opacity: 0.8;
}

.coupon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.coupon-code {
  font-family: 'Courier New', monospace;
  font-weight: bold;
  font-size: 1.1rem;
  color: #28a745;
  letter-spacing: 1px;
}

.coupon-card.used .coupon-code {
  color: #6c757d;
}

.coupon-badge {
  background: #28a745;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
}

.coupon-badge.used-badge {
  background: #6c757d;
}

.coupon-value {
  font-size: 2rem;
  font-weight: bold;
  color: #28a745;
  margin: 0.5rem 0;
}

.coupon-card.used .coupon-value {
  color: #6c757d;
}

.coupon-details {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px dashed #dee2e6;
}

.coupon-detail {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #6c757d;
  margin-bottom: 0.25rem;
}

.coupon-detail i {
  color: #28a745;
}

.coupon-card.used .coupon-detail i {
  color: #6c757d;
}

/* MFA Styles */
.mfa-status-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
}

.mfa-status-icon.enabled {
  background: #d4edda;
  color: #155724;
}

.mfa-status-icon.disabled {
  background: #f8d7da;
  color: #721c24;
}

.mfa-qr-code {
  max-width: 250px;
  border: 3px solid #dee2e6;
  border-radius: 10px;
  padding: 10px;
  background: white;
}

.mfa-setup-step {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1rem;
}

.mfa-setup-step h6 {
  color: #495057;
  margin-bottom: 0.75rem;
}

.font-monospace {
  font-family: 'Courier New', Courier, monospace;
}
</style>

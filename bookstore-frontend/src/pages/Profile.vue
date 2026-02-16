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
              <h5 class="mb-0"><i class="bi bi-person-lines-fill"></i> Profile Information</h5>
            </div>
            <div class="card-body">
              <div v-if="!editMode">
                <div class="row mb-3">
                  <div class="col-md-6">
                    <label class="fw-bold">Email:</label>
                    <p>{{ profile.email || 'Not provided' }}</p>
                  </div>
                  <div class="col-md-6">
                    <label class="fw-bold">Phone Number:</label>
                    <p>{{ profile.phoneNumber || 'Not provided' }}</p>
                  </div>
                </div>
                <div class="row mb-3">
                  <div class="col-12">
                    <label class="fw-bold">Address:</label>
                    <p>{{ profile.address || 'Not provided' }}</p>
                  </div>
                </div>
                <button @click="editMode = true" class="btn btn-primary">
                  <i class="bi bi-pencil"></i> Edit Profile
                </button>
              </div>

              <!-- Edit Form -->
              <form v-else @submit.prevent="updateProfile">
                <div class="row mb-3">
                  <div class="col-md-6">
                    <FormField
                      v-model="editForm.firstName"
                      label="First Name"
                      type="text"
                      placeholder="Enter first name"
                      required
                    />
                  </div>
                  <div class="col-md-6">
                    <FormField
                      v-model="editForm.lastName"
                      label="Last Name"
                      type="text"
                      placeholder="Enter last name"
                      required
                    />
                  </div>
                </div>
                <div class="row mb-3">
                  <div class="col-md-6">
                    <FormField
                      v-model="editForm.email"
                      label="Email"
                      type="email"
                      placeholder="Enter email address"
                    />
                  </div>
                  <div class="col-md-6">
                    <FormField
                      v-model="editForm.phoneNumber"
                      label="Phone Number"
                      type="text"
                      placeholder="Enter phone number"
                    />
                  </div>
                </div>
                <div class="mb-3">
                  <FormField
                    v-model="editForm.address"
                    label="Address"
                    type="text"
                    placeholder="Enter address"
                  />
                </div>
                <div class="d-flex gap-2">
                  <button type="submit" class="btn btn-success" :disabled="loading">
                    <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                    <i v-else class="bi bi-check-lg"></i>
                    Save Changes
                  </button>
                  <button type="button" @click="cancelEdit" class="btn btn-secondary" :disabled="loading">
                    <i class="bi bi-x-lg"></i> Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>

          <!-- Quick Actions -->
          <div class="card shadow-sm">
            <div class="card-header bg-success text-white">
              <h5 class="mb-0"><i class="bi bi-lightning-charge-fill"></i> Quick Actions</h5>
            </div>
            <div class="card-body">
              <p class="text-muted mb-4">Based on your role, here are the actions you can perform:</p>
              
              <div class="actions-grid">
                <!-- User Management Actions (ADMIN & SUPER_USER) -->
                <div v-if="canManageUsers" class="action-card">
                  <div class="action-icon bg-primary">
                    <i class="bi bi-people-fill"></i>
                  </div>
                  <h6>User Management</h6>
                  <p class="text-muted small">Create and manage user accounts</p>
                  <button @click="viewAllUsers" class="btn btn-primary btn-sm w-100">
                    <i class="bi bi-eye"></i> View All Users
                  </button>
                </div>

                <!-- Book Management Actions (ADMIN) -->
                <div v-if="canManageBooks" class="action-card">
                  <div class="action-icon bg-warning">
                    <i class="bi bi-book-fill"></i>
                  </div>
                  <h6>Book Management</h6>
                  <p class="text-muted small">Add, edit, and delete books</p>
                  <button @click="manageBooks" class="btn btn-warning btn-sm w-100">
                    <i class="bi bi-pencil-square"></i> Manage Books
                  </button>
                </div>

                <!-- Order Management Actions (ADMIN & SUPER_USER) -->
                <div v-if="canManageOrders" class="action-card">
                  <div class="action-icon bg-info">
                    <i class="bi bi-cart-check-fill"></i>
                  </div>
                  <h6>Order Management</h6>
                  <p class="text-muted small">View and manage all orders</p>
                  <button @click="viewAllOrders" class="btn btn-info btn-sm w-100">
                    <i class="bi bi-list-ul"></i> View All Orders
                  </button>
                </div>

                <!-- My Orders (ALL USERS) -->
                <div class="action-card">
                  <div class="action-icon bg-secondary">
                    <i class="bi bi-bag-check-fill"></i>
                  </div>
                  <h6>My Orders</h6>
                  <p class="text-muted small">View your order history</p>
                  <button @click="viewMyOrders" class="btn btn-secondary btn-sm w-100">
                    <i class="bi bi-clock-history"></i> View My Orders
                  </button>
                </div>

                <!-- Browse Books (ALL USERS) -->
                <div class="action-card">
                  <div class="action-icon bg-success">
                    <i class="bi bi-shop"></i>
                  </div>
                  <h6>Browse Books</h6>
                  <p class="text-muted small">Explore our book collection</p>
                  <button @click="browseBooks" class="btn btn-success btn-sm w-100">
                    <i class="bi bi-search"></i> Browse Books
                  </button>
                </div>

                <!-- Create Order (ALL USERS) -->
                <div class="action-card">
                  <div class="action-icon bg-danger">
                    <i class="bi bi-cart-plus-fill"></i>
                  </div>
                  <h6>Create Order</h6>
                  <p class="text-muted small">Place a new book order</p>
                  <button @click="createOrder" class="btn btn-danger btn-sm w-100">
                    <i class="bi bi-plus-circle"></i> New Order
                  </button>
                </div>

                <!-- Delete Users (ADMIN ONLY) -->
                <div v-if="canDeleteUsers" class="action-card">
                  <div class="action-icon bg-dark">
                    <i class="bi bi-trash-fill"></i>
                  </div>
                  <h6>Delete Users</h6>
                  <p class="text-muted small">Remove user accounts</p>
                  <button @click="deleteUsers" class="btn btn-dark btn-sm w-100">
                    <i class="bi bi-person-x"></i> Manage Deletions
                  </button>
                </div>

                <!-- Confirm Orders (ADMIN & SUPER_USER) -->
                <div v-if="canConfirmOrders" class="action-card">
                  <div class="action-icon bg-primary">
                    <i class="bi bi-check-circle-fill"></i>
                  </div>
                  <h6>Confirm Orders</h6>
                  <p class="text-muted small">Approve pending orders</p>
                  <button @click="confirmOrders" class="btn btn-primary btn-sm w-100">
                    <i class="bi bi-check2-all"></i> Review Orders
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
import { ref, reactive, onMounted, computed } from 'vue'
import { userService } from '../services/userService'
import { useRouter } from 'vue-router'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()

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
const loading = ref(false)

const editForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  address: ''
})

const alert = reactive({
  show: false,
  type: 'success',
  message: ''
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

const canDeleteUsers = computed(() => 
  profile.value.userType === 'ADMIN'
)

const canConfirmOrders = computed(() => 
  profile.value.userType === 'ADMIN' || profile.value.userType === 'SUPER_USER'
)

const showAlert = (type, message) => {
  alert.type = type
  alert.message = message
  alert.show = true
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
    showAlert('danger', error.message || 'Failed to load profile')
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
    showAlert('success', 'Profile updated successfully!')
  } catch (error) {
    showAlert('danger', error.message || 'Failed to update profile')
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
  showAlert('info', 'User management interface coming soon!')
  // TODO: Navigate to user management page
  // router.push('/users')
}

const manageBooks = () => {
  showAlert('info', 'Book management interface coming soon!')
  // TODO: Navigate to book management page
  // router.push('/books/manage')
}

const viewAllOrders = () => {
  showAlert('info', 'Order management interface coming soon!')
  // TODO: Navigate to order management page
  // router.push('/orders/all')
}

const viewMyOrders = () => {
  showAlert('info', 'My orders page coming soon!')
  // TODO: Navigate to user's orders page
  // router.push('/orders/my')
}

const browseBooks = () => {
  router.push('/')
}

const createOrder = () => {
  showAlert('info', 'Order creation interface coming soon!')
  // TODO: Navigate to create order page
  // router.push('/orders/create')
}

const deleteUsers = () => {
  showAlert('info', 'User deletion interface coming soon!')
  // TODO: Navigate to user deletion page
  // router.push('/users/delete')
}

const confirmOrders = () => {
  showAlert('info', 'Order confirmation interface coming soon!')
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

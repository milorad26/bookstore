<template>
  <div class="users-page">
    <div class="container py-5">
      <div class="row">
        <div class="col-12">
          <!-- Alert Messages -->
          <Alert v-if="alert.show" :type="alert.type" :message="alert.message" @close="alert.show = false" />
          
          <!-- Header -->
          <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h2><i class="bi bi-people-fill"></i> {{ t('users.title') }}</h2>
              <p class="text-muted">{{ t('users.subtitle') }}</p>
            </div>
            <div>
              <button @click="showCreateModal" class="btn btn-primary">
                <i class="bi bi-person-plus"></i> {{ t('users.createNew') }}
              </button>
              <button @click="goBack" class="btn btn-secondary ms-2">
                <i class="bi bi-arrow-left"></i> {{ t('users.backToProfile') }}
              </button>
            </div>
          </div>

          <!-- Users Table -->
          <div class="card shadow-sm">
            <div class="card-body">
              <div v-if="loading" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">{{ t('common.loading') }}</span>
                </div>
                <p class="mt-3 text-muted">{{ t('users.loadingUsers') }}</p>
              </div>

              <div v-else-if="users.length === 0" class="text-center py-5">
                <i class="bi bi-people" style="font-size: 3rem; color: #ccc;"></i>
                <p class="mt-3 text-muted">{{ t('users.noUsersFound') }}</p>
              </div>

              <div v-else class="table-responsive">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>{{ t('users.table.id') }}</th>
                      <th>{{ t('users.table.username') }}</th>
                      <th>{{ t('users.table.name') }}</th>
                      <th>{{ t('users.table.email') }}</th>
                      <th>{{ t('users.table.phone') }}</th>
                      <th>{{ t('users.table.role') }}</th>
                      <th>{{ t('users.table.status') }}</th>
                      <th>{{ t('users.table.actions') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="user in users" :key="user.id">
                      <td>{{ user.id }}</td>
                      <td>
                        <strong>{{ user.username }}</strong>
                      </td>
                      <td>{{ user.firstName }} {{ user.lastName }}</td>
                      <td>{{ user.email || '-' }}</td>
                      <td>{{ user.phoneNumber || '-' }}</td>
                      <td>
                        <span :class="getRoleBadgeClass(user.userType)">
                          {{ formatRole(user.userType) }}
                        </span>
                      </td>
                      <td>
                        <span :class="user.enabled ? 'badge bg-success' : 'badge bg-danger'">
                          {{ user.enabled ? t('users.status.active') : t('users.status.disabled') }}
                        </span>
                      </td>
                      <td>
                        <button 
                          @click="viewUser(user)" 
                          class="btn btn-sm btn-info me-1"
                          :title="t('users.actions.viewDetails')"
                        >
                          <i class="bi bi-eye"></i>
                        </button>
                        <button 
                          @click="editUser(user)" 
                          class="btn btn-sm btn-warning me-1"
                          :title="t('users.actions.edit')"
                        >
                          <i class="bi bi-pencil"></i>
                        </button>
                        <button 
                          v-if="canDelete(user)"
                          @click="confirmDelete(user)" 
                          class="btn btn-sm btn-danger"
                          :title="t('users.actions.delete')"
                        >
                          <i class="bi bi-trash"></i>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit User Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="bi bi-person-plus"></i>
            {{ isEditMode ? t('users.edit.title') : t('users.create.title') }}
          </h5>
          <button @click="closeModal" class="btn-close"></button>
        </div>
        <form @submit.prevent="saveUser">
          <div class="modal-body">
            <div class="row">
              <div class="col-md-6 mb-3">
                <FormField
                  id="user-username"
                  v-model="userForm.username"
                  :label="t('users.form.username')"
                  type="text"
                  :placeholder="t('users.form.usernamePlaceholder')"
                  :disabled="isEditMode"
                  required
                />
              </div>
              <div class="col-md-6 mb-3" v-if="!isEditMode">
                <FormField
                  id="user-password"
                  v-model="userForm.password"
                  :label="t('users.form.password')"
                  type="password"
                  :placeholder="t('users.form.passwordPlaceholder')"
                  required
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="user-firstName"
                  v-model="userForm.firstName"
                  :label="t('users.form.firstName')"
                  type="text"
                  :placeholder="t('users.form.firstNamePlaceholder')"
                  required
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="user-lastName"
                  v-model="userForm.lastName"
                  :label="t('users.form.lastName')"
                  type="text"
                  :placeholder="t('users.form.lastNamePlaceholder')"
                  required
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="user-email"
                  v-model="userForm.email"
                  :label="t('users.form.email')"
                  type="email"
                  :placeholder="t('users.form.emailPlaceholder')"
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="user-phoneNumber"
                  v-model="userForm.phoneNumber"
                  :label="t('users.form.phone')"
                  type="text"
                  :placeholder="t('users.form.phonePlaceholder')"
                />
              </div>
              <div class="col-12 mb-3">
                <FormField
                  id="user-address"
                  v-model="userForm.address"
                  :label="t('users.form.address')"
                  type="text"
                  :placeholder="t('users.form.addressPlaceholder')"
                />
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label">{{ t('users.form.role') }}</label>
                <select v-model="userForm.userType" class="form-select" required>
                  <option value="USER">{{ t('users.roles.user') }}</option>
                  <option value="SUPER_USER" v-if="currentUserRole === 'ADMIN'">{{ t('users.roles.superUser') }}</option>
                  <option value="ADMIN" v-if="currentUserRole === 'ADMIN'">{{ t('users.roles.admin') }}</option>
                </select>
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label">{{ t('users.form.status') }}</label>
                <select v-model="userForm.enabled" class="form-select">
                  <option :value="true">{{ t('users.status.active') }}</option>
                  <option :value="false">{{ t('users.status.disabled') }}</option>
                </select>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" @click="closeModal" class="btn btn-secondary" :disabled="saving">
              {{ t('common.cancel') }}
            </button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              <span v-if="saving" class="spinner-border spinner-border-sm me-2"></span>
              <i v-else class="bi bi-check-lg"></i>
              {{ isEditMode ? t('users.edit.button') : t('users.create.button') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- View User Modal -->
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="bi bi-person-circle"></i>
            {{ t('users.details.title') }}
          </h5>
          <button @click="closeViewModal" class="btn-close"></button>
        </div>
        <div class="modal-body">
          <div v-if="selectedUser">
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.table.id') }}:</label>
                <p>{{ selectedUser.id }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.table.username') }}:</label>
                <p>{{ selectedUser.username }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.form.firstName') }}:</label>
                <p>{{ selectedUser.firstName }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.form.lastName') }}:</label>
                <p>{{ selectedUser.lastName }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.form.email') }}:</label>
                <p>{{ selectedUser.email || t('users.notProvided') }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.form.phone') }}:</label>
                <p>{{ selectedUser.phoneNumber || t('users.notProvided') }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">{{ t('users.form.address') }}:</label>
                <p>{{ selectedUser.address || t('users.notProvided') }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.table.role') }}:</label>
                <p>
                  <span :class="getRoleBadgeClass(selectedUser.userType)">
                    {{ formatRole(selectedUser.userType) }}
                  </span>
                </p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.table.status') }}:</label>
                <p>
                  <span :class="selectedUser.enabled ? 'badge bg-success' : 'badge bg-danger'">
                    {{ selectedUser.enabled ? t('users.status.active') : t('users.status.disabled') }}
                  </span>
                </p>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeViewModal" class="btn btn-secondary">{{ t('common.close') }}</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-content modal-sm">
        <div class="modal-header bg-danger text-white">
          <h5 class="modal-title">
            <i class="bi bi-exclamation-triangle"></i>
            {{ t('users.delete.title') }}
          </h5>
          <button @click="closeDeleteModal" class="btn-close btn-close-white"></button>
        </div>
        <div class="modal-body">
          <p>{{ t('users.delete.message') }} <strong>{{ userToDelete?.username }}</strong>?</p>
          <p class="text-danger">{{ t('users.delete.warning') }}</p>
        </div>
        <div class="modal-footer">
          <button @click="closeDeleteModal" class="btn btn-secondary" :disabled="deleting">
            {{ t('common.cancel') }}
          </button>
          <button @click="deleteUser" class="btn btn-danger" :disabled="deleting">
            <span v-if="deleting" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-trash"></i>
            {{ t('users.delete.button') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { userService } from '../services/userService'
import { useAuthStore } from '../stores/authStore'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const users = ref([])
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const showModal = ref(false)
const showViewModal = ref(false)
const showDeleteModal = ref(false)
const isEditMode = ref(false)
const selectedUser = ref(null)
const userToDelete = ref(null)
const currentUserRole = ref(authStore.user?.userType || 'USER')

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  address: '',
  userType: 'USER',
  enabled: true
})

const alert = reactive({
  show: false,
  type: 'success',
  message: ''
})

const showAlert = (type, message) => {
  alert.type = type
  alert.message = message
  alert.show = true
  setTimeout(() => {
    alert.show = false
  }, 5000)
}

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await userService.getAllUsers()
    users.value = data
  } catch (error) {
    showAlert('danger', error.message || 'Failed to load users')
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEditMode.value = false
  resetForm()
  showModal.value = true
}

const viewUser = (user) => {
  selectedUser.value = user
  showViewModal.value = true
}

const editUser = (user) => {
  isEditMode.value = true
  userForm.id = user.id
  userForm.username = user.username
  userForm.password = ''
  userForm.firstName = user.firstName
  userForm.lastName = user.lastName
  userForm.email = user.email || ''
  userForm.phoneNumber = user.phoneNumber || ''
  userForm.address = user.address || ''
  userForm.userType = user.userType
  userForm.enabled = user.enabled
  showModal.value = true
}

const saveUser = async () => {
  saving.value = true
  try {
    if (isEditMode.value) {
      // Update existing user
      await userService.updateUser(userForm.id, {
        firstName: userForm.firstName,
        lastName: userForm.lastName,
        email: userForm.email,
        phoneNumber: userForm.phoneNumber,
        address: userForm.address,
        userType: userForm.userType,
        enabled: userForm.enabled
      })
      showAlert('success', 'User updated successfully!')
    } else {
      // Create new user
      await userService.createUser({
        username: userForm.username,
        password: userForm.password,
        firstName: userForm.firstName,
        lastName: userForm.lastName,
        email: userForm.email,
        phoneNumber: userForm.phoneNumber,
        address: userForm.address,
        userType: userForm.userType
      })
      showAlert('success', 'User created successfully!')
    }
    closeModal()
    await loadUsers()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to save user')
  } finally {
    saving.value = false
  }
}

const confirmDelete = (user) => {
  userToDelete.value = user
  showDeleteModal.value = true
}

const deleteUser = async () => {
  if (!userToDelete.value) return
  
  deleting.value = true
  try {
    await userService.deleteUser(userToDelete.value.id)
    showAlert('success', 'User deleted successfully!')
    closeDeleteModal()
    await loadUsers()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to delete user')
  } finally {
    deleting.value = false
  }
}

const canDelete = (user) => {
  // Admin can delete users, but not themselves
  return currentUserRole.value === 'ADMIN' && user.id !== authStore.user?.id
}

const resetForm = () => {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.firstName = ''
  userForm.lastName = ''
  userForm.email = ''
  userForm.phoneNumber = ''
  userForm.address = ''
  userForm.userType = 'USER'
  userForm.enabled = true
}

const closeModal = () => {
  showModal.value = false
  resetForm()
}

const closeViewModal = () => {
  showViewModal.value = false
  selectedUser.value = null
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  userToDelete.value = null
}

const formatRole = (role) => {
  if (!role) return ''
  return role.replace('_', ' ')
}

const getRoleBadgeClass = (role) => {
  const baseClass = 'badge '
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

const goBack = () => {
  router.push('/profile')
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.users-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.card {
  border: none;
  border-radius: 10px;
}

.table {
  margin-bottom: 0;
}

.table th {
  background-color: #f8f9fa;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-content {
  background: white;
  border-radius: 10px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-content.modal-sm {
  max-width: 500px;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  opacity: 0.5;
}

.btn-close:hover {
  opacity: 1;
}

.btn-close-white {
  filter: invert(1);
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
}
</style>

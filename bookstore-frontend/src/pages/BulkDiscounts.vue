<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h2><i class="bi bi-percent"></i> Bulk Discount Management</h2>
        <p class="text-muted mb-0">Manage discount rules and quantity tiers.</p>
      </div>
      <div>
        <button class="btn btn-primary me-2" @click="startCreate">
          <i class="bi bi-plus-circle"></i> Create rule
        </button>
        <button class="btn btn-secondary" @click="router.push('/profile')">
          <i class="bi bi-arrow-left"></i> Back to profile
        </button>
      </div>
    </div>

    <Alert v-if="alert.show" :type="alert.type" :message="alert.message" @close="alert.show = false" />

    <div v-if="editing" class="card shadow-sm mb-4">
      <div class="card-header"><h5 class="mb-0">{{ form.id ? 'Edit rule' : 'Create rule' }}</h5></div>
      <form class="card-body" @submit.prevent="saveRule">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label" for="rule-name">Name</label>
            <input id="rule-name" v-model="form.name" class="form-control" required />
          </div>
          <div class="col-md-6">
            <label class="form-label" for="rule-category">Category (optional)</label>
            <input id="rule-category" v-model="form.appliesToCategory" class="form-control" />
          </div>
          <div class="col-md-12">
            <label class="form-label" for="rule-description">Description</label>
            <textarea id="rule-description" v-model="form.description" class="form-control" rows="2"></textarea>
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-min">Minimum quantity</label>
            <input id="rule-min" v-model.number="form.minQuantity" type="number" min="1" class="form-control" required />
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-max">Maximum quantity (optional)</label>
            <input id="rule-max" v-model.number="form.maxQuantity" type="number" min="1" class="form-control" />
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-percent">Discount percent</label>
            <input id="rule-percent" v-model.number="form.discountPercentage" type="number" min="0.01" max="100" step="0.01" class="form-control" required />
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-priority">Priority</label>
            <input id="rule-priority" v-model.number="form.priority" type="number" min="0" class="form-control" />
          </div>
          <div class="col-12 form-check ms-2">
            <input id="rule-active" v-model="form.active" type="checkbox" class="form-check-input" />
            <label class="form-check-label" for="rule-active">Active</label>
          </div>
        </div>
        <div class="mt-3">
          <button class="btn btn-primary me-2" type="submit" :disabled="saving">
            <span v-if="saving" class="spinner-border spinner-border-sm me-2"></span>
            {{ form.id ? 'Save changes' : 'Create rule' }}
          </button>
          <button class="btn btn-outline-secondary" type="button" @click="cancelEdit">Cancel</button>
        </div>
      </form>
    </div>

    <div class="card shadow-sm">
      <div class="card-header d-flex justify-content-between align-items-center">
        <h5 class="mb-0">Discount rules</h5>
        <div class="btn-group btn-group-sm" role="group" aria-label="Rule list filter">
          <button class="btn" :class="{ 'btn-primary': !showingActive, 'btn-outline-primary': showingActive }" @click="loadRules(false)">All rules</button>
          <button class="btn" :class="{ 'btn-primary': showingActive, 'btn-outline-primary': !showingActive }" @click="loadRules(true)">Active rules</button>
        </div>
      </div>
      <div v-if="loading" class="text-center py-5">
        <span class="spinner-border text-primary" role="status"></span>
      </div>
      <div v-else-if="rules.length === 0" class="text-center text-muted py-5">No discount rules found.</div>
      <div v-else class="table-responsive">
        <table class="table table-hover mb-0">
          <thead>
            <tr><th>ID</th><th>Name</th><th>Quantity range</th><th>Discount</th><th>Category</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.id">
              <td>{{ rule.id }}</td>
              <td><strong>{{ rule.name }}</strong><div class="small text-muted">{{ rule.description }}</div></td>
              <td>{{ rule.minQuantity }}–{{ rule.maxQuantity || '∞' }}</td>
              <td>{{ rule.discountPercentage }}%</td>
              <td>{{ rule.appliesToCategory || 'All categories' }}</td>
              <td><span class="badge" :class="rule.active ? 'bg-success' : 'bg-secondary'">{{ rule.active ? 'Active' : 'Inactive' }}</span></td>
              <td class="text-nowrap">
                <button class="btn btn-sm btn-outline-info me-1" title="View rule" @click="viewRule(rule.id)"><i class="bi bi-eye"></i></button>
                <button class="btn btn-sm btn-outline-warning me-1" title="Edit rule" @click="editRule(rule.id)"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" title="Delete rule" @click="deleteRule(rule.id)"><i class="bi bi-trash"></i></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Alert from '../components/Alert.vue'
import bulkDiscountService from '../services/bulkDiscountService'

const router = useRouter()
const rules = ref([])
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const showingActive = ref(false)
const alert = reactive({ show: false, type: 'success', message: '' })

const emptyForm = () => ({
  id: null,
  name: '',
  description: '',
  minQuantity: 1,
  maxQuantity: null,
  discountPercentage: 1,
  appliesToCategory: '',
  active: true,
  priority: 0
})
const form = reactive(emptyForm())

const notify = (type, message) => {
  alert.type = type
  alert.message = message
  alert.show = true
}

const loadRules = async (activeOnly = showingActive.value) => {
  showingActive.value = activeOnly
  loading.value = true
  try {
    rules.value = activeOnly
      ? await bulkDiscountService.getActiveRulesForAdmin()
      : await bulkDiscountService.getAllRules()
  } catch (error) {
    notify('danger', error.message || 'Failed to load discount rules.')
  } finally {
    loading.value = false
  }
}

const startCreate = () => {
  Object.assign(form, emptyForm())
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
}

const editRule = async (id) => {
  try {
    Object.assign(form, await bulkDiscountService.getRuleById(id))
    form.appliesToCategory ||= ''
    form.maxQuantity ??= null
    editing.value = true
  } catch (error) {
    notify('danger', error.message || 'Failed to load discount rule.')
  }
}

const viewRule = async (id) => {
  try {
    const rule = await bulkDiscountService.getRuleById(id)
    notify('info', `${rule.name}: ${rule.discountPercentage}% off for ${rule.minQuantity}+ items${rule.active ? ' (active)' : ' (inactive)'}.`)
  } catch (error) {
    notify('danger', error.message || 'Failed to load discount rule.')
  }
}

const saveRule = async () => {
  saving.value = true
  const payload = {
    ...form,
    maxQuantity: form.maxQuantity || null,
    appliesToCategory: form.appliesToCategory.trim() || null
  }
  try {
    if (form.id) await bulkDiscountService.updateRule(form.id, payload)
    else await bulkDiscountService.createRule(payload)
    notify('success', form.id ? 'Discount rule updated.' : 'Discount rule created.')
    editing.value = false
    await loadRules()
  } catch (error) {
    notify('danger', error.message || 'Failed to save discount rule.')
  } finally {
    saving.value = false
  }
}

const deleteRule = async (id) => {
  if (!window.confirm(`Delete discount rule #${id}?`)) return
  try {
    await bulkDiscountService.deleteRule(id)
    notify('success', 'Discount rule deleted.')
    await loadRules()
  } catch (error) {
    notify('danger', error.message || 'Failed to delete discount rule.')
  }
}

onMounted(() => loadRules(false))
</script>

<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h2><i class="bi bi-percent"></i> {{ t('bulkDiscountManagement.title') }}</h2>
        <p class="text-muted mb-0">{{ t('bulkDiscountManagement.subtitle') }}</p>
      </div>
      <div>
        <button class="btn btn-primary me-2" @click="startCreate">
          <i class="bi bi-plus-circle"></i> {{ t('bulkDiscountManagement.createRule') }}
        </button>
        <button class="btn btn-secondary" @click="router.push('/profile')">
          <i class="bi bi-arrow-left"></i> {{ t('bulkDiscountManagement.backToProfile') }}
        </button>
      </div>
    </div>

    <Alert v-if="alert.show" :type="alert.type" :message="alert.message" @close="alert.show = false" />

    <div v-if="editing" class="card shadow-sm mb-4">
      <div class="card-header"><h5 class="mb-0">{{ form.id ? t('bulkDiscountManagement.editRule') : t('bulkDiscountManagement.createRuleTitle') }}</h5></div>
      <form class="card-body" @submit.prevent="saveRule">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label" for="rule-name">{{ t('bulkDiscountManagement.name') }}</label>
            <input id="rule-name" v-model="form.name" class="form-control" required />
          </div>
          <div class="col-md-6">
            <label class="form-label" for="rule-category">{{ t('bulkDiscountManagement.categoryOptional') }}</label>
            <input id="rule-category" v-model="form.appliesToCategory" class="form-control" />
          </div>
          <div class="col-md-12">
            <label class="form-label" for="rule-description">{{ t('bulkDiscountManagement.description') }}</label>
            <textarea id="rule-description" v-model="form.description" class="form-control" rows="2"></textarea>
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-min">{{ t('bulkDiscountManagement.minimumQuantity') }}</label>
            <input id="rule-min" v-model.number="form.minQuantity" type="number" min="1" class="form-control" required />
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-max">{{ t('bulkDiscountManagement.maximumQuantityOptional') }}</label>
            <input id="rule-max" v-model.number="form.maxQuantity" type="number" min="1" class="form-control" />
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-percent">{{ t('bulkDiscountManagement.discountPercent') }}</label>
            <input id="rule-percent" v-model.number="form.discountPercentage" type="number" min="0.01" max="100" step="0.01" class="form-control" required />
          </div>
          <div class="col-md-3">
            <label class="form-label" for="rule-priority">{{ t('bulkDiscountManagement.priority') }}</label>
            <input id="rule-priority" v-model.number="form.priority" type="number" min="0" class="form-control" />
          </div>
          <div class="col-12 form-check ms-2">
            <input id="rule-active" v-model="form.active" type="checkbox" class="form-check-input" />
            <label class="form-check-label" for="rule-active">{{ t('bulkDiscountManagement.active') }}</label>
          </div>
        </div>
        <div class="mt-3">
          <button class="btn btn-primary me-2" type="submit" :disabled="saving">
            <span v-if="saving" class="spinner-border spinner-border-sm me-2"></span>
            {{ form.id ? t('bulkDiscountManagement.saveChanges') : t('bulkDiscountManagement.createRule') }}
          </button>
          <button class="btn btn-outline-secondary" type="button" @click="cancelEdit">{{ t('bulkDiscountManagement.cancel') }}</button>
        </div>
      </form>
    </div>

    <div class="card shadow-sm">
      <div class="card-header d-flex justify-content-between align-items-center">
        <h5 class="mb-0">{{ t('bulkDiscountManagement.discountRules') }}</h5>
        <div class="btn-group btn-group-sm" role="group" :aria-label="t('bulkDiscountManagement.ruleListFilter')">
          <button class="btn" :class="{ 'btn-primary': !showingActive, 'btn-outline-primary': showingActive }" @click="loadRules(false)">{{ t('bulkDiscountManagement.allRules') }}</button>
          <button class="btn" :class="{ 'btn-primary': showingActive, 'btn-outline-primary': !showingActive }" @click="loadRules(true)">{{ t('bulkDiscountManagement.activeRules') }}</button>
        </div>
      </div>
      <div v-if="loading" class="text-center py-5">
        <span class="spinner-border text-primary" role="status"></span>
      </div>
      <div v-else-if="rules.length === 0" class="text-center text-muted py-5">{{ t('bulkDiscountManagement.noRulesFound') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover mb-0">
          <thead>
            <tr><th>{{ t('bulkDiscountManagement.id') }}</th><th>{{ t('bulkDiscountManagement.name') }}</th><th>{{ t('bulkDiscountManagement.quantityRange') }}</th><th>{{ t('bulkDiscountManagement.discount') }}</th><th>{{ t('bulkDiscountManagement.categoryOptional') }}</th><th>{{ t('bulkDiscountManagement.status') }}</th><th>{{ t('bulkDiscountManagement.actions') }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.id">
              <td>{{ rule.id }}</td>
              <td><strong>{{ rule.name }}</strong><div class="small text-muted">{{ rule.description }}</div></td>
              <td>{{ rule.minQuantity }}–{{ rule.maxQuantity || '∞' }}</td>
              <td>{{ rule.discountPercentage }}%</td>
              <td>{{ rule.appliesToCategory || t('bulkDiscountManagement.allCategories') }}</td>
              <td><span class="badge" :class="rule.active ? 'bg-success' : 'bg-secondary'">{{ rule.active ? t('bulkDiscountManagement.active') : t('bulkDiscountManagement.inactive') }}</span></td>
              <td class="text-nowrap">
                <button class="btn btn-sm btn-outline-info me-1" :title="t('bulkDiscountManagement.viewRule')" @click="viewRule(rule.id)"><i class="bi bi-eye"></i></button>
                <button class="btn btn-sm btn-outline-warning me-1" :title="t('bulkDiscountManagement.edit')" @click="editRule(rule.id)"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" :title="t('bulkDiscountManagement.delete')" @click="deleteRule(rule.id)"><i class="bi bi-trash"></i></button>
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
import { useI18n } from 'vue-i18n'
import Alert from '../components/Alert.vue'
import bulkDiscountService from '../services/bulkDiscountService'

const router = useRouter()
const { t } = useI18n()
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
    notify('danger', error.message || t('bulkDiscountManagement.loadedFailed'))
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
    notify('danger', error.message || t('bulkDiscountManagement.ruleLoadFailed'))
  }
}

const viewRule = async (id) => {
  try {
    const rule = await bulkDiscountService.getRuleById(id)
    notify('info', t('bulkDiscountManagement.detail', {
      name: rule.name,
      percentage: rule.discountPercentage,
      quantity: rule.minQuantity,
      status: rule.active ? t('bulkDiscountManagement.activeDetail') : t('bulkDiscountManagement.inactiveDetail')
    }))
  } catch (error) {
    notify('danger', error.message || t('bulkDiscountManagement.ruleLoadFailed'))
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
    notify('success', form.id ? t('bulkDiscountManagement.updated') : t('bulkDiscountManagement.created'))
    editing.value = false
    await loadRules()
  } catch (error) {
    notify('danger', error.message || t('bulkDiscountManagement.saveFailed'))
  } finally {
    saving.value = false
  }
}

const deleteRule = async (id) => {
  if (!window.confirm(t('bulkDiscountManagement.deleteConfirm', { id }))) return
  try {
    await bulkDiscountService.deleteRule(id)
    notify('success', t('bulkDiscountManagement.deleted'))
    await loadRules()
  } catch (error) {
    notify('danger', error.message || t('bulkDiscountManagement.deleteFailed'))
  }
}

onMounted(() => loadRules(false))
</script>

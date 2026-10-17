<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3">
      <div>
        <h2><i class="bi bi-graph-up-arrow me-2"></i>{{ t('reports.title') }}</h2>
        <p class="text-muted mb-0">{{ t('reports.subtitle') }}</p>
      </div>
      <button class="btn btn-secondary" @click="router.push('/profile')">
        <i class="bi bi-arrow-left me-1"></i>{{ t('reports.backToProfile') }}
      </button>
    </div>

    <Alert v-if="errorMessage" type="danger" :message="errorMessage" @close="errorMessage = ''" />

    <form class="card card-body shadow-sm mb-4" @submit.prevent="loadReports">
      <div class="row g-3 align-items-end">
        <div class="col-sm-5 col-md-4">
          <label class="form-label" for="report-start-date">{{ t('reports.startDate') }}</label>
          <input id="report-start-date" v-model="startDate" type="date" class="form-control" @keydown="preventManualDateEntry" @paste.prevent @drop.prevent />
          <div class="form-text">{{ t('reports.datePickerHint') }}</div>
        </div>
        <div class="col-sm-5 col-md-4">
          <label class="form-label" for="report-end-date">{{ t('reports.endDate') }}</label>
          <input id="report-end-date" v-model="endDate" type="date" class="form-control" :min="startDate || undefined" :disabled="!startDate" @keydown="preventManualDateEntry" @paste.prevent @drop.prevent />
          <div class="form-text">{{ t('reports.datePickerHint') }}</div>
        </div>
        <div class="col-sm-2 col-md-4">
          <div class="form-label invisible" aria-hidden="true">{{ t('reports.startDate') }}</div>
          <div class="d-flex gap-2">
            <button class="btn btn-primary" type="submit" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
              {{ t('reports.applyFilter') }}
            </button>
            <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="resetFilters">
              {{ t('reports.reset') }}
            </button>
          </div>
          <div class="form-text invisible" aria-hidden="true">{{ t('reports.datePickerHint') }}</div>
        </div>
      </div>
    </form>

    <div v-if="loading && !salesReport" class="text-center py-5">
      <span class="spinner-border text-primary" role="status"></span>
    </div>

    <template v-else-if="salesReport && inventoryReport">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">{{ t('reports.salesOverview') }}</h4>
        <button class="btn btn-outline-success btn-sm" type="button" :aria-label="t('reports.downloadSales')" :title="t('reports.downloadSales')" @click="downloadSalesReport">
          <i class="bi bi-download"></i>
        </button>
      </div>
      <div class="row g-3 mb-4">
        <div class="col-md-4">
          <div class="card shadow-sm h-100"><div class="card-body">
            <div class="text-muted">{{ t('reports.paidOrders') }}</div>
            <div class="display-6">{{ salesReport.orderCount }}</div>
          </div></div>
        </div>
        <div class="col-md-4">
          <div class="card shadow-sm h-100"><div class="card-body">
            <div class="text-muted">{{ t('reports.unitsSold') }}</div>
            <div class="display-6">{{ salesReport.unitsSold }}</div>
          </div></div>
        </div>
        <div class="col-md-4">
          <div class="card shadow-sm h-100"><div class="card-body">
            <div class="text-muted">{{ t('reports.revenue') }}</div>
            <div class="display-6">{{ formatCurrency(salesReport.revenue) }}</div>
          </div></div>
        </div>
      </div>

      <div class="card shadow-sm mb-4">
        <div class="card-header"><h5 class="mb-0">{{ t('reports.topSellingBooks') }}</h5></div>
        <div v-if="salesReport.topSellingBooks.length === 0" class="card-body text-muted">{{ t('reports.noSales') }}</div>
        <div v-else class="table-responsive">
          <table class="table table-hover mb-0">
            <thead><tr><th>{{ t('reports.book') }}</th><th>{{ t('reports.author') }}</th><th>{{ t('reports.quantitySold') }}</th><th>{{ t('reports.salesAmount') }}</th></tr></thead>
            <tbody>
              <tr v-for="book in salesReport.topSellingBooks" :key="book.bookId">
                <td>{{ book.title }}</td><td>{{ book.author }}</td><td>{{ book.quantitySold }}</td><td>{{ formatCurrency(book.salesAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">{{ t('reports.inventoryOverview') }}</h4>
        <button class="btn btn-outline-success btn-sm" type="button" :aria-label="t('reports.downloadInventory')" :title="t('reports.downloadInventory')" @click="downloadInventoryReport">
          <i class="bi bi-download"></i>
        </button>
      </div>
      <div class="row g-3 mb-4">
        <div class="col-md-3"><div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted">{{ t('reports.titlesInCatalog') }}</div><div class="display-6">{{ inventoryReport.titleCount }}</div></div></div></div>
        <div class="col-md-3"><div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted">{{ t('reports.unitsInStock') }}</div><div class="display-6">{{ inventoryReport.unitsInStock }}</div></div></div></div>
        <div class="col-md-3"><div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted">{{ t('reports.outOfStock') }}</div><div class="display-6 text-danger">{{ inventoryReport.outOfStockCount }}</div></div></div></div>
        <div class="col-md-3"><div class="card shadow-sm h-100"><div class="card-body"><div class="text-muted">{{ t('reports.inventoryValue') }}</div><div class="display-6">{{ formatCurrency(inventoryReport.inventoryValue) }}</div></div></div></div>
      </div>

      <div class="card shadow-sm">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">{{ t('reports.lowStockBooks') }}</h5>
          <span class="badge bg-warning text-dark">{{ t('reports.threshold', { count: inventoryReport.lowStockThreshold }) }}</span>
        </div>
        <div v-if="inventoryReport.lowStockBooks.length === 0" class="card-body text-muted">{{ t('reports.noLowStock') }}</div>
        <div v-else class="table-responsive">
          <table class="table table-hover mb-0">
            <thead><tr><th>{{ t('reports.book') }}</th><th>{{ t('reports.author') }}</th><th>{{ t('reports.isbn') }}</th><th>{{ t('reports.stock') }}</th><th>{{ t('reports.unitPrice') }}</th></tr></thead>
            <tbody>
              <tr v-for="book in inventoryReport.lowStockBooks" :key="book.id">
                <td>{{ book.title }}</td><td>{{ book.author }}</td><td>{{ book.isbn }}</td>
                <td><span class="badge" :class="book.stockQuantity === 0 ? 'bg-danger' : 'bg-warning text-dark'">{{ book.stockQuantity }}</span></td>
                <td>{{ formatCurrency(book.price) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onActivated, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Alert from '../components/Alert.vue'
import { reportService } from '../services/reportService'

const router = useRouter()
const { t } = useI18n()
const salesReport = ref(null)
const inventoryReport = ref(null)
const loading = ref(false)
const errorMessage = ref('')

const toDateInput = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const startDate = ref('')
const endDate = ref('')

const preventManualDateEntry = (event) => {
  if (event.key !== 'Tab') {
    event.preventDefault()
  }
}

const formatCurrency = (amount) => new Intl.NumberFormat(undefined, {
  style: 'currency',
  currency: 'USD'
}).format(Number(amount || 0))

const saveWorkbook = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const downloadSalesReport = async () => {
  if (!validateDateRange()) return

  try {
    const workbook = await reportService.exportSalesReport(startDate.value, endDate.value)
    const reportStart = salesReport.value?.startDate || startDate.value
    const reportEnd = salesReport.value?.endDate || endDate.value
    saveWorkbook(workbook, `sales_report_${reportStart}_${reportEnd}.xlsx`)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || t('reports.exportFailed')
  }
}

const downloadInventoryReport = async () => {
  try {
    const workbook = await reportService.exportInventoryReport()
    saveWorkbook(workbook, `inventory_report_${toDateInput(new Date())}.xlsx`)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || t('reports.exportFailed')
  }
}

const loadReports = async () => {
  if (!validateDateRange()) return

  loading.value = true
  errorMessage.value = ''
  try {
    const [sales, inventory] = await Promise.all([
      reportService.getSalesReport(startDate.value, endDate.value),
      reportService.getInventoryReport()
    ])
    salesReport.value = sales
    inventoryReport.value = inventory
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || t('reports.loadFailed')
  } finally {
    loading.value = false
  }
}

const validateDateRange = () => {
  if (!startDate.value) {
    errorMessage.value = t('reports.startDateRequired')
    return false
  }
  if (!endDate.value) {
    errorMessage.value = t('reports.endDateRequired')
    return false
  }
  if (endDate.value < startDate.value) {
    errorMessage.value = t('reports.invalidDateRange')
    return false
  }
  errorMessage.value = ''
  return true
}

const resetFilters = () => {
  startDate.value = ''
  endDate.value = ''
  salesReport.value = null
  inventoryReport.value = null
  errorMessage.value = ''
}

onMounted(resetFilters)
onActivated(resetFilters)
onBeforeRouteLeave(resetFilters)
</script>

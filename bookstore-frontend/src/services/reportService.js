import apiClient from './api'

export const reportService = {
  getSalesReport: async (startDate, endDate) => {
    const response = await apiClient.get('/admin/reports/sales', {
      params: { startDate, endDate }
    })
    return response.data
  },

  getInventoryReport: async () => {
    const response = await apiClient.get('/admin/reports/inventory')
    return response.data
  },

  exportSalesReport: async (startDate, endDate) => {
    const response = await apiClient.get('/admin/reports/sales/export', {
      params: { startDate, endDate },
      responseType: 'blob'
    })
    return response.data
  },

  exportInventoryReport: async () => {
    const response = await apiClient.get('/admin/reports/inventory/export', {
      responseType: 'blob'
    })
    return response.data
  }
}

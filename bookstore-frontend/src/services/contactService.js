import apiClient from './api'

export const sendContactMessage = (message) => apiClient.post('/contact', message)

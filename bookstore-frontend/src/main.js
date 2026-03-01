import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { handleVueError, handleVueWarning, sanitizeErrorMessage } from './utils/errorHandler'

const app = createApp(App)

// Global error handler - prevents stack traces from being shown to users
app.config.errorHandler = handleVueError

// Global warning handler - suppress warnings in production
app.config.warnHandler = handleVueWarning

// Suppress dev tools warning in production
app.config.productionTip = false

// Global window error handler for uncaught JavaScript errors
window.addEventListener('error', (event) => {
  // Prevent default browser error reporting
  event.preventDefault()
  
  const safeMessage = sanitizeErrorMessage(event.error, 'An unexpected error occurred')
  
  if (import.meta.env.DEV) {
    console.error('[Uncaught Error]:', event.error)
  } else {
    console.error('[Application Error]:', safeMessage)
  }
})

// Global unhandled promise rejection handler
window.addEventListener('unhandledrejection', (event) => {
  // Prevent default browser error reporting
  event.preventDefault()
  
  const safeMessage = sanitizeErrorMessage(event.reason, 'An unexpected error occurred')
  
  if (import.meta.env.DEV) {
    console.error('[Unhandled Promise Rejection]:', event.reason)
  } else {
    console.error('[Application Error]:', safeMessage)
  }
})

app.use(createPinia())
app.use(router)
app.use(i18n)
app.mount('#app')

/**
 * Sanitizes error messages to prevent exposing sensitive information or stack traces
 * @param {Error|string} error - The error to sanitize
 * @param {string} fallbackMessage - Default message if error is not user-friendly
 * @returns {string} - Safe error message for display
 */
export function sanitizeErrorMessage(error, fallbackMessage = 'An unexpected error occurred') {
  // If it's already a string, use it (assuming it's already safe from backend)
  if (typeof error === 'string') {
    return error
  }

  // If error has a message property that looks safe (no stack trace patterns)
  if (error?.message && !containsStackTrace(error.message)) {
    return error.message
  }

  // In production, always return generic message
  if (import.meta.env.PROD) {
    return fallbackMessage
  }

  // In development, show more details
  return error?.message || fallbackMessage
}

/**
 * Check if a string contains stack trace patterns
 * @param {string} str - String to check
 * @returns {boolean}
 */
function containsStackTrace(str) {
  const stackTracePatterns = [
    /at\s+.*\s+\(.*:\d+:\d+\)/,  // at functionName (file:line:column)
    /^\s+at\s+/m,                 // at ...
    /Error:\s+.*\n\s+at\s+/,      // Error: message\n at ...
    /\.js:\d+:\d+/,               // file.js:line:column
    /\n\s+at\s+/,                 // newline followed by at
  ]
  
  return stackTracePatterns.some(pattern => pattern.test(str))
}

/**
 * Global error handler for Vue applications
 * @param {Error} err - The error object
 * @param {Object} instance - Vue component instance
 * @param {string} info - Vue-specific error info
 */
export function handleVueError(err, instance, info) {
  // Log full error details to console in development only
  if (import.meta.env.DEV) {
    console.error('[Vue Error Handler]:', err)
    console.error('Component:', instance)
    console.error('Error Info:', info)
  } else {
    // In production, log sanitized version
    console.error('[Application Error]:', sanitizeErrorMessage(err))
  }

  // You could send this to an error tracking service here
  // trackError(err, { component: instance?.$options?.name, info })
}

/**
 * Global warning handler for Vue applications
 * @param {string} msg - Warning message
 * @param {Object} instance - Vue component instance
 * @param {string} trace - Component trace
 */
export function handleVueWarning(msg, instance, trace) {
  // Only show warnings in development
  if (import.meta.env.DEV) {
    console.warn('[Vue Warning]:', msg)
    console.warn('Component:', instance)
    console.warn('Trace:', trace)
  }
}

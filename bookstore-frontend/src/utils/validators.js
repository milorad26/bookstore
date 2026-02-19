// Simple validation - backend handles all validation logic
// Frontend only checks for required fields to provide immediate feedback

export const validators = {
  username: {
    required: (value) => value ? null : 'Username is required'
  },

  password: {
    // Simple validation for login - only check if provided
    required: (value) => value ? null : 'Password is required'
  },

  registerPassword: {
    // Strong validation for registration and password change
    required: (value) => {
      if (!value) return 'Password is required'
      if (value.length < 8) return 'Password must be at least 8 characters'
      if (!/[A-Z]/.test(value)) return 'Password must contain at least one uppercase letter'
      if (!/[a-z]/.test(value)) return 'Password must contain at least one lowercase letter'
      if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value)) return 'Password must contain at least one special character'
      return null
    }
  },

  email: {
    required: (value) => value ? null : 'Email is required'
  },

  firstName: {
    required: (value) => value ? null : 'First name is required'
  },

  lastName: {
    required: (value) => value ? null : 'Last name is required'
  },

  phoneNumber: {
    optional: () => null
  },

  address: {
    optional: () => null
  }
}

export function validateField(fieldName, value) {
  const fieldRules = validators[fieldName]
  if (!fieldRules) return null

  for (const [ruleName, ruleFn] of Object.entries(fieldRules)) {
    const error = ruleFn(value)
    if (error) return error
  }

  return null
}

export function validateForm(formData, fields) {
  const errors = {}
  
  for (const field of fields) {
    const error = validateField(field, formData[field])
    if (error) {
      errors[field] = error
    }
  }

  return errors
}

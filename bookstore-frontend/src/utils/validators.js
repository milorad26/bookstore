// Simple validation - backend handles all validation logic
// Frontend only checks for required fields to provide immediate feedback

export const validators = {
  username: {
    required: (value) => value ? null : 'Username is required'
  },

  password: {
    required: (value) => value ? null : 'Password is required'
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

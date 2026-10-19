const statusKeys = {
  401: 'errors.unauthorized',
  403: 'errors.forbidden',
  404: 'errors.notFound',
  500: 'errors.server'
}

/** Returns a translated, user-safe error for API failures. */
export function localizedError(error, t, fallbackKey = 'errors.requestFailed') {
  if (error?.response?.data?.code === 'PAYMENT_NOT_CONFIGURED') {
    return t('errors.paymentNotConfigured')
  }
  const key = statusKeys[error?.response?.status]
    || (error?.response?.status >= 500 ? 'errors.server' : fallbackKey)
  return t(key)
}

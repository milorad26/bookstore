import { createI18n } from 'vue-i18n'
import en from './locales/en'
import sr from './locales/sr'

const i18n = createI18n({
  legacy: false, // Use Composition API mode
  locale: localStorage.getItem('locale') || 'en', // Get saved language or default to English
  fallbackLocale: 'en',
  messages: {
    en,
    sr
  }
})

export default i18n

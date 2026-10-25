<template>
  <div>
    <div ref="container" class="turnstile-container"></div>
    <p v-if="error" class="text-danger small mt-2 mb-0" role="alert">{{ error }}</p>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  siteKey: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['verified', 'expired'])
const { t } = useI18n()
const container = ref(null)
const error = ref('')
let widgetId
let loadPromise

const loadTurnstile = () => {
  if (window.turnstile) return Promise.resolve(window.turnstile)
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector('script[data-turnstile]')
    const script = existingScript || document.createElement('script')
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
    script.async = true
    script.defer = true
    script.dataset.turnstile = 'true'
    script.addEventListener('load', () => {
      if (window.turnstile) resolve(window.turnstile)
      else reject(new Error('Turnstile did not initialize'))
    }, { once: true })
    script.addEventListener('error', () => {
      loadPromise = null
      if (!existingScript) script.remove()
      reject(new Error('Turnstile script could not be loaded'))
    }, { once: true })
    if (!existingScript) document.head.appendChild(script)
  })

  return loadPromise
}

const reset = () => {
  emit('expired')
  if (widgetId !== undefined && window.turnstile) {
    window.turnstile.reset(widgetId)
  }
}

const render = async () => {
  try {
    const turnstile = await loadTurnstile()
    if (!container.value) return

    widgetId = turnstile.render(container.value, {
      sitekey: props.siteKey,
      callback: (token) => {
        error.value = ''
        emit('verified', token)
      },
      'expired-callback': reset,
      'error-callback': () => {
        error.value = t('captcha.retry')
        reset()
        return true
      }
    })
  } catch {
    error.value = t('captcha.loadError')
  }
}

defineExpose({ reset })

onMounted(render)
onUnmounted(() => {
  if (widgetId !== undefined && window.turnstile) {
    window.turnstile.remove(widgetId)
  }
})
</script>

<style scoped>
.turnstile-container {
  min-height: 65px;
}
</style>

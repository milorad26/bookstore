import path from 'node:path'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    VueI18nPlugin({
      include: [path.resolve('src/i18n/locales/*.js')],
      runtimeOnly: true
    }),
    {
      name: 'production-content-security-policy',
      transformIndexHtml() {
        if (command !== 'build') return
        return [{
          tag: 'meta',
          attrs: {
            'http-equiv': 'Content-Security-Policy',
            content: "default-src 'self'; script-src 'self' https://challenges.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' data: https://cdn.jsdelivr.net; img-src 'self' data: blob: https:; connect-src 'self' http://localhost:8080 https: wss:; frame-src 'self' https://js.stripe.com https://challenges.cloudflare.com"
          },
          injectTo: 'head'
        }]
      }
    }
  ],
  define: {
    __INTLIFY_JIT_COMPILATION__: true
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
}))

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    {
      name: 'production-content-security-policy',
      transformIndexHtml() {
        if (command !== 'build') return
        return [{
          tag: 'meta',
          attrs: {
            'http-equiv': 'Content-Security-Policy',
            content: "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' data: https://cdn.jsdelivr.net; img-src 'self' data: blob: https:; connect-src 'self' http://localhost:8080 https: wss:; frame-src 'self' https://js.stripe.com"
          },
          injectTo: 'head'
        }]
      }
    }
  ],
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

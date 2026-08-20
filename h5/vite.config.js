import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: '/h5/',
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/static': 'http://127.0.0.1:8080'
    }
  },
  build: { sourcemap: false }
})

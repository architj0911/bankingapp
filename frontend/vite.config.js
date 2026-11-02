import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    host: true,
    proxy: {
      '/api': process.env.VITE_DEV_API_PROXY || 'http://localhost:8080',
      '/auth': process.env.VITE_DEV_API_PROXY || 'http://localhost:8080',
      '/oauth2': process.env.VITE_DEV_API_PROXY || 'http://localhost:8080',
    }
  }
})

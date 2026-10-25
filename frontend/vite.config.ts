import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // The backend (make backend-run) serves the API; proxying keeps the browser on one origin.
    // BACKEND_URL points the proxy elsewhere, e.g. a second backend on another port.
    proxy: {
      '/api': process.env.BACKEND_URL ?? 'http://localhost:8080',
    },
  },
})

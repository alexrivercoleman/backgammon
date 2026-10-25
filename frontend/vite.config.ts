import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // The backend (make backend-run) serves the API; proxying keeps the browser on one origin.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})

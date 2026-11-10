import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Dev: Vite chạy cổng 5173 và chuyển /api sang Spring Boot (8080).
// Build: xuất vào src/main/resources/static để Spring Boot phục vụ luôn giao diện (một ứng dụng duy nhất).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})

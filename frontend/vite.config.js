import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    // Vitest конфигурация
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.js'],
    reporters: ['default', 'allure-vitest/reporter'],
    outputFile: {
      'allure-vitest/reporter': './allure-results/results.xml',
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/test-setup.js', 'src/main.jsx'],
    },
  },
})

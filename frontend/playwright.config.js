import { defineConfig, devices } from '@playwright/test'

/**
 * Конфигурация Playwright для E2E тестов.
 *
 * Запуск:
 *   npm run test:e2e                  — все тесты, headless
 *   npm run test:e2e:headed           — тесты с видимым браузером
 *   npx playwright test --ui          — интерактивный UI-режим
 *   npx playwright codegen localhost  — запись тестов кликами!
 */
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,            // для демонстрации лучше последовательно
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',       // Trace записывается при первой попытке повтора
    screenshot: 'only-on-failure', // Скриншот только при падении
    video: 'retain-on-failure',    // Видео только при падении
    actionTimeout: 10_000,
  },
  // Генерация отчётов
  reporter: [
    ['list'],                       // вывод в консоль
    ['html', { open: 'never' }],    // HTML отчёт
    ['allure-playwright'],          // Allure отчёт с pyramid-разметкой
  ],
  // Браузеры для кросс-браузерного тестирования
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    // Mobile
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 13'] },
    },
  ],
  // Запускаем dev-сервер перед тестами
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})

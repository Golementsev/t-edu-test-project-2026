import { test, expect } from '@playwright/test'
import { CatalogPage } from './pages/CatalogPage'

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ ФРОНТЕНД: УРОВЕНЬ E2E — Playwright ◀◀
 *     Тесты каталога книг
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что такое E2E-тест?
 * ─────────────────────
 * End-to-End тест управляет РЕАЛЬНЫМ браузером.
 * Имитирует полный путь пользователя:
 *   открыть браузер → кликнуть → ввести текст → проверить
 *
 * Playwright vs Selenium:
 * ────────────────────────
 * Playwright (2020):
 *   ✅ Современный API (async/await)
 *   ✅ Автоожидание элементов (auto-waiting)
 *   ✅ Встроенные локаторы и assertions
 *   ✅ Трассировка, скриншоты, видео из коробки
 *   ✅ Кросс-браузерность (Chrome, Firefox, Safari, мобильные)
 *   ✅ Playwright Codegen — запись тестов кликами!
 *
 * ВАЖНО: эти тесты требуют запущенного приложения на localhost:3000
 * (с подключённым бэкендом или с MSW-моками для фронта)
 */

test.describe('Каталог книг', () => {

  // ── Allure-аннотации для E2E тестов ──────────────────────────────────
  // В Playwright аннотации добавляются через test.info().annotations
  test.use({
    // Allure-тег для уровня пирамиды
    extraHTTPHeaders: {},
  })

  let catalog

  test.beforeEach(async ({ page }) => {
    catalog = new CatalogPage(page)
    await catalog.navigate()
  })

  test('главная страница загружается корректно', async ({ page }) => {
    // Проверяем заголовок страницы
    await expect(page).toHaveTitle(/BookStore/)

    // Проверяем ключевые элементы
    await expect(page.getByRole('heading', { name: 'Книжный магазин' })).toBeVisible()
    await expect(catalog.searchInput).toBeVisible()
    await expect(catalog.cartBtn).toBeVisible()
  })

  test('каталог отображает список книг', async ({ page }) => {
    // Ждём загрузку (спиннер исчезнет, появятся карточки)
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })
    const cards = catalog.bookCards()
    await expect(cards).not.toHaveCount(0)
  })

  test('поиск фильтрует книги по названию', async () => {
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })
    const initialCount = await catalog.bookCards().count()

    await catalog.search('Чистый')

    // Количество карточек уменьшилось
    await expect(catalog.bookCards()).not.toHaveCount(initialCount)
    // Найденная карточка содержит нужный текст
    await expect(catalog.bookCards().first().getByTestId('book-title'))
        .toContainText('Чистый код')
  })

  test('очистка поиска возвращает весь список', async () => {
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })
    const initialCount = await catalog.bookCards().count()

    await catalog.search('Чистый')
    await catalog.clearSearch()

    await expect(catalog.bookCards()).toHaveCount(initialCount)
  })

  test('поиск без результатов показывает пустое состояние', async () => {
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })
    await catalog.search('Абсолютно несуществующая книга XYZ')
    await expect(catalog.emptyState).toBeVisible()
  })

  test('снимок страницы каталога (visual regression)', async ({ page }) => {
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })

    // Screenshot testing — сравниваем с эталоном
    // При первом запуске создаётся baseline, при последующих — сравнивается
    await expect(page).toHaveScreenshot('catalog-page.png', {
      maxDiffPixelRatio: 0.02, // допускаем 2% различий
    })
  })
})

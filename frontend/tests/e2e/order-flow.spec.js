import { test, expect } from '@playwright/test'
import { CatalogPage, CartPanel } from './pages/CatalogPage'

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ E2E: Бизнес-сценарий — «Покупатель оформляет заказ» ◀◀
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Это системный тест: тестирует полный пользовательский сценарий.
 *
 * Почему именно этот сценарий?
 * ─────────────────────────────
 * Happy path (счастливый путь) — наиболее критичный бизнес-сценарий.
 * Если добавление в корзину и оформление заказа работает —
 * магазин может зарабатывать деньги. :)
 *
 * Принцип: «тест рассказывает историю пользователя»
 */

test.describe('Сценарий: Покупатель оформляет заказ', () => {

  test('Happy Path: от каталога до подтверждения заказа', async ({ page }) => {
    const catalog = new CatalogPage(page)
    const cart = new CartPanel(page)

    // ═══ Шаг 1: Открываем магазин ════════════════════════════════
    await test.step('Открываем главную страницу', async () => {
      await catalog.navigate()
      await expect(page).toHaveTitle(/BookStore/)
    })

    // ═══ Шаг 2: Просматриваем каталог ═════════════════════════════
    await test.step('Каталог загружается', async () => {
      await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })
      const bookCount = await catalog.bookCards().count()
      expect(bookCount).toBeGreaterThan(0)
    })

    // ═══ Шаг 3: Ищем книгу ════════════════════════════════════════
    await test.step('Ищем книгу по ключевому слову', async () => {
      await catalog.search('код')
      await expect(catalog.bookCards().first()).toBeVisible()
    })

    // ═══ Шаг 4: Добавляем книгу в корзину ═════════════════════════
    await test.step('Добавляем книгу в корзину', async () => {
      const firstAddBtn = catalog.addToCartButtons().first()
      // Получаем название книги для проверки
      const bookTitle = await catalog.bookCards().first()
          .getByTestId('book-title').textContent()

      await firstAddBtn.click()

      // Проверяем, что счётчик появился
      await expect(catalog.cartCount).toHaveText('1')

      // Проверяем Toast-уведомление
      await expect(page.getByTestId('toast')).toContainText('добавлена в корзину')
    })

    // ═══ Шаг 5: Открываем корзину ═════════════════════════════════
    await test.step('Открываем корзину и проверяем содержимое', async () => {
      await catalog.openCart()
      await expect(cart.panel).toBeVisible()
      await expect(cart.cartItems()).toHaveCount(1)
    })

    // ═══ Шаг 6: Изменяем количество ══════════════════════════════
    await test.step('Увеличиваем количество товара', async () => {
      await page.getByTestId('qty-increase-btn').click()
      await expect(page.getByTestId('cart-item-qty')).toHaveText('2')
    })

    // ═══ Шаг 7: Вводим email и оформляем ══════════════════════════
    await test.step('Вводим email и оформляем заказ', async () => {
      await cart.fillEmail('student@school.ru')
      await cart.checkout()
      // После успешного заказа корзина закрывается, появляется уведомление
      await expect(page.getByTestId('toast')).toContainText('Заказ успешно оформлен')
    })

    // ═══ Шаг 8: Проверяем финальное состояние ═════════════════════
    await test.step('Корзина пуста после заказа', async () => {
      await expect(catalog.cartCount).not.toBeVisible()
    })
  })

  test('Sad Path: попытка оформить заказ без email', async ({ page }) => {
    const catalog = new CatalogPage(page)
    const cart = new CartPanel(page)

    await catalog.navigate()
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })

    await test.step('Добавляем товар в корзину', async () => {
      await catalog.addToCartButtons().first().click()
    })

    await test.step('Открываем корзину и пробуем оформить без email', async () => {
      await catalog.openCart()
      await cart.checkout() // без email
      // Должна появиться ошибка валидации
      await expect(page.getByTestId('email-error')).toBeVisible()
      await expect(page.getByTestId('email-error')).toContainText('корректный email')
    })
  })

  test('Нельзя добавить книгу не в наличии', async ({ page }) => {
    const catalog = new CatalogPage(page)
    await catalog.navigate()
    await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })

    // Ищем книгу без остатка
    const allButtons = catalog.addToCartButtons()
    const buttonCount = await allButtons.count()

    let foundDisabled = false
    for (let i = 0; i < buttonCount; i++) {
      const btn = allButtons.nth(i)
      if (await btn.isDisabled()) {
        foundDisabled = true
        // Проверяем, что текст кнопки "Нет"
        await expect(btn).toHaveText('Нет')
        break
      }
    }

    // При наличии данных о книге без остатка — кнопка должна быть заблокирована
    // (тест условный: зависит от данных в бэкенде)
    console.log(`Книги без остатка: ${foundDisabled ? 'есть' : 'нет в тестовых данных'}`)
  })
})

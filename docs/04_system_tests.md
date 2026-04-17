# 🌐 Уровень 4: Системные тесты / E2E

## Что тестируем на системном уровне?

**E2E (End-to-End)** — «от начала до конца».  
Тест запускает реальный браузер и симулирует действия настоящего пользователя.

```
[Playwright] → открывает браузер Chrome
            → переходит на localhost:3000
            → кликает "Добавить в корзину"
            → заполняет email
            → нажимает "Оформить заказ"
            → проверяет, что всё прошло успешно
```

---

## Playwright — современный инструмент E2E

### Почему Playwright, а не Selenium?

| | Playwright | Selenium |
|--|--|--|
| Автоожидание | ✅ встроено | ❌ нужно писать вручную |
| Скриншоты/видео | ✅ из коробки | ❌ плагины |
| Кросс-браузерность | Chrome, FF, Safari, мобильные | Chrome, FF, Edge |
| Codegen | ✅ запись тестов кликами | ❌ нет |
| Trace Viewer | ✅ | ❌ |
| API | async/await | колбэки → промисы |

### Запись теста ("Codegen")

Playwright умеет **записывать тест по вашим действиям в браузере**!

```bash
npx playwright codegen localhost:3000
```

Откроется браузер + окно с кодом теста. Просто кликай — тест пишется сам!

---

## Автоожидание (Auto-waiting)

Это суперсила Playwright. Не нужно писать `sleep(2000)` или ждать своё.

```javascript
// Playwright САМИ ждёт, пока:
await catalog.booksGrid.waitFor()          // элемент появится
await expect(btn).toBeEnabled()            // кнопка станет активной
await expect(toast).toContainText('...')   // текст изменится
```

Все `expect` в Playwright по умолчанию ждут до 5-30 секунд (настраивается).

---

## Page Object Model (POM)

Паттерн для организации E2E тестов.

**Проблема без POM:**
```javascript
// Если data-testid изменится — придётся менять во всех тестах!
await page.getByTestId('add-to-cart-btn').click()  // в тесте 1
await page.getByTestId('add-to-cart-btn').click()  // в тесте 2
await page.getByTestId('add-to-cart-btn').click()  // в тесте 3
```

**Решение с POM:**
```javascript
// CatalogPage.js — все локаторы в одном месте
class CatalogPage {
  addToCartButtons() {
    return this.page.getByTestId('add-to-cart-btn')
  }
  async addFirstBookToCart() {
    await this.addToCartButtons().first().click()
  }
}

// Тесты используют методы POM
await catalog.addFirstBookToCart()  // в тесте 1
await catalog.addFirstBookToCart()  // в тесте 2
// При изменении locator-а — правим только в POM!
```

---

## test.step() — шаги в отчёте

```javascript
test('Happy Path: оформление заказа', async ({ page }) => {
  await test.step('Открываем главную страницу', async () => {
    await page.goto('/')
  })
  await test.step('Добавляем книгу в корзину', async () => {
    await catalog.addToCartButtons().first().click()
  })
  await test.step('Оформляем заказ', async () => {
    await cart.fillEmail('test@mail.ru')
    await cart.checkout()
  })
})
```

В отчёте Allure/Playwright эти шаги будут видны как дерево — понятно, на каком шаге упало!

---

## Виды бизнес-сценариев

### Happy Path (счастливый путь)
Всё идёт как ожидается. Тест покрывает **основной** сценарий использования.

### Sad Path (грустный путь)
Пользователь что-то сделал неправильно. Тест проверяет **обработку ошибок**.

### Edge Cases (граничные случаи)
Нестандартные ситуации: пустая корзина, книга закончилась, сеть упала.

```javascript
// Happy path
test('Покупатель успешно оформляет заказ', ...)

// Sad path
test('Заказ без email → показывает ошибку', ...)
test('Книга не в наличии → кнопка заблокирована', ...)

// Edge case
test('При ошибке сети → показывает сообщение об ошибке', ...)
```

---

## Screenshot Testing (визуальная регрессия)

Playwright умеет сравнивать скриншоты страницы с эталоном.

```javascript
// При первом запуске — создаётся файл catalog-page.png (эталон)
// При следующих запусках — сравнивается с эталоном
await expect(page).toHaveScreenshot('catalog-page.png', {
    maxDiffPixelRatio: 0.02  // допускаем 2% пикселей различий
})
```

Если неожиданно изменился дизайн → тест упадёт!

---

## Trace Viewer — «чёрный ящик» теста

При падении теста Playwright записывает **трэйс** (trace):
- Все шаги с таймстемпами
- Скриншот на каждом действии
- Сетевые запросы
- Консольные ошибки

```bash
# Просмотреть трэйс в браузере:
npx playwright show-trace test-results/trace.zip
```

---

## Смотри код

**Каталог:** [`tests/e2e/catalog.spec.js`](../frontend/tests/e2e/catalog.spec.js)  
**Оформление заказа:** [`tests/e2e/order-flow.spec.js`](../frontend/tests/e2e/order-flow.spec.js)  
**Page Objects:** [`tests/e2e/pages/CatalogPage.js`](../frontend/tests/e2e/pages/CatalogPage.js)

---

## Запуск

```bash
cd frontend

# E2E тесты (нужен запущенный приложение)
npm run test:e2e

# С видимым браузером (для демонстрации!)
npm run test:e2e:headed

# Запись теста кликами
npx playwright codegen localhost:3000

# Просмотр отчёта
npx playwright show-report
```

---

## Вопросы для обсуждения

1. Почему E2E тестов должно быть мало?
2. В чём разница между happy path и edge case?
3. Почему скриншот-тесты иногда «флакают» (падают случайно)?
4. Что лучше: один длинный E2E тест или несколько коротких?

---

## ➡️ Следующий раздел

[05_mocking.md](05_mocking.md) — Мокирование на всех уровнях

# 📊 Allure и интеграция с TMS

## Что такое Allure?

**Allure** — это фреймворк для генерации красивых отчётов о тестировании.

**TMS (Test Management System)** — система управления тест-кейсами  
(например, TestRail, Zephyr, Allure TestOps).

Связка **Allure + JUnit5/Playwright** позволяет:
- Генерировать HTML-отчёты прямо из кода тестов
- Группировать тесты по фичам и историям
- Видеть тесты по уровням пирамиды
- Прикреплять скриншоты, логи, JSON к тест-кейсам
- Интегрироваться с CI/CD (Jenkins, GitLab CI, GitHub Actions)

---

## Аннотации Allure (Java)

```java
@Epic("Управление книгами")       // Крупная область (продукт)
@Feature("Логика скидок")         // Функция внутри эпика
@Story("Скидка 100% → цена 0")   // Конкретный тест-кейс

@Layer("unit")                    // ← УРОВЕНЬ ПИРАМИДЫ!
@Severity(SeverityLevel.BLOCKER)  // Критичность: BLOCKER > CRITICAL > NORMAL > MINOR

@DisplayName("Понятное название теста")
@Description("Подробное описание что и почему проверяем")
@Link("https://jira.example.com/BUG-123")  // ссылка на задачу
```

### Иерархия в Allure:
```
Epic: "Управление книгами"
  └── Feature: "Логика скидок"
        └── Story: "Скидка 100%"
              └── Test: "shouldReturnZero_whenDiscountIs100"
```

---

## Шаги в отчёте (@Step)

```java
@Test
void shouldCreateOrder() {
    Book book = createBook();          // шаги в коде
    Order order = placeOrderFor(book); // ...

    assertThatOrderIsValid(order);
}

@Step("Создаём тестовую книгу")
private Book createBook() {
    return bookRepository.save(Book.builder()...build());
}

@Step("Оформляем заказ для книги {book.title}")
private Order placeOrderFor(Book book) {
    return orderService.placeOrder("test@mail.ru", List.of(...));
}
```

В отчёте эти шаги видны как дерево — сразу понятно, где упало.

---

## Как Allure разметить тест по уровню пирамиды?

Используем `@Layer` + теги JUnit5:

```java
// Unit-тест
@Tag("unit")
@Layer("unit")
@Severity(SeverityLevel.NORMAL)
void testApplyDiscount() { ... }

// Компонентный тест
@Tag("component")
@Layer("component")
@Severity(SeverityLevel.CRITICAL)
void testOrderCreation() { ... }

// Интеграционный тест
@Tag("integration")
@Layer("integration")
@Severity(SeverityLevel.BLOCKER)
void testIsbnIntegration() { ... }
```

---

## Запуск и просмотр отчёта

```bash
cd backend

# 1. Запустить тесты (результаты попадут в allure-results/)
mvn test

# 2. Сгенерировать и открыть отчёт
mvn allure:serve

# Или сначала сгенерировать, потом открыть:
mvn allure:report
open target/site/allure-maven-plugin/index.html
```

### Для фронтенда:
```bash
cd frontend

# Запуск тестов с генерацией allure-results
npm test           # unit + component (allure-vitest)
npm run test:e2e   # E2E (allure-playwright)

# Объединить и открыть отчёт
npm run allure:generate
npm run allure:open
```

---

## Структура Allure-отчёта

```
Отчёт
├── 📊 Overview        — общая статистика
├── 🗂️ Suites          — по файлам/классам тестов
├── 🏷️ Behaviors       — по Epic → Feature → Story
│     ├── Управление книгами
│     │   └── Логика скидок
│     │       ├── ✅ Скидка 0% не меняет цену
│     │       ├── ✅ Скидка 100% → цена 0
│     │       └── ❌ BLOCKER: Скидка -5% → ошибка
│     └── ...
├── 🪄 Categories      — группировка по типам падений
├── 📈 Timeline        — хронология выполнения тестов
└── 🔗 Graph           — pie chart статусов
```

---

## Allure в CI/CD

```yaml
# .github/workflows/tests.yml (GitHub Actions)
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run tests
        run: mvn test

      - name: Generate Allure Report
        uses: simple-elf/allure-report-action@master
        with:
          allure_results: target/allure-results

      - name: Publish Allure Report
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./allure-report
```

---

## Пирамида в Allure TestOps

При использовании Allure TestOps (платная TMS):

1. Каждый `@Layer("unit")` текст попадает в соответствующий слой
2. Можно фильтровать тесты по слою: «покажи только unit-тесты»
3. Видна статистика по пирамиде: сколько тестов на каком уровне
4. Деградация: «было 70% unit, стало 40% — почему?»

---

## Полезные советы

### 1. Пиши понятные DisplayName
```java
// ❌ Плохо
void test1() { ... }

// ✅ Хорошо
@DisplayName("Нельзя оформить заказ с отрицательным количеством")
void shouldThrowException_whenQuantityIsNegative() { ... }
```

### 2. Правильно ставь Severity
- `BLOCKER` — сломана критическая функция, нельзя работать
- `CRITICAL` — серьёзная ошибка, есть обходной путь
- `NORMAL` — стандартный дефект
- `MINOR` — незначительное отклонение
- `TRIVIAL` — косметика, опечатки

### 3. Прикладывай данные к тест-кейсу
```java
// Прикладываем запрос/ответ к отчёту для диагностики
Allure.addAttachment("Request body", "application/json", requestJson, "json");
Allure.addAttachment("Response", "application/json", responseJson, "json");
```

---

## Вопросы для обсуждения

1. Зачем бизнесу видеть Allure-отчёты? Кто их читает?
2. Как @Epic/@Feature/@Story помогают навигации в больших проектах?
3. Почему важно правильно ставить `@Severity`?
4. Как Allure помогает при анализе flaky-тестов (нестабильных)?

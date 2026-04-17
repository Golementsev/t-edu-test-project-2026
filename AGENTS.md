# AGENTS.md — QA Practice: Пирамида тестирования

Этот файл описывает проект для AI-агентов, которые работают с кодовой базой.
Прочитай его **полностью** перед тем, как вносить любые изменения.

---

## 🎯 Назначение проекта

**QA Practice** — образовательный проект для демонстрации школьникам всех уровней
пирамиды тестирования. Проект специально спроектирован так, чтобы каждый тестовый
файл был **самодостаточным учебным примером**: содержит объясняющие комментарии,
поступательно усложняется, и чётко показывает разницу между уровнями.

> ⚠️ **Ключевой принцип:** Код тестов — это учебный материал. Комментарии в тестовых
> файлах — это объяснения для студентов, а не артефакты. Их нельзя удалять или
> сокращать без явного запроса.

---

## 🗂️ Структура репозитория

```
qa_practice/
├── AGENTS.md              ← этот файл
├── skills.md              ← навыки агентов
├── README.md              ← точка входа для человека
├── docs/                  ← учебные материалы (7 файлов Markdown)
├── backend/               ← Java 17 / Spring Boot 3
│   ├── pom.xml
│   ├── docker-compose.yml
│   ├── Dockerfile
│   ├── wiremock/mappings/ ← JSON-стабы для WireMock
│   └── src/
│       ├── main/java/ru/qaschool/bookstore/
│       │   ├── BookstoreApplication.java
│       │   ├── config/          AppConfig.java (RestTemplate бин)
│       │   ├── controller/      BookController, OrderController, BookEnrichController
│       │   ├── domain/          Book, Author, Order, OrderItem (JPA entities)
│       │   ├── exception/       BookNotFoundException, OutOfStockException, GlobalExceptionHandler
│       │   ├── repository/      BookRepository, AuthorRepository, OrderRepository
│       │   └── service/         BookService, OrderService, IsbnLookupClient, NotificationClient
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/init.sql
│       └── test/java/ru/qaschool/bookstore/
│           ├── unit/            BookServiceUnitTest.java
│           ├── component/       OrderServiceComponentTest.java, BookRepositorySliceTest.java
│           ├── blackbox/        BookApiBlackBoxTest.java
│           └── integration/     IsbnIntegrationTest.java
│       └── test/resources/
│           └── application-test.yml
└── frontend/              ← React 18 / Vite
    ├── package.json
    ├── vite.config.js
    ├── playwright.config.js
    ├── index.html
    └── src/
        ├── App.jsx, main.jsx, index.css
        ├── api/             booksApi.js (axios клиент)
        ├── components/      BookCard.jsx, Cart.jsx, BookList.jsx
        ├── utils/           priceUtils.js (чистые функции)
        ├── test-setup.js
        └── __tests__/
            ├── unit/        priceUtils.test.js
            └── component/   BookCard.test.jsx, BookList.test.jsx
    └── tests/e2e/
        ├── pages/           CatalogPage.js (Page Object Model)
        ├── catalog.spec.js
        └── order-flow.spec.js
```

---

## 🏗️ Архитектура приложения

### Backend: BookStore API

```
[HTTP Request]
     ↓
[BookController / OrderController / BookEnrichController]
     ↓
[BookService / OrderService]          ← БИЗНЕС-ЛОГИКА
     ↓                  ↓
[BookRepository]   [IsbnLookupClient]  [NotificationClient]
[OrderRepository]  (HTTP → Google)     (HTTP → NotifService)
     ↓
[PostgreSQL / H2]
```

**Домен:**
- `Book` — книга (title, isbn, price, stockQuantity, genre, author)
- `Author` — автор (firstName, lastName, email)
- `Order` — заказ (customerEmail, status, items, createdAt)
- `OrderItem` — позиция заказа (book, quantity, price — фиксируется при создании)

**Важные бизнес-правила (их покрывают тесты):**
- `BookService.applyDiscount()` — скидка: 0% → нет изменений, 100%+ → 0, <0 → exception
- `BookService.reserveStock()` — уменьшает `stockQuantity`, бросает `OutOfStockException`
- `OrderService.placeOrder()` — транзакция: резервирует все книги, затем шлёт уведомление
- `OrderService.cancelOrder()` — возвращает остаток на склад; нельзя отменить SHIPPED/DELIVERED
- ISBN уникален (ограничение на уровне БД и сервисного слоя)

**Внешние HTTP-зависимости (всегда мокируются в тестах ниже системного уровня):**
- `IsbnLookupClient` → `${isbn.service.url}` (Google Books API)
- `NotificationClient` → `${notification.service.url}` (email-сервис)

### Frontend: React SPA

**Компоненты:**
- `BookList` — страница каталога, управляет состоянием (книги, корзина, поиск, toasts)
- `BookCard` — карточка книги, принимает `book` и `onAddToCart` callback
- `Cart` — модальная панель корзины с оформлением заказа

**Утилиты (`utils/priceUtils.js`):**
- `formatPrice(price)` — форматирование в рубли через `Intl.NumberFormat`
- `applyDiscount(price, pct)` — те же правила, что и на бэкенде
- `calculateCartTotal(items)` — сумма позиций
- `isValidIsbn(isbn)` — regexp `/^\d{3}-\d{10}$/`
- `getGenreLabel(genre)` — EN enum → RU название

**API-клиент (`api/booksApi.js`):** axios, baseURL `/api`, proxy → `localhost:8080`

---

## 🧪 Тестовая архитектура

### Уровни и технологии

| Уровень | Тег JUnit | Maven профиль | Технологии | Мокирование |
|---------|-----------|---------------|-----------|-------------|
| Unit | `@Tag("unit")` | `-P unit` | JUnit5, Mockito, AssertJ | `@Mock` / `@InjectMocks` |
| Component | `@Tag("component")` | `-P component` | `@SpringBootTest`, `@DataJpaTest` | `@MockBean` для HTTP-клиентов |
| Black-box | `@Tag("blackbox")` | `-P blackbox` | Testcontainers, RestAssured | `@MockBean` для HTTP-клиентов |
| Integration | `@Tag("integration")` | `-P integration` | WireMock, Testcontainers, RestAssured | WireMock stub server |

### Конфигурация тестов (backend)

- **Профиль `test`** (`application-test.yml`): H2 in-memory, `ddl-auto: create-drop`
- **Testcontainers**: `@DynamicPropertySource` переопределяет datasource URL
- **WireMock**: `@DynamicPropertySource` переопределяет `isbn.service.url`
- **Allure**: `@Layer`, `@Epic`, `@Feature`, `@Story`, `@Severity` на всех тест-кейсах

### Конфигурация тестов (frontend)

- **Vitest**: `environment: 'jsdom'`, `globals: true`, setupFiles: `test-setup.js`
- **MSW**: `setupServer()` в тестах с `http.get/post()` хендлерами
- **Playwright**: `baseURL: localhost:3000`, webServer запускает `npm run dev`
- Отчёты: `allure-playwright` (E2E) и `allure-vitest` (unit/component)

---

## 📐 Соглашения, которые нужно соблюдать

### Именование тестов (Java)

```
// Формат: should<Что>_when<Условие>
void shouldReturnZero_whenDiscountIs100() { ... }
void shouldThrowOutOfStock_whenQuantityExceeds() { ... }
void shouldCreateOrder_andDecrementStock() { ... }
```

### Именование тестов (JavaScript)

```javascript
// Формат: свободный, но человекочитаемый русскоязычный
it('возвращает 0 при скидке 100%', () => { ... })
it('показывает "Нет в наличии" при stockQuantity = 0', () => { ... })
```

### Структура тестового файла (Java)

1. Блок Javadoc с пояснением уровня и технологий (обязателен в каждом файле)
2. Поля: моки, автовайр, тестовые данные
3. `@Nested` классы группируют кейсы по методу или сценарию
4. Каждый тест: `@Test` + Allure-аннотации + `@DisplayName`
5. Структура внутри теста: комментарии `// ARRANGE`, `// ACT`, `// ASSERT`

### data-testid в компонентах (React)

Все интерактивные элементы и информационные блоки **должны иметь** `data-testid`:
- Формат: `kebab-case`, осмысленные имена (напр. `add-to-cart-btn`, `cart-count`)
- Они используются в Playwright и Testing Library тестах
- Не удалять и не переименовывать без обновления тестов

### Allure-аннотации (обязательны для всех тестов)

```java
@Layer("unit")          // unit | component | blackbox | integration | e2e
@Epic("...")            // бизнес-область
@Feature("...")         // функция
@Story("...")           // конкретный сценарий
@Severity(...)          // BLOCKER | CRITICAL | NORMAL | MINOR | TRIVIAL
```

---

## ⛔ Что нельзя делать

1. **Не удалять учебные комментарии** из тестовых файлов — это контент для занятий
2. **Не использовать H2** в тестах с тегом `blackbox` или `integration` — там обязателен Testcontainers
3. **Не делать реальных HTTP-запросов** в тестах уровней unit/component/blackbox — только моки
4. **Не убирать `@Transactional`** с компонентных тестов — это нарушит изоляцию тест-кейсов
5. **Не менять форматы ISBN** в тестовых фикстурах — формат `978-XXXXXXXXXX` важен для демонстрации валидации
6. **Не добавлять зависимость без pom.xml** — все зависимости только через Maven/npm

---

## 🔧 Окружение

| Инструмент | Версия | Где нужен |
|-----------|--------|-----------|
| Java | 17+ | backend (compile + test) |
| Maven | 3.8+ | backend build |
| Docker | любая | blackbox + integration тесты, docker-compose |
| Node.js | 18+ | frontend |
| npm | 9+ | frontend |

**Переменные окружения** (для production, при тестах переопределяются):
- `SPRING_DATASOURCE_URL` — URL PostgreSQL
- `ISBN_SERVICE_URL` — URL Google Books API (по умолчанию `https://www.googleapis.com/books/v1`)
- `NOTIFICATION_SERVICE_URL` — URL сервиса уведомлений

---

## 🚀 Команды запуска

```bash
# ── Backend ────────────────────────────────────────────────────────
mvn test -P unit          # Unit-тесты (без Docker, мс)
mvn test -P component     # Компонентные (H2, секунды)
mvn test -P blackbox      # Black-box (Docker обязателен)
mvn test -P integration   # Integration (Docker обязателен)
mvn test                  # Все тесты
mvn allure:serve          # Открыть Allure-отчёт в браузере

docker-compose up -d postgres    # Только БД
mvn spring-boot:run              # Запустить приложение

# ── Frontend ───────────────────────────────────────────────────────
npm install               # Установить зависимости
npm run dev               # Dev-сервер (порт 3000)
npm test                  # Unit + Component (Vitest)
npm run test:e2e          # E2E headless (Playwright)
npm run test:e2e:headed   # E2E с видимым браузером (для демо!)
npx playwright codegen localhost:3000  # Запись теста кликами
npm run allure:generate && npm run allure:open  # Allure E2E отчёт
```

# 📚 QA Practice — Пирамида тестирования

> **Учебный проект для практики автотестирования**  
> Bookstore: магазин книг с Backend (Java/Spring Boot) и Frontend (React)

---

## 🗂️ Структура проекта

```
qa_practice/
├── backend/          — Java Spring Boot API
├── frontend/         — React приложение
└── docs/             — Материалы для занятий
    ├── 00_intro.md
    ├── 01_unit_tests.md
    ├── 02_component_tests.md
    ├── 03_integration_tests.md
    ├── 04_system_tests.md
    ├── 05_mocking.md
    └── 06_allure_tms.md
```

---

## 🏗️ Пирамида тестирования

```
                    ╔═══════════════╗
                    ║   СИСТЕМНЫЕ   ║  ← E2E, Playwright, бизнес-сценарии
                    ║    (E2E)      ║    Мало, медленные, дорогие
                    ╚═══════════════╝
               ╔═══════════════════════╗
               ║   ИНТЕГРАЦИОННЫЕ      ║  ← WireMock, RestAssured, стенды
               ║  (Integration)        ║    Средне, секунды
               ╚═══════════════════════╝
          ╔═════════════════════════════════╗
          ║       КОМПОНЕНТНЫЕ              ║  ← Spring Context / Testing Library
          ║  (Component / API slice)        ║    Spring Boot Test, @DataJpaTest
          ╚═════════════════════════════════╝
    ╔══════════════════════════════════════════════╗
    ║              ЮНИТ-ТЕСТЫ                      ║  ← JUnit5, Mockito, Vitest
    ║             (Unit)                           ║    Много, миллисекунды, дешёвые
    ╚══════════════════════════════════════════════╝
```

| Уровень | Стек (Backend) | Стек (Frontend) | Скорость | Количество |
|---------|---------------|----------------|---------|-----------|
| Unit | JUnit 5 + Mockito | Vitest | мс | 70% |
| Component | Spring Boot Test + H2 | @testing-library/react + MSW | сек | 20% |
| Integration | WireMock + RestAssured + Testcontainers | — | сек | 8% |
| E2E / System | RestAssured (live) | Playwright | мин | 2% |

---

## 🚀 Быстрый старт

### Backend

```bash
# Запуск инфраструктуры (PostgreSQL)
cd backend
docker-compose up -d postgres

# Запуск приложения
mvn spring-boot:run

# Запуск Unit-тестов (fast!)
mvn test -P unit

# Запуск компонентных тестов
mvn test -P component

# Запуск black-box тестов (нужен Docker)
mvn test -P blackbox

# Все тесты + Allure-отчёт
mvn test allure:serve
```

### Frontend

```bash
cd frontend

# Установка зависимостей
npm install

# Запуск приложения
npm run dev

# Unit + Component тесты (Vitest)
npm test

# E2E тесты (Playwright) — нужен запущенный бэкенд или моки
npm run test:e2e

# E2E с видимым браузером
npm run test:e2e:headed

# Записать новый тест кликами (Codegen!)
npx playwright codegen localhost:3000
```

---

## 📖 Материалы для занятий

| Тема | Файл |
|------|------|
| Введение: что такое пирамида тестирования | [docs/00_intro.md](docs/00_intro.md) |
| Уровень 1: Unit-тесты | [docs/01_unit_tests.md](docs/01_unit_tests.md) |
| Уровень 2: Компонентные тесты | [docs/02_component_tests.md](docs/02_component_tests.md) |
| Уровень 3: Интеграционные тесты | [docs/03_integration_tests.md](docs/03_integration_tests.md) |
| Уровень 4: Системные / E2E тесты | [docs/04_system_tests.md](docs/04_system_tests.md) |
| Мокирование на всех уровнях | [docs/05_mocking.md](docs/05_mocking.md) |
| Allure TMS и разметка тестов | [docs/06_allure_tms.md](docs/06_allure_tms.md) |

---

## 🎓 Где что смотреть

### Backend тесты

| Файл | Уровень | Что показывает |
|------|---------|---------------|
| `unit/BookServiceUnitTest.java` | **Unit** | Mockito, параметризованные тесты, Allure |
| `component/OrderServiceComponentTest.java` | **Component** | @SpringBootTest, @MockBean, транзакции |
| `component/BookRepositorySliceTest.java` | **Component** | @DataJpaTest, JPA slice |
| `blackbox/BookApiBlackBoxTest.java` | **Black-box** | Testcontainers, RestAssured |
| `integration/IsbnIntegrationTest.java` | **Integration** | WireMock, graceful degradation |

### Frontend тесты

| Файл | Уровень | Что показывает |
|------|---------|---------------|
| `__tests__/unit/priceUtils.test.js` | **Unit** | Vitest, чистые функции, test.each |
| `__tests__/component/BookCard.test.jsx` | **Component** | Testing Library, рендер, события |
| `__tests__/component/BookList.test.jsx` | **Component** | MSW mock server, async тесты |
| `tests/e2e/catalog.spec.js` | **E2E** | Playwright, POM, снимки |
| `tests/e2e/order-flow.spec.js` | **E2E** | Бизнес-сценарии, test.step |

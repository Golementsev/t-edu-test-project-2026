# skills.md — Навыки агентов для QA Practice

Этот файл описывает специализированные навыки (skills) — наборы инструкций для агентов,
выполняющих конкретные задачи в проекте. Каждый навык содержит контекст, шаги и критерии
завершения.

---

## 📋 Индекс навыков

| ID | Навык | Применение |
|----|-------|-----------|
| [SK-01](#sk-01-написание-unit-теста-java) | Написание Unit-теста (Java) | Добавление нового unit-теста для сервисов |
| [SK-02](#sk-02-написание-компонентного-теста-java) | Написание Component-теста (Java) | Тесты со Spring-контекстом и H2 |
| [SK-03](#sk-03-написание-blackbox-теста-java) | Написание Black-box теста (Java) | API-тесты с Testcontainers |
| [SK-04](#sk-04-написание-интеграционного-теста-с-wiremock) | Интеграционный тест с WireMock | Тесты взаимодействия с внешними HTTP-сервисами |
| [SK-05](#sk-05-написание-unit-теста-javascript) | Unit-тест (JavaScript/Vitest) | Тесты чистых функций во фронтенде |
| [SK-06](#sk-06-написание-компонентного-теста-react) | Component-тест (React/Testing Library) | Тесты рендеринга и взаимодействия |
| [SK-07](#sk-07-написание-e2e-теста-playwright) | E2E-тест (Playwright) | Сценарийные тесты в браузере |
| [SK-08](#sk-08-добавление-нового-api-endpoint) | Добавление нового API endpoint | Расширение REST API с тестами |
| [SK-09](#sk-09-добавление-бизнес-правила) | Добавление бизнес-правила | Новая логика в сервисе с покрытием тестами |
| [SK-10](#sk-10-debugging-упавшего-теста) | Debugging упавшего теста | Диагностика и исправление падения |

---

## SK-01: Написание Unit-теста (Java)

**Когда применять:** нужно протестировать метод сервиса без поднятия Spring-контекста.

### Контекст
- Тесты живут в `backend/src/test/java/ru/qaschool/bookstore/unit/`
- Класс помечается `@Tag("unit")` и `@ExtendWith(MockitoExtension.class)`
- **Никакого** `@SpringBootTest` — только `@Mock` и `@InjectMocks`
- Все зависимости сервиса заменяются `@Mock`
- Профиль Maven: `-P unit`

### Шаги

1. **Определи тестируемый метод** — найди в `service/` метод с бизнес-логикой
2. **Создай класс** в `unit/` по шаблону:

```java
@Tag("unit")
@Epic("...") @Feature("...") 
@ExtendWith(MockitoExtension.class)
class <ServiceName>UnitTest {

    @Mock private <DependencyType> dependency;
    @InjectMocks private <ServiceName> service;
```

3. **Для каждого тест-кейса** добавь `@Nested` класс под метод:

```java
@Nested
@DisplayName("Метод <methodName> — <описание>")
class <MethodName>Tests {

    @Test
    @Story("...") @Layer("unit") @Severity(SeverityLevel.NORMAL)
    @DisplayName("...")
    void should<What>_when<Condition>() {
        // ARRANGE
        when(dependency.someMethod(any())).thenReturn(value);

        // ACT
        Result result = service.methodUnderTest(args);

        // ASSERT
        assertThat(result)...;
        verify(dependency, times(1)).someMethod(any());
    }
}
```

4. **Добавь параметризованные тесты** для граничных значений через `@MethodSource`
5. **Добавь негативный кейс** с `assertThatThrownBy()`
6. **Добавь `@BeforeEach`** если тестовые данные общие для класса

### Критерии завершения

- [ ] Класс помечен `@Tag("unit")` и `@ExtendWith(MockitoExtension.class)`
- [ ] Нет `@SpringBootTest`, нет `@Autowired`
- [ ] Есть Allure-аннотации: `@Layer("unit")`, `@Epic`, `@Feature`, `@Story`, `@Severity`
- [ ] Есть блок-комментарий в стиле существующих тестов (объяснение уровня)
- [ ] `mvn test -P unit` проходит без ошибок
- [ ] Покрыты: happy path, отрицательный кейс, граничные значения

---

## SK-02: Написание компонентного теста (Java)

**Когда применять:** нужно проверить взаимодействие сервиса с реальной БД (H2).

### Контекст
- Тесты в `backend/src/test/java/ru/qaschool/bookstore/component/`
- Класс: `@Tag("component")`, `@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`
- БД: H2 (из `application-test.yml`)
- HTTP-клиенты: `@MockBean`
- Для тестирования только JPA-слоя: `@DataJpaTest` вместо `@SpringBootTest`

### Варианты аннотаций

```java
// Вариант А: весь контекст Spring
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SomeServiceComponentTest { ... }

// Вариант Б: только JPA-слой (для тестов репозиториев)
@DataJpaTest
@ActiveProfiles("test")
class SomeRepositorySliceTest { ... }
```

### Шаги

1. **Выбери вариант** (A или Б) исходя из того, что тестируешь
2. **Задай тестовые данные в `@BeforeEach`** через `@Autowired` репозиторий (сохрани сущности)
3. **Замокируй HTTP-клиенты** (`@MockBean NotificationClient`, `@MockBean IsbnLookupClient`)
4. **Напиши тест-кейсы**, проверяя побочные эффекты через репозиторий:

```java
// После действия — читаем из реальной H2 и проверяем
Book updated = bookRepository.findById(id).orElseThrow();
assertThat(updated.getStockQuantity()).isEqualTo(expectedQty);
```

5. **Проверь откат транзакции**: убедись, что следующий тест стартует с чистыми данными

### Критерии завершения

- [ ] `@Tag("component")`, `@ActiveProfiles("test")`, `@Transactional` proставлены
- [ ] Нет прямых HTTP-вызовов без `@MockBean`
- [ ] `@Layer("component")` на каждом тест-кейсе
- [ ] `mvn test -P component` проходит
- [ ] Удостоверься, что тесты не зависят от порядка выполнения

---

## SK-03: Написание Black-box теста (Java)

**Когда применять:** нужно протестировать HTTP API против реального PostgreSQL.

### Контекст
- Тесты в `backend/src/test/java/ru/qaschool/bookstore/blackbox/`
- `@Testcontainers` + `static PostgreSQLContainer<?>`
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- RestAssured для HTTP-запросов
- Docker должен быть запущен

### Шаблон класса

```java
@Tag("blackbox")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class <Feature>BlackBoxTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bookstore_test")
            .withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort private int port;
    @MockBean private NotificationClient notificationClient;
    @MockBean private IsbnLookupClient isbnLookupClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
```

### Шаги

1. **Создай класс** по шаблону выше
2. **Пиши тесты в стиле `given/when/then`** RestAssured
3. **Проверяй HTTP-статус и тело** (JSONPath)
4. **Для многошаговых сценариев** извлекай ID через `.extract().path("id")`

### Критерии завершения

- [ ] `@Tag("blackbox")`, `@Testcontainers`, `@DynamicPropertySource` проставлены
- [ ] `@Layer("blackbox")` на каждом тест-кейсе
- [ ] WireMock не используется (HTTP-клиенты через `@MockBean`)
- [ ] `mvn test -P blackbox` проходит при запущенном Docker

---

## SK-04: Написание интеграционного теста с WireMock

**Когда применять:** нужно проверить поведение при разных ответах внешнего HTTP-сервиса.

### Контекст
- Тесты в `backend/src/test/java/ru/qaschool/bookstore/integration/`
- `@RegisterExtension WireMockExtension` с `dynamicPort()`
- `@DynamicPropertySource` перенаправляет `isbn.service.url` на WireMock
- Testcontainers для PostgreSQL (не H2!)

### Шаблон WireMock-стаба

```java
// Успешный ответ
wireMock.stubFor(get(urlPathMatching("/volumes.*"))
    .withQueryParam("q", containing("978-..."))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{ ... }")));

// Ошибка сервера
wireMock.stubFor(get(anyUrl())
    .willReturn(aResponse().withStatus(503)));

// Таймаут
wireMock.stubFor(get(anyUrl())
    .willReturn(aResponse().withFixedDelay(10_000)));
```

### Шаги

1. **Определи сценарии**: успех, ошибка, таймаут, пустой ответ
2. **Настрой стаб** перед действием
3. **Вызови API** через RestAssured
4. **Проверь verify**: убедись, что нужный запрос ушёл в WireMock
5. **Сбрось стабы** в `@BeforeEach` через `wireMock.resetAll()`

### Критерии завершения

- [ ] `@Tag("integration")`, `@Layer("integration")`
- [ ] `wireMock.resetAll()` в `@BeforeEach`
- [ ] Каждый сценарий: настройка стаба → действие → verify
- [ ] Проверена graceful degradation (503 и таймаут не роняют приложение с 500)
- [ ] `mvn test -P integration` проходит

---

## SK-05: Написание Unit-теста (JavaScript/Vitest)

**Когда применять:** нужно протестировать чистую функцию из `utils/` или `api/`.

### Контекст
- Тесты в `frontend/src/__tests__/unit/`
- Файл: `<moduleName>.test.js`
- Vitest с `globals: true` — `describe/it/expect` доступны без импорта
- Нет DOM, нет React, нет HTTP

### Шаблон

```javascript
import { describe, it, expect, test } from 'vitest'
import { functionUnderTest } from '../../utils/<module>'

describe('<functionName> — <краткое описание>', () => {

  it('<что делает при каком условии>', () => {
    // arrange
    const input = ...
    // act
    const result = functionUnderTest(input)
    // assert
    expect(result).toBe(expected)
  })

  it('бросает ошибку при <条件>', () => {
    expect(() => functionUnderTest(invalidInput)).toThrow('...')
  })

  test.each([
    [input1, expected1],
    [input2, expected2],
  ])('<description> (%s → %s)', (input, expected) => {
    expect(functionUnderTest(input)).toBe(expected)
  })
})
```

### Критерии завершения

- [ ] Файл в `__tests__/unit/`
- [ ] Нет `render()`, нет `screen`, нет HTTP
- [ ] `test.each` для параметризованных кейсов
- [ ] Покрыты: граничное значение, ошибочный ввод, нормальное значение
- [ ] `npm test` проходит

---

## SK-06: Написание компонентного теста (React/Testing Library)

**Когда применять:** нужно протестировать рендеринг или взаимодействие с компонентом.

### Контекст
- Тесты в `frontend/src/__tests__/component/`
- `@testing-library/react` + `userEvent`
- Для тестов с HTTP: MSW (`setupServer`) перехватывает axios-запросы
- `vi.fn()` для мок-колбэков

### Шаги для теста с MSW

```javascript
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

const server = setupServer(
  http.get('/api/books', () => HttpResponse.json(MOCK_BOOKS)),
)

beforeEach(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterEach(() => server.close())
```

### Правила поиска элементов (приоритет)

```javascript
// 1. По роли (лучше всего — проверяет доступность)
screen.getByRole('button', { name: /добавить/i })
screen.getByRole('heading', { name: 'Каталог' })

// 2. По лейблу
screen.getByLabelText('Email')

// 3. По тексту
screen.getByText('Чистый код')

// 4. По testid (последний резерв)
screen.getByTestId('cart-count')
```

### Критерии завершения

- [ ] Файл в `__tests__/component/`
- [ ] `userEvent.setup()` для симуляции действий (не `fireEvent`)
- [ ] `waitFor` для асинхронных проверок
- [ ] MSW настроен и сбрасывается между тестами
- [ ] `npm test` проходит

---

## SK-07: Написание E2E-теста (Playwright)

**Когда применять:** нужно протестировать бизнес-сценарий через браузер.

### Контекст
- Тесты в `frontend/tests/e2e/`
- POM-классы в `frontend/tests/e2e/pages/`
- `baseURL: localhost:3000`, приложение запускается автоматически через `webServer`
- Для демо-режима: `npm run test:e2e:headed`

### Шаблон бизнес-сценария

```javascript
import { test, expect } from '@playwright/test'
import { CatalogPage } from './pages/CatalogPage'

test.describe('<Название сценария>', () => {

  test('<Happy/Sad path>: <описание>', async ({ page }) => {
    const catalog = new CatalogPage(page)

    await test.step('<Шаг 1 — понятное название>', async () => {
      await catalog.navigate()
      await expect(page).toHaveTitle(/BookStore/)
    })

    await test.step('<Шаг 2>', async () => {
      await expect(catalog.booksGrid).toBeVisible({ timeout: 10_000 })
      await catalog.addToCartButtons().first().click()
    })

    // ... остальные шаги
  })
})
```

### Правила локаторов

```javascript
// ✅ Предпочтительно
page.getByRole('button', { name: /В корзину/ })
page.getByLabel('Email')
page.getByTestId('add-to-cart-btn')    // если нет роли/лейбла

// ❌ Хрупко — не использовать
page.locator('.btn-primary')           // CSS-класс
page.locator('div:nth-child(3) span')  // структура DOM
```

### Добавление нового Page Object

1. Добавь методы в существующий класс в `pages/CatalogPage.js`, или
2. Создай новый `<PageName>Page.js` по образцу `CatalogPage.js`
3. Все локаторы — в конструкторе или методах, не в тест-файлах

### Критерии завершения

- [ ] Файл в `tests/e2e/`
- [ ] Используется POM (не голые `page.getByTestId()` в тесте)
- [ ] Шаги оформлены через `test.step()`
- [ ] `await expect(...)` с явным `timeout` для ожидания загрузки
- [ ] Есть happy path и хотя бы один sad path
- [ ] `npm run test:e2e` проходит
- [ ] `npm run test:e2e:headed` визуально корректен

---

## SK-08: Добавление нового API endpoint

**Когда применять:** нужно добавить новый REST-endpoint в бэкенд с тестовым покрытием.

### Шаги

1. **Добавь метод в сервис** (`service/`) с бизнес-логикой
2. **Добавь метод в репозиторий**, если нужен новый запрос к БД
3. **Добавь endpoint в контроллер** (`controller/`)
4. **Напиши unit-тест** для метода сервиса (SK-01)
5. **Напиши black-box тест** для endpoint (SK-03):
   - Успешный ответ с правильным статусом
   - Ответ 404 при несуществующем ID
   - Ответ 400 при невалидном теле
6. **Обнови WireMock-стаб**, если endpoint вызывает внешний сервис (SK-04)
7. **Добавь `data-testid`** в React-компонент, если нужен frontend

### Контрольный список

- [ ] Метод в сервисе с javadoc-комментарием
- [ ] `@Transactional` / `@Transactional(readOnly = true)` на методе сервиса
- [ ] Правильный HTTP-статус: 200, 201 (`@ResponseStatus`), 204
- [ ] Ошибки обрабатываются через `GlobalExceptionHandler` (новых `try-catch` в контроллере не добавляй)
- [ ] Unit + Black-box тесты написаны

---

## SK-09: Добавление бизнес-правила

**Когда применять:** нужно добавить новое правило в существующий сервис.

### Шаги

1. **Определи требование**: что именно должен делать/не делать сервис
2. **Опиши граничные значения**: при каких условиях срабатывает, когда нет
3. **Реализуй в сервисе** с javadoc-комментарием аналогично `applyDiscount()`
4. **Напиши unit-тест** с параметризованными case-ами для всех границ
5. **Напиши component-тест** если правило затрагивает транзакцию или несколько сущностей

### Критерии завершения

- [ ] Javadoc с описанием правил и граничных значений
- [ ] Параметризованный unit-тест (`@MethodSource`) для граничных значений
- [ ] Негативный тест с `assertThatThrownBy()`
- [ ] `mvn test -P unit` и `mvn test -P component` проходят

---

## SK-10: Debugging упавшего теста

**Когда применять:** тест падает и нужно найти причину.

### Алгоритм

1. **Прочитай сообщение об ошибке** — обычно там есть `AssertionError` с expected/actual
2. **Определи уровень** по `@Tag`:
   - `unit` → проверь настройку моков (`when(...).thenReturn(...)`)
   - `component` → проверь профиль `test` и наличие `@MockBean` для HTTP
   - `blackbox` → проверь запущен ли Docker (`docker ps`)
   - `integration` → проверь `wireMock.resetAll()` в `@BeforeEach`
3. **Для Playwright**: запусти `npm run test:e2e:headed` и смотри браузер; открой Trace Viewer:
   ```bash
   npx playwright show-trace test-results/<имя>/trace.zip
   ```
4. **Для Testcontainers**: проверь логи контейнера — иногда PostgreSQL не успел стартовать
5. **Для WireMock**: добавь `wireMock.verify(...)` чтобы понять, был ли запрос

### Частые причины падений

| Симптом | Вероятная причина | Решение |
|---------|------------------|---------|
| `NullPointerException` в сервисе | Мок не настроен | Добавь `when(...).thenReturn(...)` |
| `DataSource` ошибка в component | Нет `@ActiveProfiles("test")` | Добавь аннотацию |
| `No bean found` в component | Нет `@MockBean` для HTTP-клиента | Добавь `@MockBean` |
| `Connection refused` в blackbox | Docker не запущен | `docker ps`, запусти Docker |
| `Unexpected request` в WireMock | Не описан стаб | Добавь `stubFor()` для URL |
| Playwright: `timeout exceeded` | Элемент не появился | Проверь `data-testid`, увеличь таймаут |
| MSW: `Unhandled request` | Не задан хендлер | Добавь `http.get('/api/...')` в `setupServer` |

### Критерии завершения

- [ ] Тест проходит без изменения его логики (если был корректен)
- [ ] Если тест выявил баг — баг исправлен в production-коде
- [ ] `mvn test -P <level>` или `npm test` / `npm run test:e2e` проходит полностью

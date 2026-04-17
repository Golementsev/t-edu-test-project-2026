# 🔗 Уровень 3: Интеграционные тесты

## Black-box vs Integration

На этом материале разберём два подхода:

| | **Black-box** | **Integration** |
|--|-----|-----|
| **Что тестируем** | HTTP API как «чёрный ящик» | Взаимодействие с внешними системами |
| **БД** | Реальная PostgreSQL (в Docker) | Реальная PostgreSQL (в Docker) |
| **Внешние API** | @MockBean / WireMock | WireMock (детально) |
| **Технология** | Testcontainers + RestAssured | WireMock + RestAssured |

---

## Testcontainers — Docker в тестах

**Идея:** Запускаем настоящий PostgreSQL в Docker-контейнере прямо в процессе тестов.

```java
@Testcontainers                         // ← активирует поддержку
class BookApiBlackBoxTest {

    // Объявляем контейнер — запустится до тестов
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bookstore_test")
            .withUsername("test")
            .withPassword("test");

    // Передаём URL контейнера в Spring ДИНАМИЧЕСКИ
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Что происходит при запуске:
```
1. JUnit запускает тест
2. @Container → Testcontainers дёргает Docker API
3. Docker запускает postgres:15-alpine на случайном порту
4. @DynamicPropertySource → Spring получает URL контейнера
5. Spring Boot запускается, подключается к контейнеру
6. Тесты выполняются
7. Контейнер останавливается и удаляется автоматически
```

---

## RestAssured — DSL для HTTP тестов

RestAssured — библиотека для удобного написания HTTP-запросов в тестах.

```java
// Синтаксис: given → when → then
given()
    .contentType(ContentType.JSON)
    .body("""
        {
          "title": "Чистый код",
          "isbn": "978-0132350884",
          "price": 750.00,
          "stockQuantity": 10,
          "genre": "PROGRAMMING"
        }
        """)
.when()
    .post("/api/books")
.then()
    .statusCode(201)              // HTTP-статус
    .body("id", notNullValue())   // тело ответа (JSONPath)
    .body("title", equalTo("Чистый код"))
    .body("price", equalTo(750.00F));
```

---

## WireMock — сервер-заглушка для HTTP

WireMock запускает настоящий HTTP-сервер на локальном порту.  
Наш BookStore думает, что обращается к реальному Google Books API — но на самом деле к WireMock.

```java
@RegisterExtension
static WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())  // случайный порт
    .build();

// Перенаправляем ISBN-клиент на WireMock:
@DynamicPropertySource
static void configure(DynamicPropertyRegistry registry) {
    registry.add("isbn.service.url", wireMock::baseUrl);
}
```

### Настройка ответов (стабы):

```java
// Успешный ответ
wireMock.stubFor(get(urlPathMatching("/volumes.*"))
    .withQueryParam("q", containing("978-0132350884"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {"totalItems": 1, "items": [{"volumeInfo": {"title": "Clean Code"}}]}
            """)));

// Ошибка 503
wireMock.stubFor(get(urlPathMatching("/volumes.*"))
    .willReturn(aResponse().withStatus(503)));

// Таймаут 10 секунд
wireMock.stubFor(get(urlPathMatching("/volumes.*"))
    .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(10_000)));
```

### Проверка запросов:

```java
// Убеждаемся, что запрос к ISBN-сервису был сделан ровно 1 раз
wireMock.verify(1, getRequestedFor(
    urlPathMatching("/volumes.*"))
    .withQueryParam("q", containing("978-0132350884")));
```

---

## Статический vs Динамический стенд

### Статический стенд
```
Разработчик 1 → тест → общий стенд
Разработчик 2 → тест → общий стенд  ← конфликт данных!
CI/CD pipeline → тест → общий стенд ← тест упал из-за чужих данных!
```
- Один стенд на всех
- Данные накапливаются, тесты мешают друг другу
- Дешевле в обслуживании

### Динамический стенд
```
PR #1 → [автоматически поднимается стенд 1] → тесты → стенд удалён
PR #2 → [автоматически поднимается стенд 2] → тесты → стенд удалён
```
- Каждый пайплайн получает свой изолированный стенд
- Нет конфликтов данных
- Testcontainers — это динамический стенд в рамках одной JVM!

---

## Смотри код

**Testcontainers Black-box:** [`blackbox/BookApiBlackBoxTest.java`](../backend/src/test/java/ru/qaschool/bookstore/blackbox/BookApiBlackBoxTest.java)  
**WireMock Integration:** [`integration/IsbnIntegrationTest.java`](../backend/src/test/java/ru/qaschool/bookstore/integration/IsbnIntegrationTest.java)

---

## Запуск

```bash
# Black-box (нужен Docker!)
mvn test -P blackbox

# Integration (нужен Docker!)
mvn test -P integration
```

---

## Вопросы для обсуждения

1. Почему WireMock лучше `@MockBean` для тестирования HTTP-интеграций?
2. Что произойдёт, если Docker не запущен при старте Testcontainers?
3. Как бы вы организовали тестовые данные на статическом стенде?
4. Graceful degradation: почему наш сервис не падает при ошибке ISBN-API?

---

## ➡️ Следующий уровень

[04_system_tests.md](04_system_tests.md) — Системные тесты и E2E с Playwright

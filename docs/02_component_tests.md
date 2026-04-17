# 🧩 Уровень 2: Компонентные тесты

## Что такое компонентный тест?

Компонентный тест проверяет **взаимодействие нескольких компонентов** системы.  
В отличие от unit-теста — здесь мы поднимаем Spring-контекст и используем реальную (in-memory) БД.

```
Unit-тест:        [BookService] + [МОК Repository]
                   ↑ всё в рамках JVM, без Spring

Компонентный:     [BookService] + [BookRepository] + [H2 DB]
                   ↑ Spring поднят, HTTP-клиенты замокированы
```

---

## @SpringBootTest — поднимаем контекст Spring

Аннотация `@SpringBootTest` запускает **весь** Spring-контекст: все сервисы, репозитории, конфигурации.

```java
@SpringBootTest          // ← поднимаем контекст!
@ActiveProfiles("test")  // ← используем H2 вместо PostgreSQL
@Transactional           // ← каждый тест в транзакции → автоматический rollback
class OrderServiceComponentTest {

    @Autowired
    OrderService orderService;  // ← реальный бин из контекста!

    @Autowired
    BookRepository bookRepository;  // ← тоже реальный

    @MockBean
    NotificationClient notificationClient;  // ← HTTP-клиент → мок!
}
```

---

## H2 vs PostgreSQL

В компонентных тестах мы используем **H2** — встроенную in-memory БД.

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop  # таблицы создаются перед тестами, удаляются после
```

**Плюсы H2:**
- Не нужен Docker
- Создаётся моментально
- Изолирована от production БД

**Минусы H2:**
- Не полностью совместима с PostgreSQL
- Некоторые SQL-запросы могут работать по-разному
- Для проверки PostgreSQL-специфики → Testcontainers (следующий уровень)

---

## @DataJpaTest — «срез» контекста

Иногда не нужен весь контекст Spring.  
`@DataJpaTest` поднимает **только JPA-слой**: репозитории, EntityManager.  
Контроллеры и сервисы — не загружаются.

```java
@DataJpaTest          // ← только JPA! Быстрее чем @SpringBootTest
@ActiveProfiles("test")
class BookRepositorySliceTest {

    @Autowired
    BookRepository bookRepository;  // ← работает!

    // @Autowired BookService bookService; ← НЕ работает (бин не создан)
}
```

**Когда использовать:**
- Проверка кастомных JPQL-запросов
- Проверка ограничений уникальности в БД
- Проверка пагинации и сортировки

---

## @Transactional в тестах

Когда класс теста помечен `@Transactional`:
- Каждый тест выполняется в транзакции
- **Транзакция автоматически откатывается** после теста
- База данных остаётся чистой для следующего теста

```java
@Test
@Transactional  // или на классе целиком
void shouldCreateOrder() {
    orderService.placeOrder("test@mail.ru", ...);  // сохраняется в H2
    // ... проверки ...
    // ← здесь транзакция откатывается!
    // Следующий тест получает чистую БД
}
```

**Осторожно:** Если метод сервиса сам помечен `REQUIRES_NEW` — откат не сработает.

---

## @MockBean — замена бинов в контексте

```java
// ← Заменяет реальный NotificationClient в Spring-контексте на Mockito-мок
@MockBean
NotificationClient notificationClient;

// Настраиваем поведение как обычный Mock:
doNothing().when(notificationClient)
    .sendOrderConfirmation(any(), anyLong());
```

**Разница @Mock vs @MockBean:**
- `@Mock` — только для Mockito, никакого Spring
- `@MockBean` — создаёт мок И регистрирует его как бин в Spring-контексте

---

## Frontend: @testing-library/react + MSW

В React-тестах схожая идея:

```jsx
// Мокируем API-запросы через MSW (Mock Service Worker)
const server = setupServer(
  http.get('/api/books', () => HttpResponse.json(BOOKS))
)

// Тест рендерит реальный компонент, который делает реальный axios-запрос
// MSW перехватывает его и возвращает наши данные
it('показывает список книг', async () => {
  render(<BookList />)
  await waitFor(() => screen.getByTestId('books-grid'))
  expect(screen.getAllByTestId('book-card')).toHaveLength(3)
})
```

---

## Смотри код

**Java (OrderService):** [`component/OrderServiceComponentTest.java`](../backend/src/test/java/ru/qaschool/bookstore/component/OrderServiceComponentTest.java)  
**Java (Repository slice):** [`component/BookRepositorySliceTest.java`](../backend/src/test/java/ru/qaschool/bookstore/component/BookRepositorySliceTest.java)  
**React + MSW:** [`__tests__/component/BookList.test.jsx`](../frontend/src/__tests__/component/BookList.test.jsx)

---

## Запуск

```bash
cd backend
mvn test -P component      # H2 in-memory, Spring context
```

---

## Вопросы для обсуждения

1. Почему использование H2 в тестах может быть опасным?
2. Когда `@DataJpaTest` лучше `@SpringBootTest`?
3. Зачем `@Transactional` в тестах, если в production тоже транзакции?
4. Что будет, если забыть `@MockBean` для `NotificationClient`? Попробуй!

---

## ➡️ Следующий уровень

[03_integration_tests.md](03_integration_tests.md) — Black-box тесты и WireMock

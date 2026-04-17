# 🧱 Уровень 1: Unit-тесты

## Что такое unit-тест?

**Unit** (единица) — это один метод или функция.  
**Unit-тест** — тест, который проверяет **одну единицу** кода изолированно.

### Главные принципы
- **Никакого Spring** (не поднимаем Spring-контекст)
- **Никаких БД** (не обращаемся к базе данных)
- **Никакого HTTP** (не делаем реальных сетевых запросов)
- **Все зависимости** — заменены **моками**
- **Скорость** — миллисекунды

---

## Мок (Mock) — что это?

Представь: метод `BookService.findById()` обращается к базе данных.  
Но мы не хотим поднимать БД для unit-теста!

**Решение:** Создать **фиктивный объект** (мок), который выглядит как `BookRepository`, но на самом деле просто возвращает данные, которые мы задали.

```java
// Вместо реального репозитория, который идёт в БД:
@Mock
BookRepository bookRepository;

// Настраиваем поведение мока:
when(bookRepository.findById(1L))
    .thenReturn(Optional.of(book)); // "если спросят id=1, верни эту книгу"
```

---

## Структура теста: AAA

Каждый тест состоит из трёх частей:

```java
@Test
void shouldApplyDiscount() {
    // ARRANGE — подготовка данных
    BigDecimal price = new BigDecimal("100.00");
    int discount = 10;

    // ACT — вызов тестируемого метода
    BigDecimal result = bookService.applyDiscount(price, discount);

    // ASSERT — проверка результата
    assertThat(result).isEqualByComparingTo("90.00");
}
```

---

## Технологии

### JUnit 5
Фреймворк для написания и запуска тестов в Java.

```java
@Test                              // это тест
@DisplayName("Понятное название")  // отображается в отчёте
void someTest() { ... }

@BeforeEach                        // выполняется ДО каждого теста
void setUp() { ... }

@Nested                            // группировка тестов в классе
class WhenOutOfStock { ... }
```

### Mockito
Библиотека для создания моков.

```java
@Mock BookRepository repo;         // создаёт мок
@InjectMocks BookService service;  // вставляет моки в сервис

when(repo.findById(1L))            // условие
    .thenReturn(Optional.of(book)); // ответ мока

verify(repo, times(1)).findById(1L); // проверяем, что метод был вызван
verify(repo, never()).save(any());   // убеждаемся, что save НЕ вызывался
```

### AssertJ
Читаемые проверки результатов.

```java
// Стандартный JUnit — не очень читаемо:
assertEquals("Чистый код", book.getTitle());

// AssertJ — цепочки методов, много проверок:
assertThat(book)
    .extracting(Book::getTitle)
    .isEqualTo("Чистый код");

assertThat(result)
    .isNotNull()
    .isInstanceOf(BigDecimal.class)
    .isEqualByComparingTo("90.00");

assertThatThrownBy(() -> service.reserveStock(1L, 99))
    .isInstanceOf(OutOfStockException.class)
    .hasMessageContaining("99");
```

---

## Параметризованные тесты

Одна логика — много наборов данных. Избегает копипасты.

```java
@ParameterizedTest(name = "Скидка {1}% от {0}₽ → {2}₽")
@MethodSource("discountCases")
void shouldApplyDiscountCorrectly(String price, int discount, String expected) {
    assertThat(bookService.applyDiscount(new BigDecimal(price), discount))
        .isEqualByComparingTo(expected);
}

static Stream<Arguments> discountCases() {
    return Stream.of(
        Arguments.of("100.00", 10, "90.00"),
        Arguments.of("100.00", 25, "75.00"),
        Arguments.of("100.00", 50, "50.00")
    );
}
```

---

## Смотри код

**Java:** [`backend/src/test/java/.../unit/BookServiceUnitTest.java`](../backend/src/test/java/ru/qaschool/bookstore/unit/BookServiceUnitTest.java)  
**JavaScript:** [`frontend/src/__tests__/unit/priceUtils.test.js`](../frontend/src/__tests__/unit/priceUtils.test.js)

---

## Запуск

```bash
# Backend — только unit-тесты (быстро!)
cd backend
mvn test -P unit

# Frontend
cd frontend
npm test  # запустит все unit и component тесты
```

---

## Вопросы для обсуждения

1. Почему unit-тесты не проверяют, что данные правильно сохраняются в БД?
2. Что произойдёт, если мы не настроим mock, а вызовем `bookRepository.findById()`?
3. Зачем писать тест на `applyDiscount(-5)`? Разве такое бывает?
4. В чём разница между `@Mock` и `@Spy`?

---

## ➡️ Следующий уровень

[02_component_tests.md](02_component_tests.md) — Компонентные тесты (со Spring)

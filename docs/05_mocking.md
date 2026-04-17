# 🎭 Мокирование на разных уровнях пирамиды

## Зачем мокировать?

В реальном приложении сервис зависит от:
- Базы данных
- Внешних HTTP-сервисов (Google API, платёжная система, email)
- Других микросервисов

При тестировании мы хотим:
1. **Изолировать** тестируемый код от зависимостей
2. **Контролировать** ответы зависимостей (успех, ошибка, таймаут)
3. **Ускорить** тесты (H2 быстрее PostgreSQL, мок быстрее реального API)
4. **Тестировать** ситуации, которые сложно воспроизвести (сеть упала)

---

## Карта мокирования по уровням

```
Уровень          БД                    Внешние HTTP
─────────────────────────────────────────────────────
Unit             ❌ @Mock               ❌ @Mock (Mockito)
Component        ✅ H2 (real)           ❌ @MockBean (Mockito)
Black-box        ✅ PostgreSQL (TC)     ❌ @MockBean / WireMock
Integration      ✅ PostgreSQL (TC)     ✅ WireMock (stub server)
E2E / System     ✅ PostgreSQL (live)   ✅ WireMock / real service
```

---

## 1. Mockito — моки для Java

### @Mock — мок зависимости
```java
@Mock
BookRepository bookRepository;  // из интерфейса создаётся фиктивная реализация

// Любой вызов возвращает null/0/empty по умолчанию
// Нужно настроить явно:
when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
```

### @Spy — шпион (реальный объект + наблюдение)
```java
@Spy
BookService bookService = new BookService(...);

// Реальный метод вызывается, но вы можете перехватить:
doReturn(BigDecimal.ZERO).when(bookService).applyDiscount(any(), eq(100));
```

### @MockBean — мок в Spring-контексте
```java
// В классе с @SpringBootTest:
@MockBean
NotificationClient notificationClient;
// Spring заменяет реальный бин на мок
```

### Проверка вызовов
```java
// Метод был вызван ровно 1 раз
verify(notificationClient, times(1)).sendOrderConfirmation(any(), anyLong());

// Метод НЕ вызывался
verify(bookRepository, never()).save(any());

// Метод был вызван с конкретными аргументами
verify(notificationClient).sendOrderConfirmation(
    eq("user@email.ru"),   // первый аргумент — конкретный
    anyLong()              // второй — любой Long
);
```

---

## 2. WireMock — мок внешнего HTTP-сервиса

WireMock запускает настоящий HTTP-сервер.

### Сценарии для тестирования:

```java
// ✅ Успешный ответ
wireMock.stubFor(get(urlPathMatching("/api/books.*"))
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{...}")));

// ❌ Ошибка сервера
wireMock.stubFor(get(anyUrl())
    .willReturn(aResponse().withStatus(503)));

// ⏱️ Таймаут
wireMock.stubFor(get(anyUrl())
    .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(30_000)));  // 30 секунд → таймаут клиента

// 🔄 Первый вызов — ошибка, второй — успех (тест retry-логики)
wireMock.stubFor(get(anyUrl())
    .inScenario("retry-test")
    .whenScenarioStateIs(STARTED)
    .willReturn(serverError())
    .willSetStateTo("error-returned"));

wireMock.stubFor(get(anyUrl())
    .inScenario("retry-test")
    .whenScenarioStateIs("error-returned")
    .willReturn(ok().withBody("success")));
```

---

## 3. MSW — мок HTTP на фронтенде

MSW (Mock Service Worker) перехватывает fetch/axios-запросы в тестах Node.js.

```javascript
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

const server = setupServer(
  // Успешный ответ
  http.get('/api/books', () => HttpResponse.json(BOOKS)),

  // Имитация ошибки
  http.get('/api/books', () => HttpResponse.error()),

  // С задержкой
  http.get('/api/books', async () => {
    await new Promise(r => setTimeout(r, 2000))
    return HttpResponse.json(BOOKS)
  }),

  // POST — создание
  http.post('/api/orders', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ id: 1, status: 'NEW' }, { status: 201 })
  })
)
```

---

## 4. Мокирование в E2E (Playwright)

Playwright может перехватывать сетевые запросы в браузере:

```javascript
// Замокировать API-запрос в браузере
await page.route('/api/books', async route => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([
      { id: 1, title: 'Тестовая книга', price: 100, stockQuantity: 5 }
    ])
  })
})

await page.goto('/')
// Теперь компонент получит наши мок-данные, а не реальный API
```

Это позволяет тестировать фронтенд **без поднятого бэкенда**!

---

## Сравнение инструментов

| Инструмент | Уровень | Язык | Что мокирует |
|-----------|---------|------|-------------|
| Mockito `@Mock` | Unit | Java | Java-интерфейсы/классы |
| Mockito `@MockBean` | Component | Java | Spring-бины |
| WireMock | Integration | Java | HTTP-сервисы |
| MSW | Component/E2E | JS | HTTP в Node.js/браузере |
| Playwright `route()` | E2E | JS | HTTP в браузере |

---

## Антипаттерн: «слишком много моков»

```java
// ❌ Плохо: мокируем практически всё
@Test
void testOrderCreation() {
    when(orderRepository.save(any())).thenReturn(order);
    when(bookRepository.findById(any())).thenReturn(Optional.of(book));
    when(bookRepository.save(any())).thenReturn(book);
    when(notificationClient.send(any())).thenReturn(null);
    // Теперь тест не проверяет реальную логику...
}

// ✅ Лучше: на этом уровне используй реальную H2-базу
// Мокируй только внешние HTTP-зависимости
@MockBean NotificationClient notificationClient;
// Остальное — реально!
```

---

## Вопросы для обсуждения

1. Может ли тест с множеством моков дать ложное ощущение надёжности?
2. Зачем WireMock, если можно сделать `@MockBean`?
3. Что тяжелее поддерживать: много моков или реальная инфра?
4. Как тестировать retry-логику? Как заставить мок отвечать по-разному?

---

## ➡️ Следующий раздел

[06_allure_tms.md](06_allure_tms.md) — Allure и интеграция с TMS

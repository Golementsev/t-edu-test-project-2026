package ru.qaschool.bookstore.blackbox;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.qaschool.bookstore.service.IsbnLookupClient;
import ru.qaschool.bookstore.service.NotificationClient;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ УРОВЕНЬ 3: BLACK-BOX ТЕСТЫ (Testcontainers) ◀◀
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что такое black-box тест?
 * ──────────────────────────
 * • Тестирует приложение как «чёрный ящик» через HTTP API
 * • Приложение запущено полностью (все слои)
 * • БД — РЕАЛЬНАЯ PostgreSQL, запускается в Docker-контейнере
 * • Внешние HTTP-сервисы всё ещё замокированы (@MockBean)
 *   (или заменяются WireMock-контейнером — см. интеграционный уровень)
 *
 * Технология: Testcontainers
 * ────────────────────────────
 * Testcontainers — Java-библиотека для запуска Docker-контейнеров в тестах.
 * • Контейнер с PostgreSQL запускается автоматически
 * • После тестов контейнер останавливается и удаляется
 * • Нет никакой ручной настройки окружения!
 *
 * Требования: Docker должен быть запущен на машине.
 *
 * Отличие от компонентных тестов:
 * ──────────────────────────────────
 * comp  → сервисный слой, H2 in-memory, Java вызовы
 * black → HTTP API, реальный PostgreSQL, RestAssured
 */
@Tag("blackbox")                           // mvn test -P blackbox
@Epic("API Книжного магазина")
@Feature("CRUD-операции через REST API")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers                            // ← активирует поддержку контейнеров
@ActiveProfiles("test")
class BookApiBlackBoxTest {

    // ─── DOCKER-КОНТЕЙНЕР С PostgreSQL ──────────────────────────────────
    // @Container создаёт и запускает контейнер перед тестами
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bookstore_test")
            .withUsername("test")
            .withPassword("test");

    // ── Динамически передаём URL контейнера в Spring ─────────────────────
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @MockBean
    private NotificationClient notificationClient;
    @MockBean
    private IsbnLookupClient isbnLookupClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Story("Создание книги через API")
    @Layer("blackbox")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /api/books — создаёт книгу и возвращает 201")
    void shouldCreateBook_andReturn201() {
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
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Чистый код"))
                .body("isbn", equalTo("978-0132350884"))
                .body("price", equalTo(750.00F));
    }

    @Test
    @Story("Получение несуществующей книги")
    @Layer("blackbox")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/books/999 — возвращает 404")
    void shouldReturn404_whenBookNotFound() {
        given()
        .when()
                .get("/api/books/999999")
        .then()
                .statusCode(404)
                .body("detail", containsString("999999"));
    }

    @Test
    @Story("Валидация ISBN при создании книги")
    @Layer("blackbox")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/books с неверным ISBN — возвращает 400")
    void shouldReturn400_whenIsbnFormatInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Книга без ISBN",
                          "isbn": "INVALID-ISBN",
                          "price": 100.00,
                          "stockQuantity": 1,
                          "genre": "FICTION"
                        }
                        """)
        .when()
                .post("/api/books")
        .then()
                .statusCode(400)
                .body("errors.isbn", notNullValue());
    }

    @Test
    @Story("Полный CRUD-флоу книги")
    @Layer("blackbox")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Создание → обновление → удаление книги")
    void shouldCreateUpdateAndDeleteBook() {
        // Step 1: создаём книгу
        Integer bookId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Временная книга",
                          "isbn": "978-9991234567",
                          "price": 199.99,
                          "stockQuantity": 3,
                          "genre": "FICTION"
                        }
                        """)
        .when().post("/api/books")
        .then().statusCode(201)
                .extract().path("id");

        // Step 2: обновляем цену
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Временная книга",
                          "isbn": "978-9991234567",
                          "price": 249.99,
                          "stockQuantity": 3,
                          "genre": "FICTION"
                        }
                        """)
        .when().put("/api/books/" + bookId)
        .then().statusCode(200)
                .body("price", equalTo(249.99F));

        // Step 3: удаляем
        given()
        .when().delete("/api/books/" + bookId)
        .then().statusCode(204);

        // Step 4: убеждаемся, что удалена
        given()
        .when().get("/api/books/" + bookId)
        .then().statusCode(404);
    }
}

package ru.qaschool.bookstore.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.qameta.allure.*;
import ru.qaschool.bookstore.annotation.Layer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.qaschool.bookstore.service.NotificationClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ УРОВЕНЬ 4: ИНТЕГРАЦИОННЫЕ ТЕСТЫ ◀◀
 *     (Integration Tests с WireMock)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что тестируем?
 * ──────────────────
 * Интеграцию нашего сервиса с ВНЕШНИМИ системами:
 * • Как наш сервис реагирует, если ISBN-API недоступен?
 * • Что произойдёт, если ISBN-API вернул некорректный ответ?
 * • Как обрабатываются таймауты внешних сервисов?
 *
 * Технология: WireMock
 * ─────────────────────
 * WireMock — сервер-заглушка (stub server), который:
 * • Запускается локально на случайном порту
 * • "Симулирует" внешний HTTP-сервис
 * • Позволяет настроить любой ответ: 200, 500, таймаут, редирект
 * • Проверяет, что нужные запросы были сделаны
 *
 * Стенды для интеграционных тестов:
 * ────────────────────────────────────
 * СТАТИЧЕСКИЙ стенд:
 *   • Один общий стенд для всей команды
 *   • Данные меняются вручную / не меняются
 *   • Минус: тесты могут мешать друг другу
 *
 * ДИНАМИЧЕСКИЙ стенд (как у нас):
 *   • Стенд поднимается для каждого запуска (PR, pipeline)
 *   • Полная изоляция между запусками
 *   • Плюс: нет «флаки»-тестов из-за чужих данных
 */
@Tag("integration")                        // mvn test -P integration
@Epic("Интеграция с внешними сервисами")
@Feature("ISBN Lookup Service")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class IsbnIntegrationTest {

    // ── КОНТЕЙНЕР с БД ──────────────────────────────────────────────────
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bookstore_integration")
            .withUsername("test").withPassword("test");

    // ── WIREMOCK — запускает HTTP-сервер на случайном порту ─────────────
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())     // случайный порт
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Перенаправляем ISBN-клиент на WireMock вместо реального Google API!
        registry.add("isbn.service.url", wireMock::baseUrl);
    }

    @LocalServerPort
    private int port;

    @MockBean
    private NotificationClient notificationClient; // уведомления не нужны

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        wireMock.resetAll(); // сбрасываем все стабы перед каждым тестом
    }

    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Story("ISBN-сервис вернул описание книги")
    @Layer("integration")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("enrichFromIsbnService обогащает книгу при успешном ответе")
    void shouldEnrichBook_whenIsbnServiceReturnsData() {
        // ARRANGE: сначала создаём книгу в нашем API
        Integer bookId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title":"Война и мир","isbn":"978-5170922673",
                         "price":500.00,"stockQuantity":5,"genre":"FICTION"}
                        """)
                .post("/api/books")
                .then().statusCode(201)
                .extract().path("id");

        // ARRANGE: настраиваем WireMock — «внешний» ISBN-сервис ответит 200
        wireMock.stubFor(get(urlPathMatching("/volumes.*"))
                .withQueryParam("q", containing("978-5170922673"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "totalItems": 1,
                                  "items": [{
                                    "volumeInfo": {
                                      "title": "War and Peace",
                                      "description": "Classic Russian novel"
                                    }
                                  }]
                                }
                                """)));

        // ACT: вызываем обогащение (наш сервис идёт в WireMock)
        given()
        .when()
                .post("/api/books/" + bookId + "/enrich")
        .then()
                .statusCode(200);

        // VERIFY: проверяем, что WireMock получил ожидаемый запрос
        wireMock.verify(1, getRequestedFor(
                urlPathMatching("/volumes.*"))
                .withQueryParam("q", containing("978-5170922673")));
    }

    @Test
    @Story("ISBN-сервис вернул 503 — приложение не упало")
    @Layer("integration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Сервис работает корректно даже при недоступном ISBN API (503)")
    void shouldHandleGracefully_whenIsbnServiceUnavailable() {
        // ARRANGE: ISBN-сервис недоступен
        wireMock.stubFor(get(urlPathMatching("/volumes.*"))
                .willReturn(aResponse().withStatus(503)));

        Integer bookId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title":"Мастер и Маргарита","isbn":"978-5170101231",
                         "price":350.00,"stockQuantity":8,"genre":"FICTION"}
                        """)
                .post("/api/books")
                .then().statusCode(201)
                .extract().path("id");

        // ACT: запрашиваем обогащение — должно завершиться без ошибки
        // (NotificationClient при ошибке логирует, но не бросает исключение)
        given()
        .when()
                .post("/api/books/" + bookId + "/enrich")
        .then()
                .statusCode(200); // сервис устойчив к ошибкам внешних систем
    }

    @Test
    @Story("ISBN-сервис вернул таймаут — приложение не упало")
    @Layer("integration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Graceful degradation при таймауте ISBN API")
    void shouldHandleTimeout_whenIsbnServiceTimesOut() {
        // ARRANGE: симулируем задержку 10 секунд
        wireMock.stubFor(get(urlPathMatching("/volumes.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10_000))); // 10 секунд задержки

        Integer bookId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title":"Преступление и наказание","isbn":"978-5170108992",
                         "price":299.00,"stockQuantity":3,"genre":"FICTION"}
                        """)
                .post("/api/books")
                .then().statusCode(201)
                .extract().path("id");

        // ACT: enrich должен завершиться (с таймаутом клиента), но не упасть с 500
        // В реальном приложении RestTemplate настраивается с timeout
        given()
        .when()
                .post("/api/books/" + bookId + "/enrich")
        .then()
                .statusCode(anyOf(equalTo(200), equalTo(503)));
    }
}

package ru.qaschool.bookstore.component;

import io.qameta.allure.*;
import ru.qaschool.bookstore.annotation.Layer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.qaschool.bookstore.domain.Book;
import ru.qaschool.bookstore.domain.Order;
import ru.qaschool.bookstore.exception.OutOfStockException;
import ru.qaschool.bookstore.repository.BookRepository;
import ru.qaschool.bookstore.repository.OrderRepository;
import ru.qaschool.bookstore.service.IsbnLookupClient;
import ru.qaschool.bookstore.service.NotificationClient;
import ru.qaschool.bookstore.service.OrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ УРОВЕНЬ 2: КОМПОНЕНТНЫЕ ТЕСТЫ ◀◀
 *     (Component Tests / Spring Integration Test)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что такое компонентный тест?
 * ──────────────────────────────
 * • Поднимает реальный Spring-контекст (@SpringBootTest)
 * • Использует реальную БД (in-memory H2 вместо PostgreSQL)
 * • ВНЕШНИЕ сервисы, которые вызывают HTTP → мокируются (@MockBean)
 * • Тестирует ВЗАИМОДЕЙСТВИЕ компонентов: Service + Repository + DB
 * • Медленнее unit-тестов (секунды), но охватывает больше
 *
 * Отличие от unit:
 * ─────────────────
 * unit  → только BookService, все зависимости — моки
 * comp  → BookService + BookRepository + реальная H2 БД
 *         только HTTP-клиенты замокированы (@MockBean)
 *
 * Профиль: test (application-test.yml → H2 in-memory)
 */
@Tag("component")                          // mvn test -P component
@Epic("Управление заказами")
@Feature("Оформление заказа с БД")
@SpringBootTest                            // ← поднимаем Spring-контекст!
@ActiveProfiles("test")                    // ← используем H2 вместо PostgreSQL
@Transactional                             // ← каждый тест в транзакции → rollback
class OrderServiceComponentTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderRepository orderRepository;

    // ── @MockBean заменяет бин в Spring-контексте на мок ───────────────
    @MockBean
    private NotificationClient notificationClient;   // HTTP → не нужен

    @MockBean
    private IsbnLookupClient isbnLookupClient;       // HTTP → не нужен

    // ── Тестовые данные ─────────────────────────────────────────────────
    private Book cleanCode;
    private Book designPatterns;

    @BeforeEach
    void setUp() {
        // Сохраняем реальные записи в H2 через JPA
        cleanCode = bookRepository.save(Book.builder()
                .title("Чистый код").isbn("978-0132350884")
                .price(new BigDecimal("750.00")).stockQuantity(10)
                .genre(Book.Genre.PROGRAMMING).build());

        designPatterns = bookRepository.save(Book.builder()
                .title("Паттерны проектирования").isbn("978-0201633610")
                .price(new BigDecimal("900.00")).stockQuantity(2)
                .genre(Book.Genre.PROGRAMMING).build());
    }

    // ═══════════════════════════════════════════════════════════════════

    @Test
    @Story("Успешное оформление заказа")
    @Layer("component")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Заказ создаётся, остаток уменьшается, уведомление отправляется")
    void shouldCreateOrder_andDecrementStock_andSendNotification() {
        // ARRANGE
        doNothing().when(notificationClient)
                .sendOrderConfirmation(any(), anyLong());

        // ACT — реальный вызов через Spring, с реальной H2
        var items = List.of(new OrderService.OrderItemRequest(cleanCode.getId(), 2));
        Order order = orderService.placeOrder("student@school.ru", items);

        // ASSERT
        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.NEW);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("1500.00"); // 750 * 2

        // Проверяем, что остаток реально уменьшился в БД
        Book updatedBook = bookRepository.findById(cleanCode.getId()).orElseThrow();
        assertThat(updatedBook.getStockQuantity()).isEqualTo(8); // 10 - 2

        // Проверяем, что уведомление было отправлено
        verify(notificationClient, times(1))
                .sendOrderConfirmation(eq("student@school.ru"), eq(order.getId()));
    }

    @Test
    @Story("Нехватка товара при оформлении заказа")
    @Layer("component")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("OutOfStockException и откат транзакции при нехватке")
    void shouldThrowOutOfStock_andRollbackTransaction() {
        var items = List.of(
                new OrderService.OrderItemRequest(designPatterns.getId(), 5) // есть только 2
        );

        // Ожидаем исключение
        assertThatThrownBy(() ->
                orderService.placeOrder("student@school.ru", items)
        ).isInstanceOf(OutOfStockException.class);

        // Транзакция откатилась → остаток НЕ изменился
        Book unchanged = bookRepository.findById(designPatterns.getId()).orElseThrow();
        assertThat(unchanged.getStockQuantity()).isEqualTo(2);

        // Уведомление НЕ было отправлено
        verify(notificationClient, never()).sendOrderConfirmation(any(), any());
    }

    @Test
    @Story("Отмена заказа возвращает товар на склад")
    @Layer("component")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Отмена заказа: статус CANCELLED, остаток восстанавливается")
    void shouldCancelOrder_andRestoreStock() {
        // ARRANGE: оформяем заказ
        doNothing().when(notificationClient).sendOrderConfirmation(any(), anyLong());
        var items = List.of(new OrderService.OrderItemRequest(cleanCode.getId(), 3));
        Order order = orderService.placeOrder("buyer@email.com", items);

        // Убеждаемся, что остаток уменьшился
        assertThat(bookRepository.findById(cleanCode.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(7); // 10 - 3

        // ACT: отменяем заказ
        Order cancelled = orderService.cancelOrder(order.getId());

        // ASSERT
        assertThat(cancelled.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        // Остаток вернулся
        assertThat(bookRepository.findById(cleanCode.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(10); // вернулось !
    }

    @Test
    @Story("Нельзя отменить доставленный заказ")
    @Layer("component")
    @DisplayName("IllegalStateException при отмене заказа в статусе DELIVERED")
    void shouldThrowException_whenCancellingDeliveredOrder() {
        // ARRANGE
        doNothing().when(notificationClient).sendOrderConfirmation(any(), anyLong());
        var items = List.of(new OrderService.OrderItemRequest(cleanCode.getId(), 1));
        Order order = orderService.placeOrder("buyer@email.com", items);
        orderService.confirmOrder(order.getId());

        // Вручную переводим в DELIVERED через репозиторий (в реальном приложении это делал бы другой сервис)
        // Важно: нельзя просто сделать order.setStatus() — это detached объект;
        // нужно загрузить из БД и сохранить обратно, чтобы cancelOrder увидел актуальный статус
        Order fresh = orderRepository.findById(order.getId()).orElseThrow();
        fresh.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(fresh);
        orderRepository.flush(); // сбрасываем в БД до вызова cancelOrder

        // ACT + ASSERT
        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED");
    }
}

package ru.qaschool.bookstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.qaschool.bookstore.domain.*;
import ru.qaschool.bookstore.exception.BookNotFoundException;
import ru.qaschool.bookstore.exception.OutOfStockException;
import ru.qaschool.bookstore.repository.BookRepository;
import ru.qaschool.bookstore.repository.OrderRepository;

import java.util.List;

/**
 * Сервис оформления заказов.
 *
 * <p>Демонстрирует сложную бизнес-логику, затрагивающую несколько сущностей —
 * хороший кандидат для компонентных тестов с поднятием Spring-контекста
 * и реальной in-memory БД.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final NotificationClient notificationClient;

    /**
     * Создаёт новый заказ для покупателя.
     *
     * <p>Бизнес-правила:
     * <ol>
     *   <li>Каждая книга проверяется на наличие на складе</li>
     *   <li>Цена фиксируется на момент оформления</li>
     *   <li>Остаток на складе уменьшается</li>
     *   <li>После создания отправляется уведомление покупателю</li>
     * </ol>
     */
    @Transactional
    public Order placeOrder(String customerEmail, List<OrderItemRequest> items) {
        log.info("Оформление заказа для: {}", customerEmail);

        Order order = Order.builder()
                .customerEmail(customerEmail)
                .status(Order.OrderStatus.NEW)
                .build();

        for (OrderItemRequest req : items) {
            Book book = bookRepository.findById(req.bookId())
                    .orElseThrow(() -> new BookNotFoundException("Книга id=" + req.bookId()));

            if (book.getStockQuantity() < req.quantity()) {
                throw new OutOfStockException(
                        "Недостаточно '%s': нужно %d, есть %d"
                                .formatted(book.getTitle(), req.quantity(), book.getStockQuantity()));
            }

            book.setStockQuantity(book.getStockQuantity() - req.quantity());
            bookRepository.save(book);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(req.quantity())
                    .price(book.getPrice())   // цена фиксируется
                    .build();
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);

        // Уведомление — внешний вызов (мокируем в тестах)
        notificationClient.sendOrderConfirmation(customerEmail, saved.getId());

        log.info("Заказ #{} создан, сумма: {}", saved.getId(), saved.getTotalAmount());
        return saved;
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = findById(orderId);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findById(orderId);
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Нельзя отменить заказ в статусе: " + order.getStatus());
        }
        // Возвращаем книги на склад
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
            bookRepository.save(book);
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заказ #" + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomer(String email) {
        return orderRepository.findByCustomerEmail(email);
    }

    /** DTO для запроса позиции заказа */
    public record OrderItemRequest(Long bookId, int quantity) {}
}

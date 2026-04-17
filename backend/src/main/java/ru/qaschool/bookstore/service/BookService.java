package ru.qaschool.bookstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.qaschool.bookstore.domain.Book;
import ru.qaschool.bookstore.exception.BookNotFoundException;
import ru.qaschool.bookstore.exception.OutOfStockException;
import ru.qaschool.bookstore.repository.BookRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Сервис управления книгами.
 *
 * <p><b>Примечание для тестировщиков:</b><br>
 * Этот класс содержит бизнес-логику, которую удобно тестировать на UNIT-уровне:
 * метод {@link #applyDiscount} не зависит ни от БД, ни от Spring — его можно
 * тестировать мгновенно, без поднятия контекста.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final IsbnLookupClient isbnLookupClient;

    // =====================================================================
    //  CRUD
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Книга с id=" + id + " не найдена"));
    }

    @Transactional(readOnly = true)
    public Book findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Книга с ISBN=" + isbn + " не найдена"));
    }

    @Transactional
    public Book create(Book book) {
        log.info("Создание книги: {}", book.getTitle());
        validateUniqueIsbn(book.getIsbn(), null);
        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long id, Book updated) {
        Book existing = findById(id);
        validateUniqueIsbn(updated.getIsbn(), id);
        existing.setTitle(updated.getTitle());
        existing.setIsbn(updated.getIsbn());
        existing.setPrice(updated.getPrice());
        existing.setStockQuantity(updated.getStockQuantity());
        existing.setGenre(updated.getGenre());
        return bookRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findById(id);
        bookRepository.delete(book);
        log.info("Книга удалена: id={}", id);
    }

    // =====================================================================
    //  БИЗНЕС-ЛОГИКА — целевая для unit-тестов
    // =====================================================================

    /**
     * Применяет скидку к цене книги.
     *
     * <p>Правила скидок (хорошо подходят для параметризованных тестов):
     * <ul>
     *   <li>0% — скидка не применяется</li>
     *   <li>1–99% — стандартная скидка</li>
     *   <li>100% и более — книга бесплатна (цена = 0)</li>
     *   <li>скидка < 0 — выбрасывает {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @param originalPrice исходная цена
     * @param discountPercent процент скидки (0–100)
     * @return цена после скидки, округлённая до копеек
     */
    public BigDecimal applyDiscount(BigDecimal originalPrice, int discountPercent) {
        if (discountPercent < 0) {
            throw new IllegalArgumentException(
                    "Скидка не может быть отрицательной: " + discountPercent);
        }
        if (discountPercent >= 100) {
            return BigDecimal.ZERO;
        }
        if (discountPercent == 0) {
            return originalPrice;
        }
        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPercent)
                .divide(BigDecimal.valueOf(100));
        return originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Резервирует книгу на складе (уменьшает stockQuantity).
     *
     * @throws OutOfStockException если книги нет в наличии
     */
    @Transactional
    public Book reserveStock(Long bookId, int quantity) {
        Book book = findById(bookId);
        if (book.getStockQuantity() < quantity) {
            throw new OutOfStockException(
                    "Недостаточно книг '%s' на складе: запрошено %d, доступно %d"
                            .formatted(book.getTitle(), quantity, book.getStockQuantity()));
        }
        book.setStockQuantity(book.getStockQuantity() - quantity);
        return bookRepository.save(book);
    }

    /**
     * Обогащает книгу данными из внешнего ISBN-сервиса
     * (демонстрация интеграции с внешней системой).
     */
    @Transactional
    public Book enrichFromIsbnService(Long bookId) {
        Book book = findById(bookId);
        IsbnLookupClient.IsbnInfo info = isbnLookupClient.lookup(book.getIsbn());
        if (info != null && info.description() != null) {
            log.info("Обогащение книги '{}' данными из ISBN-сервиса", book.getTitle());
        }
        return bookRepository.save(book);
    }

    // =====================================================================
    //  Вспомогательные методы
    // =====================================================================

    private void validateUniqueIsbn(String isbn, Long excludeId) {
        bookRepository.findByIsbn(isbn).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Книга с ISBN=" + isbn + " уже существует");
            }
        });
    }
}

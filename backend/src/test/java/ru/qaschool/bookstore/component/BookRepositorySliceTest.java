package ru.qaschool.bookstore.component;

import io.qameta.allure.*;
import ru.qaschool.bookstore.annotation.Layer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.qaschool.bookstore.domain.Book;
import ru.qaschool.bookstore.repository.BookRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ КОМПОНЕНТНЫЙ ТЕСТ: Slice — @DataJpaTest ◀◀
 * ═══════════════════════════════════════════════════════════════════════
 *
 * @DataJpaTest — «срез» контекста Spring:
 * ─────────────────────────────────────────
 * • Поднимает ТОЛЬКО JPA-слой (репозитории, EntityManager)
 * • Web-контроллеры и сервисы НЕ загружаются
 * • Использует H2 in-memory по умолчанию
 * • Каждый тест изолирован (транзакции откатываются)
 *
 * Когда использовать:
 * ─────────────────────
 * Когда нужно протестировать именно репозиторий:
 * кастомные JPQL-запросы, пагинацию, джойны — без поднятия
 * всего контекста приложения.
 */
@Tag("component")
@Epic("Управление книгами")
@Feature("Репозиторный слой")
@DataJpaTest                               // ← только JPA slice!
@ActiveProfiles("test")
class BookRepositorySliceTest {

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.save(Book.builder()
                .title("Чистый код").isbn("978-0132350884")
                .price(new BigDecimal("750.00")).stockQuantity(5)
                .genre(Book.Genre.PROGRAMMING).build());

        bookRepository.save(Book.builder()
                .title("Рефакторинг").isbn("978-0134757599")
                .price(new BigDecimal("850.00")).stockQuantity(0) // нет на складе!
                .genre(Book.Genre.PROGRAMMING).build());

        bookRepository.save(Book.builder()
                .title("Гарри Поттер").isbn("978-5389078338")
                .price(new BigDecimal("400.00")).stockQuantity(20)
                .genre(Book.Genre.FICTION).build());
    }

    @Test
    @Story("Поиск по ISBN")
    @Layer("component")
    @DisplayName("findByIsbn возвращает книгу при совпадении")
    void shouldFindBookByIsbn() {
        Optional<Book> result = bookRepository.findByIsbn("978-0132350884");
        assertThat(result).isPresent()
                .get()
                .extracting(Book::getTitle)
                .isEqualTo("Чистый код");
    }

    @Test
    @Story("Поиск книг в наличии")
    @Layer("component")
    @DisplayName("findAllInStock возвращает только книги с остатком > 0")
    void shouldFindOnlyBooksInStock() {
        List<Book> inStock = bookRepository.findAllInStock();
        assertThat(inStock)
                .hasSize(2)                              // "Чистый код" и "Гарри Поттер"
                .noneMatch(b -> b.getStockQuantity() == 0)  // "Рефакторинг" исключён!
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Чистый код", "Гарри Поттер");
    }

    @Test
    @Story("Поиск по жанру")
    @Layer("component")
    @DisplayName("findByGenre возвращает только книги указанного жанра")
    void shouldFindBooksByGenre() {
        List<Book> programming = bookRepository.findByGenre(Book.Genre.PROGRAMMING);
        assertThat(programming)
                .hasSize(2)
                .allMatch(b -> b.getGenre() == Book.Genre.PROGRAMMING);
    }

    @Test
    @Story("Нарушение уникальности ISBN")
    @Layer("component")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Дублирование ISBN вызывает исключение БД")
    void shouldThrowException_whenDuplicateIsbn() {
        Book duplicate = Book.builder()
                .title("Другая книга").isbn("978-0132350884") // тот же ISBN!
                .price(BigDecimal.TEN).stockQuantity(1)
                .genre(Book.Genre.FICTION).build();

        assertThatThrownBy(() -> {
            bookRepository.saveAndFlush(duplicate);
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException
    }
}

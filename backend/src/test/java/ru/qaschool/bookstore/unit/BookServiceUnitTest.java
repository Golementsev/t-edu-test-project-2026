package ru.qaschool.bookstore.unit;

import io.qameta.allure.*;
import ru.qaschool.bookstore.annotation.Layer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.qaschool.bookstore.domain.Book;
import ru.qaschool.bookstore.exception.BookNotFoundException;
import ru.qaschool.bookstore.exception.OutOfStockException;
import ru.qaschool.bookstore.repository.BookRepository;
import ru.qaschool.bookstore.service.BookService;
import ru.qaschool.bookstore.service.IsbnLookupClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ УРОВЕНЬ 1: UNIT-ТЕСТЫ ◀◀
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что такое unit-тест?
 * ─────────────────────
 * • Тестирует ОДНУ единицу — один класс или один метод
 * • НЕ поднимает Spring-контекст (нет @SpringBootTest)
 * • Все зависимости ЗАМЕНЯЮТСЯ МОКАМИ (Mockito)
 * • Запускается за миллисекунды
 * • Первый и самый многочисленный слой пирамиды
 *
 * Технологии:
 * ────────────
 * • JUnit 5     — фреймворк запуска тестов
 * • Mockito     — библиотека мокирования
 * • AssertJ     — выразительные проверки (assertThat)
 * • Allure      — отчёты и TMS-интеграция
 *
 * Аннотации Allure:
 * ─────────────────
 * @Epic     → крупная функциональность (тема)
 * @Feature  → функция внутри эпика
 * @Story    → конкретная пользовательская история
 * @Layer    → уровень пирамиды (unit/component/integration/e2e)
 */
@Tag("unit")                               // ← тег для запуска: mvn test -P unit
@Epic("Управление книгами")
@Feature("Логика скидок и инвентаря")
@ExtendWith(MockitoExtension.class)        // ← подключает Mockito без Spring
class BookServiceUnitTest {

    // ── Mockito создаёт фиктивный объект вместо реального репозитория ──
    @Mock
    private BookRepository bookRepository;

    @Mock
    private IsbnLookupClient isbnLookupClient;

    // ── @InjectMocks создаёт BookService и вставляет в него моки ───────
    @InjectMocks
    private BookService bookService;

    // ═══════════════════════════════════════════════════════════════════
    //  ТЕСТ-КЕЙСЫ: applyDiscount()
    //  Метод не зависит ни от БД, ни от внешних сервисов →
    //  идеален для unit-тестирования
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Метод applyDiscount — применение скидки")
    class ApplyDiscountTests {

        @Test
        @Story("Скидка 0% — цена не меняется")
        @Layer("unit")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Скидка 0% не изменяет цену")
        void shouldNotChangePrice_whenDiscountIsZero() {
            // ARRANGE  — подготовка данных
            BigDecimal price = new BigDecimal("500.00");

            // ACT      — вызов тестируемого метода
            BigDecimal result = bookService.applyDiscount(price, 0);

            // ASSERT   — проверка результата
            assertThat(result).isEqualByComparingTo("500.00");
        }

        @Test
        @Story("Скидка 100% — книга бесплатна")
        @Layer("unit")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Скидка 100% даёт цену 0")
        void shouldReturnZero_whenDiscountIs100() {
            BigDecimal result = bookService.applyDiscount(new BigDecimal("999.99"), 100);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @Story("Скидка более 100% — книга бесплатна")
        @Layer("unit")
        @DisplayName("Скидка > 100% тоже даёт цену 0")
        void shouldReturnZero_whenDiscountExceeds100() {
            BigDecimal result = bookService.applyDiscount(new BigDecimal("100.00"), 150);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * ПАРАМЕТРИЗОВАННЫЙ ТЕСТ — одна логика, много наборов данных.
         * Позволяет элегантно проверить граничные значения.
         */
        @ParameterizedTest(name = "Скидка {1}% от {0}₽ → {2}₽")
        @MethodSource("discountCases")
        @Story("Корректное применение скидки")
        @Layer("unit")
        @DisplayName("Применение стандартных скидок")
        void shouldApplyDiscountCorrectly(
                String originalStr, int discount, String expectedStr) {
            BigDecimal result = bookService.applyDiscount(
                    new BigDecimal(originalStr), discount);
            assertThat(result)
                    .as("Цена после скидки %d%% от %s", discount, originalStr)
                    .isEqualByComparingTo(expectedStr);
        }

        static Stream<Arguments> discountCases() {
            return Stream.of(
                    Arguments.of("100.00", 10, "90.00"),
                    Arguments.of("100.00", 25, "75.00"),
                    Arguments.of("100.00", 50, "50.00"),
                    Arguments.of("333.33", 33, "223.33"),
                    Arguments.of("1000.00", 15, "850.00")
            );
        }

        @Test
        @Story("Отрицательная скидка вызывает ошибку")
        @Layer("unit")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("IllegalArgumentException при отрицательной скидке")
        void shouldThrowException_whenDiscountIsNegative() {
            assertThatThrownBy(
                    () -> bookService.applyDiscount(new BigDecimal("100.00"), -5)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("отрицательной");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ТЕСТ-КЕЙСЫ: findById()  — тест с моком репозитория
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Метод findById — поиск книги по ID")
    class FindByIdTests {

        @Test
        @Story("Книга найдена по ID")
        @Layer("unit")
        @DisplayName("Возвращает книгу, если она существует")
        void shouldReturnBook_whenFound() {
            // ARRANGE: настраиваем поведение мока
            Book book = Book.builder()
                    .id(1L).title("Чистый код").isbn("978-0132350884")
                    .price(new BigDecimal("750.00")).stockQuantity(10)
                    .genre(Book.Genre.PROGRAMMING).build();

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            // ACT
            Book result = bookService.findById(1L);

            // ASSERT
            assertThat(result.getTitle()).isEqualTo("Чистый код");
            verify(bookRepository, times(1)).findById(1L); // проверяем вызов мока
        }

        @Test
        @Story("Книга не найдена — выброс исключения")
        @Layer("unit")
        @DisplayName("BookNotFoundException если книги нет")
        void shouldThrowNotFoundException_whenBookMissing() {
            // ARRANGE: мок возвращает пустой Optional
            when(bookRepository.findById(99L)).thenReturn(Optional.empty());

            // ASSERT + ACT — проверяем ожидаемое исключение
            assertThatThrownBy(() -> bookService.findById(99L))
                    .isInstanceOf(BookNotFoundException.class)
                    .hasMessageContaining("99");

            verify(bookRepository).findById(99L);
            // Убеждаемся, что других взаимодействий с репозиторием не было
            verifyNoMoreInteractions(bookRepository);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ТЕСТ-КЕЙСЫ: reserveStock() — проверка бизнес-правила инвентаря
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Метод reserveStock — резервирование товара")
    class ReserveStockTests {

        private Book testBook;

        @BeforeEach
        void setUp() {
            testBook = Book.builder()
                    .id(1L).title("Паттерны проектирования")
                    .isbn("978-0201633610")
                    .price(new BigDecimal("900.00"))
                    .stockQuantity(5)    // 5 штук на складе
                    .genre(Book.Genre.PROGRAMMING)
                    .build();

            when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
            lenient().when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @Story("Успешное резервирование при достаточном остатке")
        @Layer("unit")
        @DisplayName("Остаток уменьшается после резервирования")
        void shouldDecreaseStock_whenSufficientQuantity() {
            Book result = bookService.reserveStock(1L, 3);

            assertThat(result.getStockQuantity()).isEqualTo(2); // 5 - 3 = 2
            verify(bookRepository).save(testBook);
        }

        @Test
        @Story("OutOfStockException при нехватке товара")
        @Layer("unit")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Выброс OutOfStockException при нехватке")
        void shouldThrowOutOfStock_whenInsufficientQuantity() {
            assertThatThrownBy(() -> bookService.reserveStock(1L, 10)) // запрашиваем 10, есть 5
                    .isInstanceOf(OutOfStockException.class)
                    .hasMessageContaining("5");  // сообщение должно содержать доступный остаток

            // Убеждаемся, что сохранения не было
            verify(bookRepository, never()).save(any());
        }

        @Test
        @Story("Резервирование оставшегося количества целиком")
        @Layer("unit")
        @DisplayName("Можно зарезервировать ровно всё оставшееся")
        void shouldAllowReservingExactRemainingStock() {
            Book result = bookService.reserveStock(1L, 5); // ровно 5
            assertThat(result.getStockQuantity()).isZero();
        }
    }
}

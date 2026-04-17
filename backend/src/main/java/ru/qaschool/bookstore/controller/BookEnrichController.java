package ru.qaschool.bookstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.qaschool.bookstore.domain.Book;
import ru.qaschool.bookstore.service.BookService;

/**
 * Дополнительный контроллер для демонстрации интеграции с ISBN-сервисом.
 * Эндпоинт POST /api/books/{id}/enrich используется в интеграционных тестах
 * для демонстрации WireMock.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookEnrichController {

    private final BookService bookService;

    /**
     * Обогащает данные книги информацией из внешнего ISBN-сервиса.
     * Демонстрирует интеграцию с внешними API и graceful degradation.
     */
    @PostMapping("/{id}/enrich")
    public Book enrich(@PathVariable Long id) {
        return bookService.enrichFromIsbnService(id);
    }
}

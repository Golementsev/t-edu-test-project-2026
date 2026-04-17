package ru.qaschool.bookstore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.qaschool.bookstore.domain.Book;
import ru.qaschool.bookstore.service.BookService;

import java.util.List;

/**
 * REST-контроллер для управления книгами.
 *
 * <p>API:
 * <pre>
 * GET    /api/books          — все книги
 * GET    /api/books/{id}     — книга по ID
 * GET    /api/books/search   — поиск по названию (?q=...)
 * POST   /api/books          — создать книгу
 * PUT    /api/books/{id}     — обновить книгу
 * DELETE /api/books/{id}     — удалить книгу
 * </pre>
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<Book> getAll() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    public Book getById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @GetMapping("/search")
    public List<Book> search(@RequestParam String q) {
        return bookService.findAll().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(q.toLowerCase()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book create(@Valid @RequestBody Book book) {
        return bookService.create(book);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable Long id, @Valid @RequestBody Book book) {
        return bookService.update(id, book);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }
}

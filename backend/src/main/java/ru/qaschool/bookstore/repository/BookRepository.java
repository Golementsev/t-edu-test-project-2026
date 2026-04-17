package ru.qaschool.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.qaschool.bookstore.domain.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findByGenre(Book.Genre genre);

    List<Book> findByTitleContainingIgnoreCase(String keyword);

    @Query("SELECT b FROM Book b WHERE b.stockQuantity > 0")
    List<Book> findAllInStock();

    @Query("SELECT b FROM Book b WHERE b.author.id = :authorId")
    List<Book> findByAuthorId(Long authorId);
}

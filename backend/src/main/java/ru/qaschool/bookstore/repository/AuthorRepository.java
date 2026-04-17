package ru.qaschool.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.qaschool.bookstore.domain.Author;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByEmail(String email);
}

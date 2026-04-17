package ru.qaschool.bookstore.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Книга — основная сущность магазина.
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "author")
@EqualsAndHashCode(of = "id")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название книги не может быть пустым")
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "ISBN не может быть пустым")
    @Pattern(regexp = "\\d{3}-\\d{10}", message = "Неверный формат ISBN: ожидается 978-XXXXXXXXXX")
    @Column(nullable = false, unique = true, length = 14)
    private String isbn;

    @NotNull(message = "Цена не может быть null")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Количество на складе не может быть отрицательным")
    @Column(nullable = false)
    private int stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    public enum Genre {
        FICTION, NON_FICTION, SCIENCE, HISTORY, CHILDREN, PROGRAMMING
    }
}

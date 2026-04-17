import React from 'react'
import { formatPrice, getGenreLabel } from '../utils/priceUtils'

/**
 * Карточка книги.
 *
 * Props:
 * - book: объект книги ({id, title, isbn, price, stockQuantity, genre, author})
 * - onAddToCart: callback при клике на "В корзину"
 *
 * data-testid атрибуты добавлены для удобства написания тестов:
 * - Playwright/Testing Library ищет элементы по data-testid
 * - Это лучше, чем поиск по CSS-классам (хрупкий) или тексту (зависит от языка)
 */
const BookCard = ({ book, onAddToCart }) => {
  const inStock = book.stockQuantity > 0

  return (
    <article
      className="book-card"
      data-testid="book-card"
      data-book-id={book.id}
    >
      <div className="book-card__genre" data-testid="book-genre">
        {getGenreLabel(book.genre)}
      </div>

      <h3 className="book-card__title" data-testid="book-title">
        {book.title}
      </h3>

      {book.author && (
        <p className="book-card__author" data-testid="book-author">
          {book.author.firstName} {book.author.lastName}
        </p>
      )}

      <p className="book-card__isbn" data-testid="book-isbn">
        ISBN: {book.isbn}
      </p>

      <div className="book-card__footer">
        <div>
          <p className="book-card__price" data-testid="book-price">
            {formatPrice(book.price)}
            <span>/ шт.</span>
          </p>
          <p
            className={`book-card__stock ${!inStock ? 'book-card__stock--empty' : ''}`}
            data-testid="book-stock"
          >
            {inStock ? `В наличии: ${book.stockQuantity} шт.` : 'Нет в наличии'}
          </p>
        </div>

        <button
          className="btn btn--primary btn--sm"
          onClick={() => onAddToCart(book)}
          disabled={!inStock}
          data-testid="add-to-cart-btn"
          aria-label={`Добавить "${book.title}" в корзину`}
        >
          {inStock ? '+ В корзину' : 'Нет'}
        </button>
      </div>
    </article>
  )
}

export default BookCard

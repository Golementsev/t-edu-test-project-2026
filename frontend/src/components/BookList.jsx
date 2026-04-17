import React, { useState, useEffect } from 'react'
import BookCard from './BookCard.jsx'
import Cart from './Cart.jsx'
import { booksApi, ordersApi } from '../api/booksApi.js'

/**
 * Главная страница магазина — каталог книг.
 */
const BookList = () => {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const [cartItems, setCartItems] = useState([])
  const [showCart, setShowCart] = useState(false)
  const [toasts, setToasts] = useState([])

  // ─── Загрузка книг ────────────────────────────────────────────
  useEffect(() => {
    setLoading(true)
    booksApi.getAll()
      .then(data => { setBooks(data); setError(null) })
      .catch(() => setError('Не удалось загрузить книги'))
      .finally(() => setLoading(false))
  }, [])

  // ─── Поиск ─────────────────────────────────────────────────────
  const filteredBooks = books.filter(b =>
    b.title.toLowerCase().includes(search.toLowerCase()) ||
    (b.isbn && b.isbn.includes(search))
  )

  // ─── Корзина ────────────────────────────────────────────────────
  const addToCart = (book) => {
    setCartItems(prev => {
      const existing = prev.find(i => i.book.id === book.id)
      if (existing) {
        return prev.map(i =>
          i.book.id === book.id ? { ...i, quantity: i.quantity + 1 } : i
        )
      }
      return [...prev, { book, quantity: 1 }]
    })
    showToast(`"${book.title}" добавлена в корзину`)
  }

  const removeFromCart = (bookId) => {
    setCartItems(prev => prev.filter(i => i.book.id !== bookId))
  }

  const updateQuantity = (bookId, qty) => {
    if (qty < 1) return
    setCartItems(prev => prev.map(i =>
      i.book.id === bookId ? { ...i, quantity: qty } : i
    ))
  }

  const handleCheckout = async (email) => {
    try {
      const items = cartItems.map(i => ({ bookId: i.book.id, quantity: i.quantity }))
      await ordersApi.create({ customerEmail: email, items })
      setCartItems([])
      setShowCart(false)
      showToast('Заказ успешно оформлен! 🎉')
    } catch (e) {
      showToast('Ошибка при оформлении заказа', 'error')
    }
  }

  // ─── Toast-уведомления ──────────────────────────────────────────
  const showToast = (message, type = 'success') => {
    const id = Date.now()
    setToasts(prev => [...prev, { id, message, type }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3000)
  }

  const cartCount = cartItems.reduce((s, i) => s + i.quantity, 0)

  return (
    <>
      {/* ── Navbar ── */}
      <nav className="navbar" role="navigation">
        <div className="container navbar__inner">
          <div className="navbar__brand">
            📚 Book<span>Store</span>
          </div>
          <button
            className="btn btn--ghost flex gap-1"
            onClick={() => setShowCart(true)}
            data-testid="cart-btn"
            aria-label={`Корзина, ${cartCount} товаров`}
          >
            🛒 Корзина
            {cartCount > 0 && (
              <span className="cart-badge" data-testid="cart-count">{cartCount}</span>
            )}
          </button>
        </div>
      </nav>

      {/* ── Главный контент ── */}
      <main className="container" role="main">
        <section className="hero">
          <h1 className="hero__title">Книжный магазин</h1>
          <p className="hero__sub">
            Тысячи книг. Всегда в наличии. Быстрая доставка.
          </p>
        </section>

        {/* ── Поиск ── */}
        <div className="search-bar" role="search">
          <input
            type="text"
            placeholder="Поиск по названию или ISBN..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            data-testid="search-input"
            aria-label="Поиск книг"
          />
          {search && (
            <button
              className="btn btn--ghost"
              onClick={() => setSearch('')}
              data-testid="search-clear-btn"
            >
              Очистить
            </button>
          )}
        </div>

        {/* ── Состояния ── */}
        {loading && <div className="spinner" data-testid="loading-spinner" role="status" />}

        {error && (
          <div
            className="empty-state"
            data-testid="error-message"
            role="alert"
          >
            <div className="empty-state__icon">⚠️</div>
            <p>{error}</p>
          </div>
        )}

        {!loading && !error && filteredBooks.length === 0 && (
          <div className="empty-state" data-testid="empty-state">
            <div className="empty-state__icon">🔍</div>
            <p>Книги не найдены</p>
          </div>
        )}

        {/* ── Сетка книг ── */}
        {!loading && !error && filteredBooks.length > 0 && (
          <section
            className="books-grid"
            aria-label="Каталог книг"
            data-testid="books-grid"
          >
            {filteredBooks.map(book => (
              <BookCard
                key={book.id}
                book={book}
                onAddToCart={addToCart}
              />
            ))}
          </section>
        )}
      </main>

      {/* ── Корзина ── */}
      {showCart && (
        <Cart
          items={cartItems}
          onRemove={removeFromCart}
          onUpdateQuantity={updateQuantity}
          onCheckout={handleCheckout}
          onClose={() => setShowCart(false)}
        />
      )}

      {/* ── Toast-уведомления ── */}
      <div className="toast-container" aria-live="polite">
        {toasts.map(t => (
          <div
            key={t.id}
            className={`toast${t.type === 'error' ? ' toast--error' : ''}`}
            data-testid="toast"
          >
            {t.message}
          </div>
        ))}
      </div>
    </>
  )
}

export default BookList

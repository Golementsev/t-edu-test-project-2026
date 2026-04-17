import React, { useState } from 'react'
import { formatPrice, calculateCartTotal } from '../utils/priceUtils'

/**
 * Боковая панель корзины.
 *
 * Props:
 * - items: массив [{book, quantity}]
 * - onRemove: callback(bookId)
 * - onUpdateQuantity: callback(bookId, newQty)
 * - onCheckout: callback(customerEmail)
 * - onClose: callback()
 */
const Cart = ({ items, onRemove, onUpdateQuantity, onCheckout, onClose }) => {
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState('')

  const total = calculateCartTotal(
    items.map(i => ({ price: i.book.price, quantity: i.quantity }))
  )

  const handleCheckout = () => {
    if (!email || !email.includes('@')) {
      setEmailError('Введите корректный email')
      return
    }
    setEmailError('')
    onCheckout(email)
  }

  return (
    <div className="modal-overlay" onClick={onClose} data-testid="cart-overlay">
      <div
        className="modal"
        style={{ maxWidth: '540px' }}
        onClick={e => e.stopPropagation()}
        data-testid="cart-panel"
      >
        <div className="flex" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>🛒 Корзина</h2>
          <button
            className="btn btn--ghost btn--sm"
            onClick={onClose}
            data-testid="cart-close-btn"
          >
            ✕
          </button>
        </div>

        {items.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state__icon">🛒</div>
            <p>Корзина пуста</p>
          </div>
        ) : (
          <>
            <div className="cart-summary">
              {items.map(({ book, quantity }) => (
                <div className="cart-item" key={book.id} data-testid="cart-item">
                  <div style={{ flex: 1 }}>
                    <p style={{ fontWeight: 500 }} data-testid="cart-item-title">
                      {book.title}
                    </p>
                    <p className="text-muted text-sm">{formatPrice(book.price)}</p>
                  </div>

                  <div className="flex gap-1" style={{ alignItems: 'center' }}>
                    <button
                      className="btn btn--ghost btn--sm"
                      onClick={() => onUpdateQuantity(book.id, quantity - 1)}
                      disabled={quantity <= 1}
                      data-testid="qty-decrease-btn"
                    >
                      −
                    </button>
                    <span data-testid="cart-item-qty" style={{ minWidth: '1.5rem', textAlign: 'center' }}>
                      {quantity}
                    </span>
                    <button
                      className="btn btn--ghost btn--sm"
                      onClick={() => onUpdateQuantity(book.id, quantity + 1)}
                      disabled={quantity >= book.stockQuantity}
                      data-testid="qty-increase-btn"
                    >
                      +
                    </button>
                  </div>

                  <p data-testid="cart-item-total">
                    {formatPrice(book.price * quantity)}
                  </p>

                  <button
                    className="btn btn--danger btn--sm"
                    onClick={() => onRemove(book.id)}
                    data-testid="cart-item-remove-btn"
                    aria-label={`Удалить "${book.title}"`}
                  >
                    ✕
                  </button>
                </div>
              ))}

              <div className="cart-total">
                <span>Итого:</span>
                <span data-testid="cart-total">{formatPrice(total)}</span>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="customer-email">Email для подтверждения заказа</label>
              <input
                id="customer-email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                data-testid="customer-email-input"
              />
              {emailError && (
                <p className="form-error" data-testid="email-error">{emailError}</p>
              )}
            </div>

            <button
              className="btn btn--primary"
              onClick={handleCheckout}
              data-testid="checkout-btn"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              Оформить заказ на {formatPrice(total)}
            </button>
          </>
        )}
      </div>
    </div>
  )
}

export default Cart

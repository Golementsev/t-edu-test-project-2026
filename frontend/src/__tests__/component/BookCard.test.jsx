import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import BookCard from '../../components/BookCard'

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ ФРОНТЕНД: УРОВЕНЬ 2 — КОМПОНЕНТНЫЕ ТЕСТЫ ◀◀
 *     (@testing-library/react + Vitest)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что тестируем?
 * ──────────────────
 * React-компоненты: рендеринг, взаимодействие, состояния.
 * Компонент изолирован — нет реальных HTTP-запросов.
 *
 * Технологии:
 * ────────────
 * • @testing-library/react — рендер компонентов в jsdom
 * • userEvent               — симуляция действий пользователя
 * • screen                  — поиск элементов в DOM
 *
 * Принцип Testing Library:
 * ─────────────────────────
 * «Тесты должны имитировать поведение пользователя».
 * Ищем элементы так, как пользователь их видит:
 *   - по роли: getByRole('button', {name: '...'})
 *   - по тексту: getByText('...')
 *   - по label: getByLabelText('...')
 *   - по testid: getByTestId('...') ← только если нет лучшего варианта
 */

// ── Тестовые данные (фикстуры) ────────────────────────────────────────
const createBook = (overrides = {}) => ({
  id: 1,
  title: 'Чистый код',
  isbn: '978-0132350884',
  price: 750,
  stockQuantity: 10,
  genre: 'PROGRAMMING',
  author: { firstName: 'Роберт', lastName: 'Мартин' },
  ...overrides,
})

// ─────────────────────────────────────────────────────────────────────

describe('BookCard — рендеринг', () => {

  it('отображает название книги', () => {
    render(<BookCard book={createBook()} onAddToCart={() => {}} />)
    expect(screen.getByTestId('book-title')).toHaveTextContent('Чистый код')
  })

  it('отображает ISBN', () => {
    render(<BookCard book={createBook()} onAddToCart={() => {}} />)
    expect(screen.getByTestId('book-isbn')).toHaveTextContent('978-0132350884')
  })

  it('отображает цену в рублях', () => {
    render(<BookCard book={createBook({ price: 750 })} onAddToCart={() => {}} />)
    const priceEl = screen.getByTestId('book-price')
    expect(priceEl).toHaveTextContent('750')
    expect(priceEl).toHaveTextContent('₽')
  })

  it('отображает имя автора', () => {
    render(<BookCard book={createBook()} onAddToCart={() => {}} />)
    expect(screen.getByTestId('book-author')).toHaveTextContent('Роберт Мартин')
  })

  it('отображает жанр на русском', () => {
    render(<BookCard book={createBook()} onAddToCart={() => {}} />)
    expect(screen.getByTestId('book-genre')).toHaveTextContent('Программирование')
  })
})

describe('BookCard — наличие на складе', () => {

  it('показывает количество, когда книга есть в наличии', () => {
    render(<BookCard book={createBook({ stockQuantity: 5 })} onAddToCart={() => {}} />)
    expect(screen.getByTestId('book-stock')).toHaveTextContent('В наличии: 5 шт.')
  })

  it('показывает "Нет в наличии" при stockQuantity = 0', () => {
    render(<BookCard book={createBook({ stockQuantity: 0 })} onAddToCart={() => {}} />)
    expect(screen.getByTestId('book-stock')).toHaveTextContent('Нет в наличии')
  })

  it('кнопка "В корзину" активна при наличии товара', () => {
    render(<BookCard book={createBook({ stockQuantity: 3 })} onAddToCart={() => {}} />)
    const btn = screen.getByTestId('add-to-cart-btn')
    expect(btn).not.toBeDisabled()
    expect(btn).toHaveTextContent('+ В корзину')  // компонент рендерит '+ В корзину'
  })

  it('кнопка "В корзину" заблокирована при отсутствии товара', () => {
    render(<BookCard book={createBook({ stockQuantity: 0 })} onAddToCart={() => {}} />)
    const btn = screen.getByTestId('add-to-cart-btn')
    expect(btn).toBeDisabled()
    expect(btn).toHaveTextContent('Нет')
  })
})

describe('BookCard — взаимодействие', () => {

  it('вызывает onAddToCart с объектом книги при клике', async () => {
    const user = userEvent.setup()
    const onAddToCart = vi.fn()                       // ← мок-функция для проверки вызова
    const book = createBook()

    render(<BookCard book={book} onAddToCart={onAddToCart} />)
    await user.click(screen.getByTestId('add-to-cart-btn'))

    expect(onAddToCart).toHaveBeenCalledTimes(1)
    expect(onAddToCart).toHaveBeenCalledWith(book)    // проверяем аргумент
  })

  it('не вызывает onAddToCart для книги не в наличии', async () => {
    const user = userEvent.setup()
    const onAddToCart = vi.fn()

    render(<BookCard book={createBook({ stockQuantity: 0 })} onAddToCart={onAddToCart} />)
    await user.click(screen.getByTestId('add-to-cart-btn'))

    expect(onAddToCart).not.toHaveBeenCalled()
  })
})

describe('BookCard — accessibility (доступность)', () => {

  it('кнопка имеет aria-label с названием книги', () => {
    render(<BookCard book={createBook()} onAddToCart={() => {}} />)
    // screen.getByRole находит кнопку по роли и accessible name
    const btn = screen.getByRole('button', { name: /Чистый код/i })
    expect(btn).toBeInTheDocument()
  })
})

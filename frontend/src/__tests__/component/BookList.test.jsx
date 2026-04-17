import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import BookList from '../../components/BookList'

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ ФРОНТЕНД: КОМПОНЕНТНЫЙ ТЕСТ С MSW (Mock Service Worker) ◀◀
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что такое MSW?
 * ──────────────────
 * MSW (Mock Service Worker) — библиотека для перехвата HTTP-запросов.
 * Работает на уровне сети: наш компонент делает РЕАЛЬНЫЙ axios-запрос,
 * но перехватчик MSW возвращает заготовленный ответ.
 *
 * Преимущества перед vi.mock():
 *   • Компонент не знает о моке — тест максимально реалистичен
 *   • Можно тестировать загрузку, ошибки, разные сценарии ответов
 *   • Один и тот же handler работает в unit + e2e тестах
 *
 * Аналог в бэкенде: WireMock (Java), но для JS
 */

// ── MSW сервер — перехватывает HTTP в Node.js ──────────────────────────
const BOOKS = [
  { id: 1, title: 'Чистый код', isbn: '978-0132350884',
    price: 750, stockQuantity: 5, genre: 'PROGRAMMING' },
  { id: 2, title: 'Паттерны проектирования', isbn: '978-0201633610',
    price: 900, stockQuantity: 2, genre: 'PROGRAMMING' },
  { id: 3, title: 'Гарри Поттер', isbn: '978-5389078338',
    price: 400, stockQuantity: 0, genre: 'FICTION' },
]

const server = setupServer(
  http.get('/api/books', () => HttpResponse.json(BOOKS)),
  http.post('/api/orders', () => HttpResponse.json({ id: 42, status: 'NEW' }, { status: 201 })),
)

beforeEach(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => { server.resetHandlers() })
afterEach(() => server.close())

// ─────────────────────────────────────────────────────────────────────

describe('BookList — загрузка каталога', () => {

  it('показывает спиннер во время загрузки', () => {
    render(<BookList />)
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
  })

  it('показывает список книг после загрузки', async () => {
    render(<BookList />)
    // waitFor ожидает пока DOM обновится (асинхронный запрос)
    await waitFor(() => {
      expect(screen.getByTestId('books-grid')).toBeInTheDocument()
    })
    const cards = screen.getAllByTestId('book-card')
    expect(cards).toHaveLength(3)
  })

  it('показывает названия всех книг', async () => {
    render(<BookList />)
    await waitFor(() => screen.getByText('Чистый код'))
    expect(screen.getByText('Паттерны проектирования')).toBeInTheDocument()
    expect(screen.getByText('Гарри Поттер')).toBeInTheDocument()
  })
})

describe('BookList — ошибка загрузки', () => {

  it('показывает сообщение об ошибке при сбое API', async () => {
    // Переопределяем хендлер — API возвращает 500
    server.use(
      http.get('/api/books', () => HttpResponse.error())
    )

    render(<BookList />)
    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument()
    })
    expect(screen.getByTestId('error-message')).toHaveTextContent('Не удалось загрузить')
  })
})

describe('BookList — поиск', () => {

  it('фильтрует книги по названию', async () => {
    const user = userEvent.setup()
    render(<BookList />)
    await waitFor(() => screen.getByTestId('books-grid'))

    await user.type(screen.getByTestId('search-input'), 'Чистый')

    const cards = screen.getAllByTestId('book-card')
    expect(cards).toHaveLength(1)
    expect(screen.getByTestId('book-title')).toHaveTextContent('Чистый код')
  })

  it('показывает "не найдено" если нет совпадений', async () => {
    const user = userEvent.setup()
    render(<BookList />)
    await waitFor(() => screen.getByTestId('books-grid'))

    await user.type(screen.getByTestId('search-input'), 'Несуществующая книга')
    expect(screen.getByTestId('empty-state')).toBeInTheDocument()
  })

  it('кнопка "Очистить" сбрасывает поиск', async () => {
    const user = userEvent.setup()
    render(<BookList />)
    await waitFor(() => screen.getByTestId('books-grid'))

    await user.type(screen.getByTestId('search-input'), 'Чистый')
    expect(screen.getAllByTestId('book-card')).toHaveLength(1)

    await user.click(screen.getByTestId('search-clear-btn'))
    expect(screen.getAllByTestId('book-card')).toHaveLength(3)
  })
})

describe('BookList — корзина', () => {

  it('значок корзины появляется после добавления товара', async () => {
    const user = userEvent.setup()
    render(<BookList />)
    await waitFor(() => screen.getByTestId('books-grid'))

    // Изначально счётчика нет
    expect(screen.queryByTestId('cart-count')).not.toBeInTheDocument()

    // Добавляем книгу (только первые две — в наличии)
    const addButtons = screen.getAllByTestId('add-to-cart-btn')
    await user.click(addButtons[0]) // "Чистый код"

    expect(screen.getByTestId('cart-count')).toHaveTextContent('1')
  })

  it('добавление одной книги дважды увеличивает количество', async () => {
    const user = userEvent.setup()
    render(<BookList />)
    await waitFor(() => screen.getByTestId('books-grid'))

    const addButtons = screen.getAllByTestId('add-to-cart-btn')
    await user.click(addButtons[0])
    await user.click(addButtons[0])

    expect(screen.getByTestId('cart-count')).toHaveTextContent('2')
  })

  it('кнопка для книги без остатка заблокирована', async () => {
    render(<BookList />)
    await waitFor(() => screen.getByTestId('books-grid'))

    const addButtons = screen.getAllByTestId('add-to-cart-btn')
    // Третья книга — "Гарри Поттер", stockQuantity = 0
    expect(addButtons[2]).toBeDisabled()
  })
})

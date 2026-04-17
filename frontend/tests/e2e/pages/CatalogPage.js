import { test, expect } from '@playwright/test'

/**
 * Page Object Model (POM) для страницы каталога.
 *
 * Что такое POM?
 * ──────────────────
 * Паттерн проектирования тестов: выносим локаторы и действия
 * со страницей в отдельный класс. Это делает тесты:
 * • Читаемыми: test action → page.addBookToCart()
 * • Поддерживаемыми: при изменении UI правим только POM, не тесты
 * • Переиспользуемыми: один класс → много тест-файлов
 */
class CatalogPage {
  constructor(page) {
    this.page = page
    // Локаторы — выражения для поиска элементов
    this.searchInput = page.getByTestId('search-input')
    this.cartBtn = page.getByTestId('cart-btn')
    this.cartCount = page.getByTestId('cart-count')
    this.loadingSpinner = page.getByTestId('loading-spinner')
    this.booksGrid = page.getByTestId('books-grid')
    this.emptyState = page.getByTestId('empty-state')
  }

  async navigate() {
    await this.page.goto('/')
    await this.page.waitForLoadState('networkidle')
  }

  bookCards() {
    return this.page.getByTestId('book-card')
  }

  addToCartButtons() {
    return this.page.getByTestId('add-to-cart-btn')
  }

  async search(query) {
    await this.searchInput.fill(query)
  }

  async clearSearch() {
    await this.page.getByTestId('search-clear-btn').click()
  }

  async openCart() {
    await this.cartBtn.click()
  }
}

class CartPanel {
  constructor(page) {
    this.page = page
    this.panel = page.getByTestId('cart-panel')
    this.emailInput = page.getByTestId('customer-email-input')
    this.checkoutBtn = page.getByTestId('checkout-btn')
    this.closeBtn = page.getByTestId('cart-close-btn')
    this.totalEl = page.getByTestId('cart-total')
  }

  cartItems() {
    return this.page.getByTestId('cart-item')
  }

  async fillEmail(email) {
    await this.emailInput.fill(email)
  }

  async checkout() {
    await this.checkoutBtn.click()
  }

  async close() {
    await this.closeBtn.click()
  }
}

export { CatalogPage, CartPanel }

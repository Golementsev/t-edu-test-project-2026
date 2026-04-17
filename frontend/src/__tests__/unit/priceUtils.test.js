import { describe, it, expect, test } from 'vitest'
import {
  formatPrice,
  applyDiscount,
  calculateCartTotal,
  isValidIsbn,
  getGenreLabel,
} from '../../utils/priceUtils'

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ▶▶ ФРОНТЕНД: УРОВЕНЬ 1 — UNIT-ТЕСТЫ ◀◀
 *     (Vitest, без рендеринга компонентов)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Что тестируем?
 * ──────────────────
 * Чистые функции-утилиты — они не зависят ни от React, ни от DOM.
 * Это самый быстрый уровень тестов.
 *
 * Технологии:
 * ────────────
 * • Vitest — тест-раннер (аналог Jest, но встроен в Vite)
 * • describe/it/expect — стандартный API
 *
 * Запуск:
 *   npm test            (один проход)
 *   npm run test:watch  (режим слежения → тест запускается при сохранении файла)
 */

describe('formatPrice — форматирование цены', () => {

  it('форматирует число в рубли', () => {
    const result = formatPrice(750)
    // В русской локали: "750,00 ₽"
    expect(result).toContain('750')
    expect(result).toContain('₽')
  })

  it('форматирует дробную цену', () => {
    const result = formatPrice(99.9)
    expect(result).toContain('99')
    expect(result).toContain('₽')
  })

  it('возвращает "—" для null', () => {
    expect(formatPrice(null)).toBe('—')
  })

  it('возвращает "—" для undefined', () => {
    expect(formatPrice(undefined)).toBe('—')
  })

  it('возвращает "—" для NaN', () => {
    expect(formatPrice(NaN)).toBe('—')
  })
})

describe('applyDiscount — применение скидки', () => {

  it('не изменяет цену при скидке 0%', () => {
    expect(applyDiscount(100, 0)).toBe(100)
  })

  it('применяет скидку 10%', () => {
    expect(applyDiscount(100, 10)).toBe(90)
  })

  it('применяет скидку 50%', () => {
    expect(applyDiscount(200, 50)).toBe(100)
  })

  it('возвращает 0 при скидке 100%', () => {
    expect(applyDiscount(999, 100)).toBe(0)
  })

  it('возвращает 0 при скидке > 100%', () => {
    expect(applyDiscount(999, 150)).toBe(0)
  })

  it('бросает ошибку при отрицательной скидке', () => {
    expect(() => applyDiscount(100, -5)).toThrow('отрицательной')
  })

  // Параметризованный тест через test.each
  test.each([
    [100, 25, 75],
    [500, 20, 400],
    [333, 33, 223.11],
    [1000, 15, 850],
  ])('applyDiscount(%i, %i%) → %i', (price, discount, expected) => {
    expect(applyDiscount(price, discount)).toBeCloseTo(expected, 1)
  })
})

describe('calculateCartTotal — сумма корзины', () => {

  it('считает сумму одной позиции', () => {
    const items = [{ price: 100, quantity: 3 }]
    expect(calculateCartTotal(items)).toBe(300)
  })

  it('считает сумму нескольких позиций', () => {
    const items = [
      { price: 100, quantity: 2 },   // 200
      { price: 50,  quantity: 1 },   // 50
      { price: 250, quantity: 3 },   // 750
    ]
    expect(calculateCartTotal(items)).toBe(1000)
  })

  it('возвращает 0 для пустой корзины', () => {
    expect(calculateCartTotal([])).toBe(0)
  })
})

describe('isValidIsbn — валидация ISBN', () => {

  it('принимает корректный ISBN', () => {
    expect(isValidIsbn('978-0132350884')).toBe(true)
    expect(isValidIsbn('978-5170922673')).toBe(true)
  })

  it('отклоняет ISBN без дефиса', () => {
    expect(isValidIsbn('9780132350884')).toBe(false)
  })

  it('отклоняет ISBN с буквами', () => {
    expect(isValidIsbn('978-ABC123456')).toBe(false)
  })

  it('отклоняет пустую строку', () => {
    expect(isValidIsbn('')).toBe(false)
  })

  it('отклоняет неправильную длину', () => {
    expect(isValidIsbn('978-012345678')).toBe(false)  // 9 цифр вместо 10
    expect(isValidIsbn('978-01234567890')).toBe(false) // 11 цифр
  })
})

describe('getGenreLabel — название жанра', () => {

  it('возвращает "Программирование" для PROGRAMMING', () => {
    expect(getGenreLabel('PROGRAMMING')).toBe('Программирование')
  })

  it('возвращает "Художественная" для FICTION', () => {
    expect(getGenreLabel('FICTION')).toBe('Художественная')
  })

  it('возвращает оригинальное значение для неизвестного жанра', () => {
    expect(getGenreLabel('UNKNOWN_GENRE')).toBe('UNKNOWN_GENRE')
  })
})

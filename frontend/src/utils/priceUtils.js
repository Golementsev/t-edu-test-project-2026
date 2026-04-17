/**
 * Утилиты для работы с ценами.
 *
 * ПРИМЕЧАНИЕ: Эти чистые функции — идеальный кандидат для UNIT-тестов!
 * Никаких зависимостей, только входные данные → выходные данные.
 */

/**
 * Форматирует цену в читаемый вид с рублёвым знаком.
 * @param {number} price - цена в рублях
 * @returns {string} - строка вида "750,00 ₽"
 */
export const formatPrice = (price) => {
  if (price == null || isNaN(price)) return '—'
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    minimumFractionDigits: 2,
  }).format(price)
}

/**
 * Применяет скидку к цене.
 * @param {number} price - исходная цена
 * @param {number} discountPercent - процент скидки (0-100)
 * @returns {number} - цена после скидки
 */
export const applyDiscount = (price, discountPercent) => {
  if (discountPercent < 0) throw new Error('Скидка не может быть отрицательной')
  if (discountPercent >= 100) return 0
  return Number((price * (1 - discountPercent / 100)).toFixed(2))
}

/**
 * Вычисляет итоговую сумму корзины.
 * @param {Array<{price: number, quantity: number}>} items
 * @returns {number}
 */
export const calculateCartTotal = (items) => {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0)
}

/**
 * Валидирует формат ISBN.
 * Ожидаемый формат: 978-XXXXXXXXXX
 * @param {string} isbn
 * @returns {boolean}
 */
export const isValidIsbn = (isbn) => {
  return /^\d{3}-\d{10}$/.test(isbn)
}

/**
 * Возвращает человекочитаемое название жанра.
 * @param {string} genre - enum-значение (FICTION, PROGRAMMING, etc.)
 * @returns {string}
 */
export const getGenreLabel = (genre) => {
  const labels = {
    FICTION: 'Художественная',
    NON_FICTION: 'Нон-фикшн',
    SCIENCE: 'Наука',
    HISTORY: 'История',
    CHILDREN: 'Детская',
    PROGRAMMING: 'Программирование',
  }
  return labels[genre] ?? genre
}

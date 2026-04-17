import axios from 'axios'

/**
 * API-клиент для взаимодействия с BookStore бэкендом.
 *
 * Используется в тестах для демонстрации мокирования через MSW (Mock Service Worker).
 */
const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

export const booksApi = {
  getAll: () => apiClient.get('/books').then(r => r.data),
  getById: (id) => apiClient.get(`/books/${id}`).then(r => r.data),
  search: (q) => apiClient.get('/books/search', { params: { q } }).then(r => r.data),
  create: (book) => apiClient.post('/books', book).then(r => r.data),
  update: (id, book) => apiClient.put(`/books/${id}`, book).then(r => r.data),
  delete: (id) => apiClient.delete(`/books/${id}`),
}

export const ordersApi = {
  create: (orderData) => apiClient.post('/orders', orderData).then(r => r.data),
  getById: (id) => apiClient.get(`/orders/${id}`).then(r => r.data),
  getByEmail: (email) => apiClient.get('/orders', { params: { email } }).then(r => r.data),
  cancel: (id) => apiClient.post(`/orders/${id}/cancel`).then(r => r.data),
}

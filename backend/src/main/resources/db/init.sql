-- Инициализация БД и тестовые данные
-- (запускается один раз при старте PostgreSQL-контейнера)

-- Данные для демонстрации и SystemTests
INSERT INTO authors (first_name, last_name, email)
VALUES
  ('Роберт', 'Мартин', 'robert.martin@example.com'),
  ('Эрих', 'Гамма', 'erich.gamma@example.com'),
  ('Лев', 'Толстой', 'lev.tolstoy@example.com')
ON CONFLICT DO NOTHING;

INSERT INTO books (title, isbn, price, stock_quantity, genre, author_id)
VALUES
  ('Чистый код', '978-0132350884', 750.00, 10, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'robert.martin@example.com')),
  ('Паттерны проектирования', '978-0201633610', 900.00, 5, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'erich.gamma@example.com')),
  ('Война и мир', '978-5170922673', 500.00, 15, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com')),
  ('Рефакторинг', '978-0134757599', 850.00, 3, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'robert.martin@example.com'))
ON CONFLICT DO NOTHING;

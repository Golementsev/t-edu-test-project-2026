-- Инициализация БД и тестовые данные
-- (запускается один раз при старте PostgreSQL-контейнера)

CREATE TABLE IF NOT EXISTS authors (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS books (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    isbn VARCHAR(14) NOT NULL UNIQUE,
    price NUMERIC(10,2) NOT NULL,
    stock_quantity INT NOT NULL,
    genre VARCHAR(50) NOT NULL,
    author_id INT REFERENCES authors(id)
);

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
    (SELECT id FROM authors WHERE email = 'robert.martin@example.com')),
  ('Идеальный программист', '978-0137081073', 600.00, 20, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'robert.martin@example.com')),
  ('Преступление и наказание', '978-5171120252', 450.00, 12, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com')),
  ('Анна Каренина', '978-5041042730', 550.00, 8, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com')),
  ('Гарри Поттер и философский камень', '978-5389074354', 800.00, 25, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com')),
  ('Властелин колец: Братство Кольца', '978-5170870943', 950.00, 5, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com')),
  ('Алгоритмы: построение и анализ', '978-5845920164', 1200.00, 4, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'erich.gamma@example.com')),
  ('Совершенный код', '978-5750200641', 1100.00, 7, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'robert.martin@example.com')),
  ('Дюна', '978-5171161743', 700.00, 14, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com')),
  ('Грокаем алгоритмы', '978-5496025416', 500.00, 30, 'PROGRAMMING',
    (SELECT id FROM authors WHERE email = 'erich.gamma@example.com')),
  ('Мастер и Маргарита', '978-5170911769', 400.00, 18, 'FICTION',
    (SELECT id FROM authors WHERE email = 'lev.tolstoy@example.com'))
ON CONFLICT DO NOTHING;

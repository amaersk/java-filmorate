-- Filmorate Database Data Initialization
-- SQL скрипт для инициализации справочных данных

-- Вставка справочных данных для рейтингов MPA (только если не существуют)
INSERT INTO mpa_ratings (code, description) 
SELECT 'G', 'у фильма нет возрастных ограничений' WHERE NOT EXISTS (SELECT 1 FROM mpa_ratings WHERE code = 'G')
UNION ALL
SELECT 'PG', 'детям рекомендуется смотреть фильм с родителями' WHERE NOT EXISTS (SELECT 1 FROM mpa_ratings WHERE code = 'PG')
UNION ALL
SELECT 'PG-13', 'детям до 13 лет просмотр не желателен' WHERE NOT EXISTS (SELECT 1 FROM mpa_ratings WHERE code = 'PG-13')
UNION ALL
SELECT 'R', 'лицам до 17 лет просматривать фильм можно только в присутствии взрослого' WHERE NOT EXISTS (SELECT 1 FROM mpa_ratings WHERE code = 'R')
UNION ALL
SELECT 'NC-17', 'лицам до 18 лет просмотр запрещён' WHERE NOT EXISTS (SELECT 1 FROM mpa_ratings WHERE code = 'NC-17');

-- Вставка справочных данных для жанров (только если не существуют)
INSERT INTO genres (name) 
SELECT 'Комедия' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Комедия')
UNION ALL
SELECT 'Драма' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Драма')
UNION ALL
SELECT 'Мультфильм' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Мультфильм')
UNION ALL
SELECT 'Триллер' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Триллер')
UNION ALL
SELECT 'Документальный' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Документальный')
UNION ALL
SELECT 'Боевик' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Боевик');

-- Создаем тестовый фильм для Postman тестов (только если не существует)
INSERT INTO films (name, description, release_date, duration, mpa_rating_id) 
SELECT 'Test Film', 'Test Description', '2020-01-01', 120, 1 
WHERE NOT EXISTS (SELECT 1 FROM films WHERE id = 4);
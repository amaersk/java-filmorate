-- Filmorate Database Schema
-- SQL скрипт для создания таблиц базы данных Filmorate

-- Создание таблицы пользователей
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    login VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    birthday DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для таблицы users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_login ON users(login);

-- Создание таблицы рейтингов MPA
CREATE TABLE mpa_ratings (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы жанров
CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы фильмов
CREATE TABLE films (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(200),
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL,
    mpa_rating_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mpa_rating_id) REFERENCES mpa_ratings(id)
);

-- Создание индексов для таблицы films
CREATE INDEX idx_films_release_date ON films(release_date);
CREATE INDEX idx_films_mpa_rating_id ON films(mpa_rating_id);

-- Создание таблицы связей фильмов с жанрами (многие ко многим)
CREATE TABLE film_genres (
    id SERIAL PRIMARY KEY,
    film_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE,
    UNIQUE(film_id, genre_id)
);

-- Создание индексов для таблицы film_genres
CREATE INDEX idx_film_genres_film_id ON film_genres(film_id);
CREATE INDEX idx_film_genres_genre_id ON film_genres(genre_id);

-- Создание таблицы дружбы между пользователями
CREATE TABLE user_friends (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    friend_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, friend_id),
    CHECK (user_id != friend_id)
);

-- Создание индексов для таблицы user_friends
CREATE INDEX idx_user_friends_user_id ON user_friends(user_id);
CREATE INDEX idx_user_friends_friend_id ON user_friends(friend_id);
CREATE INDEX idx_user_friends_status ON user_friends(status);

-- Создание таблицы лайков фильмов
CREATE TABLE film_likes (
    id SERIAL PRIMARY KEY,
    film_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(film_id, user_id)
);

-- Создание индексов для таблицы film_likes
CREATE INDEX idx_film_likes_film_id ON film_likes(film_id);
CREATE INDEX idx_film_likes_user_id ON film_likes(user_id);

-- Вставка справочных данных для рейтингов MPA
INSERT INTO mpa_ratings (code, description) VALUES
('G', 'у фильма нет возрастных ограничений'),
('PG', 'детям рекомендуется смотреть фильм с родителями'),
('PG-13', 'детям до 13 лет просмотр не желателен'),
('R', 'лицам до 17 лет просматривать фильм можно только в присутствии взрослого'),
('NC-17', 'лицам до 18 лет просмотр запрещён');

-- Вставка справочных данных для жанров
INSERT INTO genres (name) VALUES
('Комедия'),
('Драма'),
('Мультфильм'),
('Триллер'),
('Документальный'),
('Боевик');

-- Создание представления для популярных фильмов
CREATE VIEW popular_films AS
SELECT 
    f.id as film_id,
    f.name as film_name,
    COUNT(fl.user_id) as likes_count,
    mr.code as mpa_rating_code
FROM films f
LEFT JOIN film_likes fl ON f.id = fl.film_id
LEFT JOIN mpa_ratings mr ON f.mpa_rating_id = mr.id
GROUP BY f.id, f.name, mr.code
ORDER BY likes_count DESC;

-- Создание представления для друзей пользователя с их статусом
CREATE VIEW user_friends_with_status AS
SELECT 
    uf.user_id,
    uf.friend_id,
    u.login as friend_login,
    u.name as friend_name,
    uf.status,
    uf.created_at
FROM user_friends uf
JOIN users u ON uf.friend_id = u.id;

-- Создание представления для фильмов с их жанрами
CREATE VIEW film_genres_list AS
SELECT 
    f.id as film_id,
    f.name as film_name,
    STRING_AGG(g.name, ', ' ORDER BY g.name) as genres,
    mr.code as mpa_rating_code,
    mr.description as mpa_rating_description
FROM films f
LEFT JOIN film_genres fg ON f.id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.id
LEFT JOIN mpa_ratings mr ON f.mpa_rating_id = mr.id
GROUP BY f.id, f.name, mr.code, mr.description;

-- Filmorate Database Schema for H2
-- SQL скрипт для создания таблиц базы данных Filmorate

-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    login VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    birthday DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для таблицы users
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_login ON users(login);

-- Создание таблицы рейтингов MPA
CREATE TABLE IF NOT EXISTS mpa_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы жанров
CREATE TABLE IF NOT EXISTS genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы фильмов
CREATE TABLE IF NOT EXISTS films (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(200),
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL,
    mpa_rating_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mpa_rating_id) REFERENCES mpa_ratings(id)
);

-- Создание индексов для таблицы films
CREATE INDEX IF NOT EXISTS idx_films_release_date ON films(release_date);
CREATE INDEX IF NOT EXISTS idx_films_mpa_rating_id ON films(mpa_rating_id);

-- Создание таблицы связей фильмов с жанрами (многие ко многим)
CREATE TABLE IF NOT EXISTS film_genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    film_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE,
    UNIQUE(film_id, genre_id)
);

-- Создание индексов для таблицы film_genres
CREATE INDEX IF NOT EXISTS idx_film_genres_film_id ON film_genres(film_id);
CREATE INDEX IF NOT EXISTS idx_film_genres_genre_id ON film_genres(genre_id);

-- Создание таблицы дружбы между пользователями
CREATE TABLE IF NOT EXISTS user_friends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, friend_id),
    CHECK (user_id != friend_id)
);

-- Создание индексов для таблицы user_friends
CREATE INDEX IF NOT EXISTS idx_user_friends_user_id ON user_friends(user_id);
CREATE INDEX IF NOT EXISTS idx_user_friends_friend_id ON user_friends(friend_id);
CREATE INDEX IF NOT EXISTS idx_user_friends_status ON user_friends(status);

-- Создание таблицы лайков фильмов
CREATE TABLE IF NOT EXISTS film_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    film_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(film_id, user_id)
);

-- Создание индексов для таблицы film_likes
CREATE INDEX IF NOT EXISTS idx_film_likes_film_id ON film_likes(film_id);
CREATE INDEX IF NOT EXISTS idx_film_likes_user_id ON film_likes(user_id);

-- Создание представления для популярных фильмов
CREATE VIEW IF NOT EXISTS popular_films AS
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
CREATE VIEW IF NOT EXISTS user_friends_with_status AS
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
CREATE VIEW IF NOT EXISTS film_genres_list AS
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

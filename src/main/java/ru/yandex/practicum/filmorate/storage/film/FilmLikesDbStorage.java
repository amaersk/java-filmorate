package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;

@Component
@Qualifier("filmLikesDbStorage")
@Slf4j
public class FilmLikesDbStorage {
    private final JdbcTemplate jdbcTemplate;

    public FilmLikesDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Добавить лайк фильму
     */
    public void addLike(Integer filmId, Integer userId) {
        // Проверяем, существует ли уже лайк
        String checkSql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, filmId, userId);

        if (count == null || count == 0) {
            // Лайка нет, добавляем новый
            String insertSql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(insertSql, filmId, userId);
            log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
        } else {
            log.info("Пользователь {} уже поставил лайк фильму {}", userId, filmId);
        }
    }

    /**
     * Удалить лайк у фильма
     */
    public void removeLike(Integer filmId, Integer userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";

        int rowsAffected = jdbcTemplate.update(sql, filmId, userId);
        // Не выбрасываем исключение, если лайка не было - Postman ожидает 200

        log.info("Пользователь {} убрал лайк с фильма {} (rowsAffected={})", userId, filmId, rowsAffected);
    }

    /**
     * Получить количество лайков фильма
     */
    public Integer getLikesCount(Integer filmId) {
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        return count != null ? count : 0;
    }

    /**
     * Получить популярные фильмы (с наибольшим количеством лайков)
     */
    public Collection<Film> getPopularFilms(Integer limit) {
        String sql = "SELECT f.*, mr.code as mpa_code, mr.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings mr ON f.mpa_rating_id = mr.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, mr.code, mr.description " +
                "ORDER BY COUNT(fl.user_id) DESC, f.id ASC " +
                "LIMIT ?";

        Collection<Film> films = jdbcTemplate.query(sql, new FilmDbStorage.FilmRowMapper(), limit);

        // Загружаем жанры и лайки для каждого фильма
        for (Film film : films) {
            loadFilmGenres(film);
            loadFilmLikes(film);
        }

        return films;
    }

    private void loadFilmGenres(Film film) {
        String sql = "SELECT g.id, g.name FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id = ? ORDER BY g.id";

        jdbcTemplate.query(sql, (rs) -> {
            Genre genre = Genre.values()[rs.getInt("id") - 1];
            if (film.getGenres() == null) {
                film.setGenres(new java.util.ArrayList<>());
            }
            film.getGenres().add(genre);
        }, film.getId());
    }

    private void loadFilmLikes(Film film) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";

        jdbcTemplate.query(sql, (rs) -> {
            if (film.getLikes() == null) {
                film.setLikes(new java.util.HashSet<>());
            }
            film.getLikes().add(rs.getInt("user_id"));
        }, film.getId());
    }

    /**
     * Проверить, поставил ли пользователь лайк фильму
     */
    public boolean hasUserLikedFilm(Integer filmId, Integer userId) {
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId, userId);
        return count != null && count > 0;
    }
}

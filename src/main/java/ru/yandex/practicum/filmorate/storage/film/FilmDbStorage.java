package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("filmDbStorage")
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Film create(Film film) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", film.getName());
        parameters.put("description", film.getDescription());
        parameters.put("release_date", film.getReleaseDate());
        parameters.put("duration", film.getDuration());
        parameters.put("mpa_rating_id", film.getMpa().ordinal() + 1); // MPA рейтинги начинаются с 1

        Number generatedId = jdbcInsert.executeAndReturnKey(parameters);
        film.setId(generatedId.intValue());

        // Сохраняем жанры фильма
        saveFilmGenres(film);

        log.info("Создан фильм id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, " +
                "mpa_rating_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        int rowsAffected = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().ordinal() + 1,
                film.getId());

        if (rowsAffected == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        // Обновляем жанры фильма
        updateFilmGenres(film);

        log.info("Обновлён фильм id={}, name={}", film.getId(), film.getName());
        return getById(film.getId());
    }

    @Override
    public Film getById(Integer id) {
        String sql = "SELECT f.*, mr.code as mpa_code, mr.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings mr ON f.mpa_rating_id = mr.id " +
                "WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, new FilmRowMapper(), id);
            loadFilmGenres(film);
            loadFilmLikes(film);
            return film;
        } catch (Exception e) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    @Override
    public Collection<Film> getAll() {
        String sql = "SELECT f.*, mr.code as mpa_code, mr.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings mr ON f.mpa_rating_id = mr.id " +
                "ORDER BY f.id";

        Collection<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());

        // Загружаем жанры и лайки для каждого фильма
        for (Film film : films) {
            loadFilmGenres(film);
            loadFilmLikes(film);
        }

        return films;
    }

    private void saveFilmGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(sql, film.getId(), genre.ordinal() + 1);
            }
        }
    }

    private void updateFilmGenres(Film film) {
        // Удаляем старые жанры
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        // Добавляем новые жанры
        saveFilmGenres(film);
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

    public static class FilmRowMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getInt("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));

            LocalDate releaseDate = rs.getDate("release_date") != null ?
                    rs.getDate("release_date").toLocalDate() : null;
            film.setReleaseDate(releaseDate);

            film.setDuration(rs.getInt("duration"));

            // Устанавливаем MPA рейтинг
            String mpaCode = rs.getString("mpa_code");
            if (mpaCode != null) {
                for (MpaRating mpa : MpaRating.values()) {
                    if (mpa.getCode().equals(mpaCode)) {
                        film.setMpa(mpa);
                        break;
                    }
                }
            }

            // Инициализируем пустые списки для жанров и лайков
            film.setGenres(new java.util.ArrayList<>());
            film.setLikes(new java.util.HashSet<>());

            return film;
        }
    }
}

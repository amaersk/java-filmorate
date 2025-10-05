package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmLikesDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmLikesDbStorage filmLikesStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       FilmLikesDbStorage filmLikesStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmLikesStorage = filmLikesStorage;
    }

    public Film create(Film film) {
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        return filmStorage.update(film);
    }

    public Collection<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(Integer id) {
        return filmStorage.getById(id);
    }

    public void addLike(Integer filmId, Integer userId) {
        log.debug("Добавление лайка: фильм={}, пользователь={}", filmId, userId);
        // Проверяем, что пользователь и фильм существуют
        userStorage.getById(userId);
        filmStorage.getById(filmId);
        // Добавляем лайк в БД
        filmLikesStorage.addLike(filmId, userId);
    }

    public void removeLike(Integer filmId, Integer userId) {
        log.debug("Удаление лайка: фильм={}, пользователь={}", filmId, userId);
        // Проверяем, что пользователь и фильм существуют
        userStorage.getById(userId);
        filmStorage.getById(filmId);
        // Удаляем лайк из БД
        filmLikesStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopular(int count) {
        log.debug("Получение популярных фильмов: количество={}", count);
        Collection<Film> popularFilms = filmLikesStorage.getPopularFilms(count);
        return new ArrayList<>(popularFilms);
    }

    // Методы для работы с жанрами
    public List<Genre> getAllGenres() {
        return Arrays.asList(Genre.values());
    }

    public Genre getGenreById(int id) {
        Genre[] genres = Genre.values();
        if (id < 1 || id > genres.length) {
            throw new ru.yandex.practicum.filmorate.exception.NotFoundException("Жанр с id=" + id + " не найден");
        }
        return genres[id - 1];
    }

    // Методы для работы с рейтингом MPA
    public List<MpaRating> getAllMpaRatings() {
        return Arrays.asList(MpaRating.values());
    }

    public MpaRating getMpaRatingById(int id) {
        MpaRating[] ratings = MpaRating.values();
        if (id < 1 || id > ratings.length) {
            throw new ru.yandex.practicum.filmorate.exception.NotFoundException("MPA с id=" + id + " не найден");
        }
        return ratings[id - 1];
    }

}



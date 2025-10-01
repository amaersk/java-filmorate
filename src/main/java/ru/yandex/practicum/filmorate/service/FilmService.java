package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    // Дополнительная валидация бизнес-правил, чтобы тесты работали с замоканным хранилищем
    private static final java.time.LocalDate EARLIEST_RELEASE_DATE = java.time.LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
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

    public Film getById(int id) {
        return filmStorage.getById(id);
    }

    public void addLike(int filmId, int userId) {
        log.debug("Добавление лайка: фильм={}, пользователь={}", filmId, userId);
        userStorage.getById(userId);
        Film film = filmStorage.getById(filmId);
        film.getLikes().add(userId);
    }

    public void removeLike(int filmId, int userId) {
        log.debug("Удаление лайка: фильм={}, пользователь={}", filmId, userId);
        userStorage.getById(userId);
        Film film = filmStorage.getById(filmId);
        film.getLikes().remove(userId);
    }

    public List<Film> getPopular(int count) {
        Collection<Film> all = filmStorage.getAll();
        log.debug("Получение популярных фильмов: количество={}, в хранилище={}", count, all.size());
        return all.stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public Film validateAndCreate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не раньше 28 декабря 1895 года");
        }
        if (film.getDuration() != null && film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительной");
        }
        return create(film);
    }
}



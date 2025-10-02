package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public Film getById(Integer id) {
        return filmStorage.getById(id);
    }

    public void addLike(Integer filmId, Integer userId) {
        log.debug("Добавление лайка: фильм={}, пользователь={}", filmId, userId);
        userStorage.getById(userId);
        Film film = filmStorage.getById(filmId);
        film.getLikes().add(userId);
    }

    public void removeLike(Integer filmId, Integer userId) {
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

}



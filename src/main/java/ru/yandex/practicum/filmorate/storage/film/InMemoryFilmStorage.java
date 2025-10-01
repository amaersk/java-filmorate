package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @Override
    public Film create(Film film) {
        validateOnCreate(film);
        int id = idSequence.incrementAndGet();
        film.setId(id);
        films.put(id, film);
        log.info("Создан фильм id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        int id = film.getId();
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        Film existing = films.get(id);
        merge(existing, film);
        log.info("Обновлён фильм id={}, name={}", existing.getId(), existing.getName());
        return existing;
    }

    @Override
    public Film getById(int id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        return film;
    }

    @Override
    public Collection<Film> getAll() {
        return new ArrayList<>(films.values());
    }

    private void validateOnCreate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не раньше 28 декабря 1895 года");
        }
    }

    private void validateRelease(LocalDate date) {
        if (date != null && date.isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не раньше 28 декабря 1895 года");
        }
    }

    private void merge(Film existing, Film incoming) {
        if (incoming.getName() != null && !incoming.getName().isBlank()) {
            existing.setName(incoming.getName());
        }
        if (incoming.getDescription() != null) {
            existing.setDescription(incoming.getDescription());
        }
        if (incoming.getReleaseDate() != null) {
            validateRelease(incoming.getReleaseDate());
            existing.setReleaseDate(incoming.getReleaseDate());
        }
        if (incoming.getDuration() != null) {
            if (incoming.getDuration() <= 0) {
                throw new ValidationException("Продолжительность фильма должна быть положительной");
            }
            existing.setDuration(incoming.getDuration());
        }
    }
}



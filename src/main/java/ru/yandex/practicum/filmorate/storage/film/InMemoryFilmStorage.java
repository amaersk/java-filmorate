package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @Override
    public Film create(Film film) {
        int id = idSequence.incrementAndGet();
        film.setId(id);
        films.put(id, film);
        log.info("Создан фильм id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        Integer id = film.getId();
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        Film existing = films.get(id);
        merge(existing, film);
        log.info("Обновлён фильм id={}, name={}", existing.getId(), existing.getName());
        return existing;
    }

    @Override
    public Film getById(Integer id) {
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


    private void merge(Film existing, Film incoming) {
        if (incoming.getName() != null && !incoming.getName().isBlank()) {
            existing.setName(incoming.getName());
        }
        if (incoming.getDescription() != null) {
            existing.setDescription(incoming.getDescription());
        }
        if (incoming.getReleaseDate() != null) {
            existing.setReleaseDate(incoming.getReleaseDate());
        }
        if (incoming.getDuration() != null) {
            existing.setDuration(incoming.getDuration());
        }
    }
}



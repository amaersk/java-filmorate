package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmUpdateDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.validation.CreateGroup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Slf4j
@RequestMapping("/films")
public class FilmController {
    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @PostMapping
    public Film addFilm(@Validated(CreateGroup.class) @RequestBody Film film) {
        validateFilmReleaseDate(film);
        int id = idSequence.incrementAndGet();
        film.setId(id);
        films.put(id, film);
        log.info("Создан фильм id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody FilmUpdateDto filmDto) {
        int id = filmDto.getId();
        if (!films.containsKey(id)) {
            log.warn("Попытка обновить несуществующий фильм id={}", id);
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        Film existingFilm = films.get(id);
        updateFilmFields(existingFilm, filmDto);

        log.info("Обновлён фильм id={}, name={}", existingFilm.getId(), existingFilm.getName());
        return existingFilm;
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    private void validateFilmReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            log.warn("Валидация фильма не пройдена: дата релиза {} раньше {}", film.getReleaseDate(), EARLIEST_RELEASE_DATE);
            throw new ValidationException("Дата релиза не раньше 28 декабря 1895 года");
        }
    }

    private void updateFilmFields(Film existingFilm, FilmUpdateDto newFilm) {
        if (newFilm.getName() != null && !newFilm.getName().isBlank()) {
            existingFilm.setName(newFilm.getName());
        }
        if (newFilm.getDescription() != null) {
            // Валидация длины описания
            if (newFilm.getDescription().length() > 200) {
                log.warn("Валидация фильма не пройдена: описание длиннее 200 символов");
                throw new ValidationException("Максимальная длина описания — 200 символов");
            }
            existingFilm.setDescription(newFilm.getDescription());
        }
        if (newFilm.getReleaseDate() != null) {
            // Создаем временный объект Film для валидации даты
            Film tempFilm = new Film();
            tempFilm.setReleaseDate(newFilm.getReleaseDate());
            validateFilmReleaseDate(tempFilm);
            existingFilm.setReleaseDate(newFilm.getReleaseDate());
        }
        if (newFilm.getDuration() > 0) {
            existingFilm.setDuration(newFilm.getDuration());
        }
    }
}

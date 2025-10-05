package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmRequestDto;
import ru.yandex.practicum.filmorate.dto.FilmResponseDto;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.validation.CreateGroup;
import ru.yandex.practicum.filmorate.validation.UpdateGroup;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {
    private final FilmService filmService;
    private final FilmMapper filmMapper;

    @PostMapping
    public FilmResponseDto addFilm(@Validated(CreateGroup.class) @RequestBody FilmRequestDto filmDto) {
        Film film = filmMapper.toFilm(filmDto);
        Film createdFilm = filmService.create(film);
        log.info("Создан фильм id={}, name={}", createdFilm.getId(), createdFilm.getName());
        return filmMapper.toFilmResponseDto(createdFilm);
    }

    @PutMapping
    public FilmResponseDto updateFilm(@Validated(UpdateGroup.class) @RequestBody FilmRequestDto filmDto) {
        Film film = filmMapper.toFilm(filmDto);
        Film updatedFilm = filmService.update(film);
        log.info("Обновлён фильм id={}, name={}", updatedFilm.getId(), updatedFilm.getName());
        return filmMapper.toFilmResponseDto(updatedFilm);
    }

    @GetMapping
    public Collection<FilmResponseDto> getAllFilms() {
        return filmService.getAll().stream()
                .map(filmMapper::toFilmResponseDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public FilmResponseDto getFilm(@PathVariable Integer id) {
        return filmMapper.toFilmResponseDto(filmService.getById(id));
    }

    @GetMapping("/popular")
    public List<FilmResponseDto> getPopular(@RequestParam(defaultValue = "10") int count) {
        return filmService.getPopular(count).stream()
                .map(filmMapper::toFilmResponseDto)
                .collect(Collectors.toList());
    }

    // Методы для работы с жанрами
    @GetMapping("/genres")
    public List<GenreDto> getAllGenres() {
        return filmService.getAllGenres().stream()
                .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/genres/{id}")
    public GenreDto getGenre(@PathVariable int id) {
        ru.yandex.practicum.filmorate.model.Genre genre = filmService.getGenreById(id);
        return new GenreDto(genre.getId(), genre.getName());
    }

    // Методы для работы с рейтингом MPA
    @GetMapping("/mpa")
    public List<MpaDto> getAllMpaRatings() {
        return filmService.getAllMpaRatings().stream()
                .map(mpa -> new MpaDto(mpa.getId(), mpa.getCode()))
                .collect(Collectors.toList());
    }

    @GetMapping("/mpa/{id}")
    public MpaDto getMpaRating(@PathVariable int id) {
        ru.yandex.practicum.filmorate.model.MpaRating mpa = filmService.getMpaRatingById(id);
        return new MpaDto(mpa.getId(), mpa.getCode());
    }

    // Методы для работы с лайками
    @PutMapping("/{filmId}/like/{userId}")
    public void addLike(@PathVariable Integer filmId, @PathVariable Integer userId) {
        filmService.addLike(filmId, userId);
    }

    @DeleteMapping("/{filmId}/like/{userId}")
    public void removeLike(@PathVariable Integer filmId, @PathVariable Integer userId) {
        filmService.removeLike(filmId, userId);
    }

}

package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmRequestDto;
import ru.yandex.practicum.filmorate.dto.FilmResponseDto;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.validation.CreateGroup;
import ru.yandex.practicum.filmorate.validation.UpdateGroup;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public FilmResponseDto addFilm(@Validated(CreateGroup.class) @RequestBody FilmRequestDto filmDto) {
        Film film = convertToFilm(filmDto);
        Film createdFilm = filmService.create(film);
        log.info("Создан фильм id={}, name={}", createdFilm.getId(), createdFilm.getName());
        return FilmResponseDto.fromFilm(createdFilm);
    }

    @PutMapping
    public FilmResponseDto updateFilm(@Validated(UpdateGroup.class) @RequestBody FilmRequestDto filmDto) {
        Film film = convertToFilm(filmDto);
        Film updatedFilm = filmService.update(film);
        log.info("Обновлён фильм id={}, name={}", updatedFilm.getId(), updatedFilm.getName());
        return FilmResponseDto.fromFilm(updatedFilm);
    }

    @GetMapping
    public Collection<FilmResponseDto> getAllFilms() {
        return filmService.getAll().stream()
                .map(FilmResponseDto::fromFilm)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public FilmResponseDto getFilm(@PathVariable Integer id) {
        return FilmResponseDto.fromFilm(filmService.getById(id));
    }


    @GetMapping("/popular")
    public List<FilmResponseDto> getPopular(@RequestParam(defaultValue = "10") int count) {
        return filmService.getPopular(count).stream()
                .map(FilmResponseDto::fromFilm)
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
        Genre genre = filmService.getGenreById(id);
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
        MpaRating mpa = filmService.getMpaRatingById(id);
        return new MpaDto(mpa.getId(), mpa.getCode());
    }

    private Film convertToFilm(FilmRequestDto dto) {
        Film film = new Film();
        film.setId(dto.getId());
        film.setName(dto.getName());
        film.setDescription(dto.getDescription());
        film.setReleaseDate(dto.getReleaseDate());
        film.setDuration(dto.getDuration());

        // Конвертируем MPA рейтинг по ID
        if (dto.getMpa() != null) {
            MpaRating[] ratings = MpaRating.values();
            if (dto.getMpa().getId() < 1 || dto.getMpa().getId() > ratings.length) {
                throw new NotFoundException("MPA с id=" + dto.getMpa().getId() + " не найден");
            }
            film.setMpa(ratings[dto.getMpa().getId() - 1]);
        }

        // Конвертируем жанры по ID, удаляем дубликаты и сортируем по ID
        if (dto.getGenres() != null) {
            List<Genre> genres = dto.getGenres().stream()
                    .map(genreDto -> {
                        Genre[] genreArray = Genre.values();
                        if (genreDto.getId() < 1 || genreDto.getId() > genreArray.length) {
                            throw new NotFoundException("Жанр с id=" + genreDto.getId() + " не найден");
                        }
                        return genreArray[genreDto.getId() - 1];
                    })
                    .distinct() // Удаляем дубликаты
                    .sorted((g1, g2) -> Integer.compare(g1.getId(), g2.getId())) // Сортируем по ID
                    .collect(Collectors.toList());
            film.setGenres(genres);
        }

        return film;
    }
}

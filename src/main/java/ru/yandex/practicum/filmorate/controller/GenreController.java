package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
public class GenreController {

    private final FilmService filmService;

    @GetMapping
    public List<GenreDto> getAll() {
        return filmService.getAllGenres().stream()
                .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{id}")
    public GenreDto getById(@PathVariable int id) {
        ru.yandex.practicum.filmorate.model.Genre genre = filmService.getGenreById(id);
        return new GenreDto(genre.getId(), genre.getName());
    }

}



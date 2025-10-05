package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/genres")
public class GenreController {

    @GetMapping
    public List<GenreDto> getAll() {
        List<GenreDto> list = new ArrayList<>();
        Genre[] genres = Genre.values();
        for (int i = 0; i < genres.length; i++) {
            list.add(new GenreDto(i + 1, genres[i].getName()));
        }
        return list;
    }

    @GetMapping("/{id}")
    public GenreDto getById(@PathVariable int id) {
        Genre[] genres = Genre.values();
        if (id < 1 || id > genres.length) {
            throw new NotFoundException("Жанр с id=" + id + " не найден");
        }
        return new GenreDto(id, genres[id - 1].getName());
    }

    public static class GenreDto {
        private final int id;
        private final String name;

        public GenreDto(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}



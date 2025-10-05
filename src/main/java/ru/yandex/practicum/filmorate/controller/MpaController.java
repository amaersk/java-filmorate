package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/mpa")
public class MpaController {

    @GetMapping
    public List<MpaDto> getAll() {
        List<MpaDto> list = new ArrayList<>();
        MpaRating[] ratings = MpaRating.values();
        for (int i = 0; i < ratings.length; i++) {
            list.add(new MpaDto(i + 1, ratings[i].getCode()));
        }
        return list;
    }

    @GetMapping("/{id}")
    public MpaDto getById(@PathVariable int id) {
        MpaRating[] ratings = MpaRating.values();
        if (id < 1 || id > ratings.length) {
            throw new NotFoundException("MPA с id=" + id + " не найден");
        }
        return new MpaDto(id, ratings[id - 1].getCode());
    }

}



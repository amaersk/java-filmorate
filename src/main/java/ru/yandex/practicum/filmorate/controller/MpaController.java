package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    private final FilmService filmService;

    @GetMapping
    public List<MpaDto> getAll() {
        return filmService.getAllMpaRatings().stream()
                .map(mpa -> new MpaDto(mpa.getId(), mpa.getCode()))
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{id}")
    public MpaDto getById(@PathVariable int id) {
        ru.yandex.practicum.filmorate.model.MpaRating mpa = filmService.getMpaRatingById(id);
        return new MpaDto(mpa.getId(), mpa.getCode());
    }

}



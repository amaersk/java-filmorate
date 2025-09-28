package ru.yandex.practicum.filmorate.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * DTO для обновления фильма.
 */
@Data
public class FilmUpdateDto {
    private int id;

    private String name;

    private String description;

    private LocalDate releaseDate;

    private int duration;
}

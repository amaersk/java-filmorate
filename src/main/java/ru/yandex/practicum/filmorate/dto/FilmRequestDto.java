package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.validation.CreateGroup;
import ru.yandex.practicum.filmorate.validation.NotBefore;
import ru.yandex.practicum.filmorate.validation.UpdateGroup;

import java.time.LocalDate;
import java.util.List;

@Data
public class FilmRequestDto {
    @NotNull(groups = {UpdateGroup.class})
    private Integer id;

    @NotBlank(message = "Название фильма не может быть пустым", groups = {CreateGroup.class})
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов", groups = {CreateGroup.class, UpdateGroup.class})
    private String description;

    @NotNull(message = "Дата релиза обязательна", groups = {CreateGroup.class})
    @PastOrPresent(message = "Дата релиза не может быть в будущем", groups = {CreateGroup.class, UpdateGroup.class})
    @NotBefore(value = "1895-12-28", message = "Дата релиза не раньше 28 декабря 1895 года", groups = {CreateGroup.class, UpdateGroup.class})
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительной", groups = {CreateGroup.class, UpdateGroup.class})
    private Integer duration;

    // MPA рейтинг как объект с id и name
    private MpaDto mpa;

    // Жанры как список объектов с id и name
    private List<GenreDto> genres;
}

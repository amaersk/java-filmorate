package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.validation.CreateGroup;
import ru.yandex.practicum.filmorate.validation.UpdateGroup;

import java.time.LocalDate;

/**
 * Фильм.
 */
@Data
public class Film {
    private int id;

    @NotBlank(message = "Название фильма не может быть пустым", groups = {CreateGroup.class})
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов", groups = {CreateGroup.class, UpdateGroup.class})
    private String description;

    @NotNull(message = "Дата релиза обязательна", groups = {CreateGroup.class})
    @PastOrPresent(message = "Дата релиза не может быть в будущем", groups = {CreateGroup.class, UpdateGroup.class})
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительной", groups = {CreateGroup.class, UpdateGroup.class})
    private int duration;
}

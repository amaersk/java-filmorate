package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.validation.CreateGroup;
import ru.yandex.practicum.filmorate.validation.UpdateGroup;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Пользователь.
 */
@Data
public class User {
    @NotNull(groups = {UpdateGroup.class})
    private Integer id;

    @NotBlank(message = "Электронная почта не может быть пустой", groups = {CreateGroup.class})
    @Email(message = "Электронная почта должна содержать символ @ и быть корректной", groups = {CreateGroup.class, UpdateGroup.class})
    private String email;

    @NotBlank(message = "Логин не может быть пустым", groups = {CreateGroup.class})
    @Pattern(regexp = "\\S+", message = "Логин не может содержать пробелы", groups = {CreateGroup.class, UpdateGroup.class})
    private String login;

    private String name;

    @PastOrPresent(message = "Дата рождения не может быть в будущем", groups = {CreateGroup.class, UpdateGroup.class})
    private LocalDate birthday;

    // Друзья пользователя: множество id друзей
    private Set<Integer> friends = new HashSet<>();
}



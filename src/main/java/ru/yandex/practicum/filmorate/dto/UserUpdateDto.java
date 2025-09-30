package ru.yandex.practicum.filmorate.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * DTO для обновления пользователя.
 */
@Data
public class UserUpdateDto {
    private int id;

    private String email;

    private String login;

    private String name;

    private LocalDate birthday;
}

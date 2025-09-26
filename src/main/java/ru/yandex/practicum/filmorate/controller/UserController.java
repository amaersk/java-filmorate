package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @PostMapping
    public User createUser(@RequestBody User user) {
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        int id = idSequence.incrementAndGet();
        user.setId(id);
        users.put(id, user);
        log.info("Создан пользователь id={}, login={}", user.getId(), user.getLogin());
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        int id = user.getId();
        if (!users.containsKey(id)) {
            log.warn("Попытка обновить несуществующего пользователя id={}", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        users.put(id, user);
        log.info("Обновлён пользователь id={}, login={}", user.getId(), user.getLogin());
        return user;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Валидация пользователя не пройдена: некорректный email='{}'", user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @ и не быть пустой");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Валидация пользователя не пройдена: некорректный login='{}'", user.getLogin());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        LocalDate today = LocalDate.now();
        if (user.getBirthday() != null && user.getBirthday().isAfter(today)) {
            log.warn("Валидация пользователя не пройдена: дата рождения {} в будущем", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}



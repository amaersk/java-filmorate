package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validation.CreateGroup;
import ru.yandex.practicum.filmorate.validation.UpdateGroup;

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
    public User createUser(@Validated(CreateGroup.class) @RequestBody User user) {
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
    public User updateUser(@Validated(UpdateGroup.class) @RequestBody User user) {
        int id = user.getId();
        if (!users.containsKey(id)) {
            log.warn("Попытка обновить несуществующего пользователя id={}", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        User existingUser = users.get(id);
        updateUserFields(existingUser, user);

        log.info("Обновлён пользователь id={}, login={}", existingUser.getId(), existingUser.getLogin());
        return existingUser;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    private void updateUserFields(User existingUser, User newUser) {
        if (newUser.getEmail() != null && !newUser.getEmail().isBlank()) {
            existingUser.setEmail(newUser.getEmail());
        }
        if (newUser.getLogin() != null && !newUser.getLogin().isBlank()) {
            existingUser.setLogin(newUser.getLogin());
        }
        if (newUser.getName() != null) {
            if (newUser.getName().isBlank()) {
                existingUser.setName(newUser.getLogin());
            } else {
                existingUser.setName(newUser.getName());
            }
        }
        if (newUser.getBirthday() != null) {
            existingUser.setBirthday(newUser.getBirthday());
        }
    }
}



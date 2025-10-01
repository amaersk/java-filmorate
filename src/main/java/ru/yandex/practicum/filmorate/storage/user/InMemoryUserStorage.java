package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @Override
    public User create(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        validateUser(user);
        int id = idSequence.incrementAndGet();
        user.setId(id);
        users.put(id, user);
        return user;
    }

    @Override
    public User update(User user) {
        int id = user.getId();
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        User existing = users.get(id);
        merge(existing, user);
        return existing;
    }

    @Override
    public User getById(int id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }

    @Override
    public Collection<User> getAll() {
        return new ArrayList<>(users.values());
    }

    private void validateUser(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank() && !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта должна содержать символ @ и быть корректной");
        }
        if (user.getLogin() != null && user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void merge(User existing, User incoming) {
        if (incoming.getEmail() != null && !incoming.getEmail().isBlank()) {
            validateUser(incoming);
            existing.setEmail(incoming.getEmail());
        }
        if (incoming.getLogin() != null && !incoming.getLogin().isBlank()) {
            if (incoming.getLogin().contains(" ")) {
                throw new ValidationException("Логин не может содержать пробелы");
            }
            existing.setLogin(incoming.getLogin());
        }
        if (incoming.getName() != null) {
            if (incoming.getName().isBlank()) {
                existing.setName(incoming.getLogin());
            } else {
                existing.setName(incoming.getName());
            }
        }
        if (incoming.getBirthday() != null) {
            if (incoming.getBirthday().isAfter(LocalDate.now())) {
                throw new ValidationException("Дата рождения не может быть в будущем");
            }
            existing.setBirthday(incoming.getBirthday());
        }
    }
}



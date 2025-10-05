package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Qualifier("inMemoryUserStorage")
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @Override
    public User create(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        int id = idSequence.incrementAndGet();
        user.setId(id);
        users.put(id, user);
        return user;
    }

    @Override
    public User update(User user) {
        Integer id = user.getId();
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        User existing = users.get(id);
        merge(existing, user);
        return existing;
    }

    @Override
    public User getById(Integer id) {
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


    private void merge(User existing, User incoming) {
        if (incoming.getEmail() != null && !incoming.getEmail().isBlank()) {
            existing.setEmail(incoming.getEmail());
        }
        if (incoming.getLogin() != null && !incoming.getLogin().isBlank()) {
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
            existing.setBirthday(incoming.getBirthday());
        }
    }
}



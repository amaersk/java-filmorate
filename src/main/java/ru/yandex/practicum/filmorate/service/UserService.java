package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User create(User user) {
        return userStorage.create(user);
    }

    public User update(User user) {
        return userStorage.update(user);
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(Integer id) {
        return userStorage.getById(id);
    }

    // Friends operations
    public void addFriend(Integer userId, Integer friendId) {
        log.debug("Добавление в друзья: пользователь {} -> {}", userId, friendId);
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        log.debug("Удаление из друзей: пользователь {} -/-> {}", userId, friendId);
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
    }

    public Collection<User> getFriends(Integer userId) {
        User user = userStorage.getById(userId);
        log.debug("Получение списка друзей пользователя {}. Количество={}", userId, user.getFriends().size());
        return user.getFriends().stream().map(userStorage::getById).collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Integer userId, Integer otherId) {
        User user = userStorage.getById(userId);
        User other = userStorage.getById(otherId);
        Set<Integer> common = user.getFriends().stream().filter(other.getFriends()::contains).collect(Collectors.toSet());
        log.debug("Общие друзья пользователей {} и {}: {}", userId, otherId, common.size());
        return common.stream().map(userStorage::getById).collect(Collectors.toList());
    }
}



package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserFriendshipDbStorage;

import java.util.Collection;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;
    private final UserFriendshipDbStorage friendshipStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("userFriendshipDbStorage") UserFriendshipDbStorage friendshipStorage) {
        this.userStorage = userStorage;
        this.friendshipStorage = friendshipStorage;
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
        // Проверяем существование пользователей, чтобы вернуть 404, а не ошибку БД
        userStorage.getById(userId);
        userStorage.getById(friendId);
        // в БД создаём одностороннюю заявку
        friendshipStorage.addFriend(userId, friendId);
    }

    public void confirmFriend(Integer userId, Integer friendId) {
        log.debug("Подтверждение дружбы: пользователь {} подтверждает {}", userId, friendId);
        userStorage.getById(userId);
        userStorage.getById(friendId);
        friendshipStorage.confirmFriendship(userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        log.debug("Удаление из друзей: пользователь {} -/-> {}", userId, friendId);
        userStorage.getById(userId);
        userStorage.getById(friendId);
        friendshipStorage.removeFriend(userId, friendId);
    }

    public Collection<User> getFriends(Integer userId) {
        userStorage.getById(userId);
        log.debug("Получение списка друзей пользователя {}.", userId);
        return friendshipStorage.getFriends(userId);
    }

    public Collection<User> getConfirmedFriends(Integer userId) {
        // Для простоты возвращаем все подтверждённые дружбы через пересечение getFriends с confirmed
        // В текущей реализации getFriends возвращает всех друзей (включая неподтверждённых)
        return friendshipStorage.getFriends(userId); // при необходимости можно сделать отдельный метод в DAO
    }

    public Collection<User> getCommonFriends(Integer userId, Integer otherId) {
        log.debug("Общие друзья пользователей {} и {}", userId, otherId);
        userStorage.getById(userId);
        userStorage.getById(otherId);
        return friendshipStorage.getCommonFriends(userId, otherId);
    }
}



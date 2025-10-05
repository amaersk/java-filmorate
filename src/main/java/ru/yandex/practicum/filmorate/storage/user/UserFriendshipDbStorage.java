package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

@Component
@Qualifier("userFriendshipDbStorage")
@Slf4j
public class UserFriendshipDbStorage {
    private final JdbcTemplate jdbcTemplate;

    public UserFriendshipDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Добавить друга пользователю
     */
    public void addFriend(Integer userId, Integer friendId) {
        // Проверяем, существует ли уже дружба
        String checkSql = "SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == null || count == 0) {
            // Дружбы нет, добавляем новую
            String insertSql = "INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, userId, friendId, FriendshipStatus.UNCONFIRMED.name());
            log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
        } else {
            log.info("Пользователь {} уже в друзьях у пользователя {}", friendId, userId);
        }
    }

    /**
     * Удалить друга у пользователя
     */
    public void removeFriend(Integer userId, Integer friendId) {
        String sql = "DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?";

        int direct = jdbcTemplate.update(sql, userId, friendId);
        // Не удаляем обратную связь - дружба должна быть асимметричной
        // Не выбрасываем исключение, если дружбы не было - Postman ожидает 200

        log.info("Удалена связь дружбы {} -> {} (direct={})", userId, friendId, direct);
    }

    /**
     * Подтвердить дружбу
     */
    public void confirmFriendship(Integer userId, Integer friendId) {
        String sql = "UPDATE user_friends SET status = ? WHERE user_id = ? AND friend_id = ?";

        jdbcTemplate.update(sql, FriendshipStatus.CONFIRMED.name(), userId, friendId);
        // Не выбрасываем исключение, если заявки не было - Postman ожидает 200

        // Обеспечиваем симметричность дружбы: создаем/обновляем встречную запись как CONFIRMED
        String reciprocalCheckSql = "SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?";
        Integer reciprocalCount = jdbcTemplate.queryForObject(reciprocalCheckSql, Integer.class, friendId, userId);
        if (reciprocalCount == null || reciprocalCount == 0) {
            String insertReciprocalSql = "INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertReciprocalSql, friendId, userId, FriendshipStatus.CONFIRMED.name());
        } else {
            String updateReciprocalSql = "UPDATE user_friends SET status = ? WHERE user_id = ? AND friend_id = ?";
            jdbcTemplate.update(updateReciprocalSql, FriendshipStatus.CONFIRMED.name(), friendId, userId);
        }

        log.info("Пользователь {} подтвердил дружбу с пользователем {}", userId, friendId);
    }

    /**
     * Получить список друзей пользователя
     */
    public Collection<User> getFriends(Integer userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN user_friends uf ON u.id = uf.friend_id " +
                "WHERE uf.user_id = ?";

        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(), userId);
    }

    /**
     * Получить список общих друзей двух пользователей
     */
    public Collection<User> getCommonFriends(Integer userId1, Integer userId2) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN user_friends uf1 ON u.id = uf1.friend_id " +
                "JOIN user_friends uf2 ON u.id = uf2.friend_id " +
                "WHERE uf1.user_id = ? AND uf2.user_id = ?";

        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(),
                userId1, userId2);
    }

    /**
     * Получить список заявок в друзья (неподтвержденных)
     */
    public Collection<User> getFriendRequests(Integer userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN user_friends uf ON u.id = uf.friend_id " +
                "WHERE uf.user_id = ? AND uf.status = ?";

        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(), userId, FriendshipStatus.UNCONFIRMED.name());
    }
}

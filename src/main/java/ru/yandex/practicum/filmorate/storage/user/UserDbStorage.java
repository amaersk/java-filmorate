package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("userDbStorage")
@Slf4j
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public User create(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", user.getEmail());
        parameters.put("login", user.getLogin());
        parameters.put("name", user.getName());
        parameters.put("birthday", user.getBirthday());

        Number generatedId = jdbcInsert.executeAndReturnKey(parameters);
        user.setId(generatedId.intValue());

        log.info("Создан пользователь id={}, email={}", user.getId(), user.getEmail());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        int rowsAffected = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());

        if (rowsAffected == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }

        log.info("Обновлён пользователь id={}, email={}", user.getId(), user.getEmail());
        return getById(user.getId());
    }

    @Override
    public User getById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try {
            User user = jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
            loadUserFriends(user);
            return user;
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    @Override
    public Collection<User> getAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        Collection<User> users = jdbcTemplate.query(sql, new UserRowMapper());
        
        // Загружаем друзей для каждого пользователя
        for (User user : users) {
            loadUserFriends(user);
        }
        
        return users;
    }

    private void loadUserFriends(User user) {
        String sql = "SELECT friend_id, status FROM user_friends WHERE user_id = ?";
        
        jdbcTemplate.query(sql, (rs) -> {
            Integer friendId = rs.getInt("friend_id");
            String statusStr = rs.getString("status");
            FriendshipStatus status = FriendshipStatus.valueOf(statusStr);
            if (user.getFriends() == null) {
                user.setFriends(new java.util.HashMap<>());
            }
            user.getFriends().put(friendId, status);
        }, user.getId());
    }

    public static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));
            
            LocalDate birthday = rs.getDate("birthday") != null ? 
                rs.getDate("birthday").toLocalDate() : null;
            user.setBirthday(birthday);
            
            return user;
        }
    }
}

package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
class UserDbStorageTest {
    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // Создаем тестовых пользователей
        testUser1 = new User();
        testUser1.setEmail("user1@example.com");
        testUser1.setLogin("user1");
        testUser1.setName("User One");
        testUser1.setBirthday(LocalDate.of(1990, 1, 1));

        testUser2 = new User();
        testUser2.setEmail("user2@example.com");
        testUser2.setLogin("user2");
        testUser2.setName("User Two");
        testUser2.setBirthday(LocalDate.of(1995, 5, 15));
    }

    @Test
    void testCreateUser() {
        // Создаем пользователя
        User createdUser = userStorage.create(testUser1);

        // Проверяем, что пользователь создан с ID
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("user1@example.com");
        assertThat(createdUser.getLogin()).isEqualTo("user1");
        assertThat(createdUser.getName()).isEqualTo("User One");
        assertThat(createdUser.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));

        // Проверяем, что пользователь сохранен в БД
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, createdUser.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCreateUserWithEmptyName() {
        // Создаем пользователя с пустым именем
        testUser1.setName("");
        User createdUser = userStorage.create(testUser1);

        // Проверяем, что имя установлено как логин
        assertThat(createdUser.getName()).isEqualTo("user1");
    }

    @Test
    void testUpdateUser() {
        // Создаем пользователя
        User createdUser = userStorage.create(testUser1);
        Integer userId = createdUser.getId();

        // Обновляем пользователя
        createdUser.setName("Updated Name");
        createdUser.setEmail("updated@example.com");
        User updatedUser = userStorage.update(createdUser);

        // Проверяем обновления
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getId()).isEqualTo(userId);

        // Проверяем в БД
        String sql = "SELECT name, email FROM users WHERE id = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, userId);
        assertThat(result.get("name")).isEqualTo("Updated Name");
        assertThat(result.get("email")).isEqualTo("updated@example.com");
    }

    @Test
    void testUpdateNonExistentUser() {
        // Пытаемся обновить несуществующего пользователя
        testUser1.setId(999);

        assertThatThrownBy(() -> userStorage.update(testUser1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id=999 не найден");
    }

    @Test
    void testGetUserById() {
        // Создаем пользователя
        User createdUser = userStorage.create(testUser1);
        Integer userId = createdUser.getId();

        // Получаем пользователя по ID
        User retrievedUser = userStorage.getById(userId);

        // Проверяем данные
        assertThat(retrievedUser.getId()).isEqualTo(userId);
        assertThat(retrievedUser.getEmail()).isEqualTo("user1@example.com");
        assertThat(retrievedUser.getLogin()).isEqualTo("user1");
        assertThat(retrievedUser.getName()).isEqualTo("User One");
        assertThat(retrievedUser.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void testGetNonExistentUserById() {
        // Пытаемся получить несуществующего пользователя
        assertThatThrownBy(() -> userStorage.getById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id=999 не найден");
    }

    @Test
    void testGetAllUsers() {
        // Создаем нескольких пользователей
        User user1 = userStorage.create(testUser1);
        User user2 = userStorage.create(testUser2);

        // Получаем всех пользователей
        Collection<User> allUsers = userStorage.getAll();

        // Проверяем количество и содержимое
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(User::getId).contains(user1.getId(), user2.getId());
        assertThat(allUsers).extracting(User::getEmail).contains("user1@example.com", "user2@example.com");
    }

    @Test
    void testGetAllUsersWhenEmpty() {
        // Получаем всех пользователей из пустой БД
        Collection<User> allUsers = userStorage.getAll();

        // Проверяем, что список пуст
        assertThat(allUsers).isEmpty();
    }

    @Test
    void testUserFriendsLoading() {
        // Создаем двух пользователей
        User user1 = userStorage.create(testUser1);
        User user2 = userStorage.create(testUser2);

        // Добавляем дружбу в БД напрямую
        String sql = "INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, user1.getId(), user2.getId(), FriendshipStatus.CONFIRMED.name());

        // Получаем пользователя с друзьями
        User userWithFriends = userStorage.getById(user1.getId());

        // Проверяем, что друзья загружены
        assertThat(userWithFriends.getFriends()).hasSize(1);
        assertThat(userWithFriends.getFriends()).containsEntry(user2.getId(), FriendshipStatus.CONFIRMED);
    }

    @Test
    void testUserWithMultipleFriends() {
        // Создаем трех пользователей
        User user1 = userStorage.create(testUser1);
        User user2 = userStorage.create(testUser2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setLogin("user3");
        user3.setName("User Three");
        user3.setBirthday(LocalDate.of(2000, 12, 25));
        User user3Created = userStorage.create(user3);

        // Добавляем дружбы
        String sql = "INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, user1.getId(), user2.getId(), FriendshipStatus.CONFIRMED.name());
        jdbcTemplate.update(sql, user1.getId(), user3Created.getId(), FriendshipStatus.UNCONFIRMED.name());

        // Получаем пользователя с друзьями
        User userWithFriends = userStorage.getById(user1.getId());

        // Проверяем друзей
        assertThat(userWithFriends.getFriends()).hasSize(2);
        assertThat(userWithFriends.getFriends()).containsEntry(user2.getId(), FriendshipStatus.CONFIRMED);
        assertThat(userWithFriends.getFriends()).containsEntry(user3Created.getId(), FriendshipStatus.UNCONFIRMED);
    }
}

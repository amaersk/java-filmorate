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
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserFriendshipDbStorage.class, UserDbStorage.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
class UserFriendshipDbStorageTest {
    private final UserFriendshipDbStorage friendshipStorage;
    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // Создаем тестовых пользователей
        user1 = createTestUser("user1@example.com", "user1", "User One", LocalDate.of(1990, 1, 1));
        user2 = createTestUser("user2@example.com", "user2", "User Two", LocalDate.of(1995, 5, 15));
        user3 = createTestUser("user3@example.com", "user3", "User Three", LocalDate.of(2000, 12, 25));
    }

    private User createTestUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        return userStorage.create(user);
    }

    @Test
    void testAddFriend() {
        // Добавляем друга
        friendshipStorage.addFriend(user1.getId(), user2.getId());

        // Проверяем, что дружба добавлена в БД
        String sql = "SELECT status FROM user_friends WHERE user_id = ? AND friend_id = ?";
        String status = jdbcTemplate.queryForObject(sql, String.class, user1.getId(), user2.getId());
        assertThat(status).isEqualTo(FriendshipStatus.UNCONFIRMED.name());
    }

    @Test
    void testAddFriendDuplicate() {
        // Добавляем друга дважды
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.addFriend(user1.getId(), user2.getId());

        // Проверяем, что запись одна и статус UNCONFIRMED
        String sql = "SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, user1.getId(), user2.getId());
        assertThat(count).isEqualTo(1);

        String statusSql = "SELECT status FROM user_friends WHERE user_id = ? AND friend_id = ?";
        String status = jdbcTemplate.queryForObject(statusSql, String.class, user1.getId(), user2.getId());
        assertThat(status).isEqualTo(FriendshipStatus.UNCONFIRMED.name());
    }

    @Test
    void testRemoveFriend() {
        // Добавляем друга
        friendshipStorage.addFriend(user1.getId(), user2.getId());

        // Удаляем друга
        friendshipStorage.removeFriend(user1.getId(), user2.getId());

        // Проверяем, что дружба удалена
        String sql = "SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, user1.getId(), user2.getId());
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testRemoveNonExistentFriend() {
        // Пытаемся удалить несуществующую дружбу - должно работать без исключения
        assertThatCode(() -> friendshipStorage.removeFriend(user1.getId(), user2.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void testConfirmFriendship() {
        // Добавляем друга
        friendshipStorage.addFriend(user1.getId(), user2.getId());

        // Подтверждаем дружбу
        friendshipStorage.confirmFriendship(user1.getId(), user2.getId());

        // Проверяем, что статус изменился на CONFIRMED
        String sql = "SELECT status FROM user_friends WHERE user_id = ? AND friend_id = ?";
        String status = jdbcTemplate.queryForObject(sql, String.class, user1.getId(), user2.getId());
        assertThat(status).isEqualTo(FriendshipStatus.CONFIRMED.name());
    }

    @Test
    void testConfirmNonExistentFriendship() {
        // Пытаемся подтвердить несуществующую дружбу - должно работать без исключения
        assertThatCode(() -> friendshipStorage.confirmFriendship(user1.getId(), user2.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void testGetFriends() {
        // Добавляем друзей и подтверждаем дружбу
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.confirmFriendship(user1.getId(), user2.getId());

        friendshipStorage.addFriend(user1.getId(), user3.getId());
        friendshipStorage.confirmFriendship(user1.getId(), user3.getId());

        // Получаем список друзей
        Collection<User> friends = friendshipStorage.getFriends(user1.getId());

        // Проверяем друзей
        assertThat(friends).hasSize(2);
        assertThat(friends).extracting(User::getId).contains(user2.getId(), user3.getId());
        assertThat(friends).extracting(User::getLogin).contains("user2", "user3");
    }

    @Test
    void testGetFriendsEmpty() {
        // Получаем друзей пользователя без друзей
        Collection<User> friends = friendshipStorage.getFriends(user1.getId());

        // Проверяем, что список пуст
        assertThat(friends).isEmpty();
    }

    @Test
    void testGetCommonFriends() {
        // Добавляем друзей для user1
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.confirmFriendship(user1.getId(), user2.getId());

        friendshipStorage.addFriend(user1.getId(), user3.getId());
        friendshipStorage.confirmFriendship(user1.getId(), user3.getId());

        // Добавляем друзей для user2
        friendshipStorage.addFriend(user2.getId(), user3.getId());
        friendshipStorage.confirmFriendship(user2.getId(), user3.getId());

        // Получаем общих друзей
        Collection<User> commonFriends = friendshipStorage.getCommonFriends(user1.getId(), user2.getId());

        // Проверяем общих друзей
        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends).extracting(User::getId).contains(user3.getId());
    }

    @Test
    void testGetCommonFriendsEmpty() {
        // Получаем общих друзей у пользователей без общих друзей
        Collection<User> commonFriends = friendshipStorage.getCommonFriends(user1.getId(), user2.getId());

        // Проверяем, что список пуст
        assertThat(commonFriends).isEmpty();
    }

    @Test
    void testGetFriendRequests() {
        // Добавляем заявки в друзья
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.addFriend(user1.getId(), user3.getId());

        // Получаем заявки в друзья
        Collection<User> requests = friendshipStorage.getFriendRequests(user1.getId());

        // Проверяем заявки
        assertThat(requests).hasSize(2);
        assertThat(requests).extracting(User::getId).contains(user2.getId(), user3.getId());
    }

    @Test
    void testGetFriendRequestsEmpty() {
        // Получаем заявки в друзья у пользователя без заявок
        Collection<User> requests = friendshipStorage.getFriendRequests(user1.getId());

        // Проверяем, что список пуст
        assertThat(requests).isEmpty();
    }

    @Test
    void testBidirectionalFriendship() {
        // Добавляем дружбу в обе стороны
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.addFriend(user2.getId(), user1.getId());

        // Подтверждаем дружбу в обе стороны
        friendshipStorage.confirmFriendship(user1.getId(), user2.getId());
        friendshipStorage.confirmFriendship(user2.getId(), user1.getId());

        // Проверяем, что оба пользователя видят друг друга в друзьях
        Collection<User> user1Friends = friendshipStorage.getFriends(user1.getId());
        Collection<User> user2Friends = friendshipStorage.getFriends(user2.getId());

        assertThat(user1Friends).hasSize(1);
        assertThat(user1Friends).extracting(User::getId).contains(user2.getId());

        assertThat(user2Friends).hasSize(1);
        assertThat(user2Friends).extracting(User::getId).contains(user1.getId());
    }
}

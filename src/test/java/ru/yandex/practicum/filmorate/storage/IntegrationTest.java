package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmLikesDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserFriendshipDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест для проверки работы всех DAO-классов вместе
 */
@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class, FilmDbStorage.class, UserFriendshipDbStorage.class, FilmLikesDbStorage.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
class IntegrationTest {

    @Qualifier("userDbStorage")
    private final UserDbStorage userStorage;

    @Qualifier("filmDbStorage")
    private final FilmDbStorage filmStorage;

    @Qualifier("userFriendshipDbStorage")
    private final UserFriendshipDbStorage friendshipStorage;

    @Qualifier("filmLikesDbStorage")
    private final FilmLikesDbStorage likesStorage;

    private final JdbcTemplate jdbcTemplate;

    private User user1;
    private User user2;
    private Film film1;
    private Film film2;

    @BeforeEach
    void setUp() {
        // Создаем тестовых пользователей
        user1 = createTestUser("user1@example.com", "user1", "User One", LocalDate.of(1990, 1, 1));
        user2 = createTestUser("user2@example.com", "user2", "User Two", LocalDate.of(1995, 5, 15));

        // Создаем тестовые фильмы
        film1 = createTestFilm("Film 1", "Description 1", LocalDate.of(2020, 1, 1), 120, MpaRating.PG_13);
        film2 = createTestFilm("Film 2", "Description 2", LocalDate.of(2021, 6, 15), 90, MpaRating.R);
    }

    private User createTestUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        return userStorage.create(user);
    }

    private Film createTestFilm(String name, String description, LocalDate releaseDate, Integer duration, MpaRating mpa) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(mpa);
        film.setGenres(List.of(Genre.COMEDY, Genre.DRAMA));
        return filmStorage.create(film);
    }

    @Test
    void testCompleteWorkflow() {
        // 1. Создаем дружбу между пользователями
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.confirmFriendship(user1.getId(), user2.getId());

        // 2. Пользователи ставят лайки фильмам
        likesStorage.addLike(film1.getId(), user1.getId());
        likesStorage.addLike(film1.getId(), user2.getId());
        likesStorage.addLike(film2.getId(), user1.getId());

        // 3. Проверяем дружбу
        Collection<User> user1Friends = friendshipStorage.getFriends(user1.getId());
        assertThat(user1Friends).hasSize(1);
        assertThat(user1Friends).extracting(User::getId).contains(user2.getId());

        // 4. Проверяем лайки
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(2);
        assertThat(likesStorage.getLikesCount(film2.getId())).isEqualTo(1);
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), user1.getId())).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), user2.getId())).isTrue();

        // 5. Проверяем популярные фильмы
        Collection<Film> popularFilms = likesStorage.getPopularFilms(2);
        assertThat(popularFilms).hasSize(2);
        Film[] filmsArray = popularFilms.toArray(new Film[0]);
        assertThat(filmsArray[0].getName()).isEqualTo("Film 1"); // 2 лайка
        assertThat(filmsArray[1].getName()).isEqualTo("Film 2"); // 1 лайк

        // 6. Обновляем данные пользователя
        user1.setName("Updated User One");
        User updatedUser = userStorage.update(user1);
        assertThat(updatedUser.getName()).isEqualTo("Updated User One");

        // 7. Обновляем данные фильма
        film1.setName("Updated Film 1");
        film1.setGenres(List.of(Genre.ACTION));
        Film updatedFilm = filmStorage.update(film1);
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film 1");
        assertThat(updatedFilm.getGenres()).containsExactly(Genre.ACTION);

        // 8. Проверяем, что все связи сохранились после обновлений
        assertThat(likesStorage.getLikesCount(updatedFilm.getId())).isEqualTo(2);
        assertThat(friendshipStorage.getFriends(updatedUser.getId())).hasSize(1);
    }


    @Test
    void testCascadeOperations() {
        // Создаем дружбу и лайки
        friendshipStorage.addFriend(user1.getId(), user2.getId());
        friendshipStorage.confirmFriendship(user1.getId(), user2.getId());
        likesStorage.addLike(film1.getId(), user1.getId());
        likesStorage.addLike(film1.getId(), user2.getId());

        // Проверяем, что данные созданы
        assertThat(friendshipStorage.getFriends(user1.getId())).hasSize(1);
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(2);

        // Удаляем пользователя (в реальном приложении это было бы через каскадное удаление)
        String deleteFriendshipSql = "DELETE FROM user_friends WHERE user_id = ? OR friend_id = ?";
        jdbcTemplate.update(deleteFriendshipSql, user1.getId(), user1.getId());

        String deleteLikesSql = "DELETE FROM film_likes WHERE user_id = ?";
        jdbcTemplate.update(deleteLikesSql, user1.getId());

        // Проверяем, что связанные данные удалены
        assertThat(friendshipStorage.getFriends(user1.getId())).isEmpty();
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(1);
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), user1.getId())).isFalse();
    }
}

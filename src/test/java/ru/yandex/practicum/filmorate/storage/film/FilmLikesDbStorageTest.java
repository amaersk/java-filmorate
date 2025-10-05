package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmLikesDbStorage.class, FilmDbStorage.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
class FilmLikesDbStorageTest {
    private final FilmLikesDbStorage likesStorage;
    private final FilmDbStorage filmStorage;
    private final JdbcTemplate jdbcTemplate;

    private Film film1;
    private Film film2;
    private Film film3;
    private Integer userId1;
    private Integer userId2;
    private Integer userId3;
    private Integer userId4;
    private Integer userId5;

    @BeforeEach
    void setUp() {
        // Создаем тестовых пользователей
        String insertUserSql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertUserSql, "user1@test.com", "user1", "Test User 1", "1990-01-01");
        jdbcTemplate.update(insertUserSql, "user2@test.com", "user2", "Test User 2", "1992-05-15");
        jdbcTemplate.update(insertUserSql, "user3@test.com", "user3", "Test User 3", "1995-12-10");
        jdbcTemplate.update(insertUserSql, "user4@test.com", "user4", "Test User 4", "1988-03-20");
        jdbcTemplate.update(insertUserSql, "user5@test.com", "user5", "Test User 5", "1993-07-15");

        // Получаем ID созданных пользователей
        String getUserSql = "SELECT id FROM users WHERE login = ?";
        userId1 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user1");
        userId2 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user2");
        userId3 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user3");
        userId4 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user4");
        userId5 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user5");

        // Создаем тестовые фильмы
        film1 = createTestFilm("Film 1", "Description 1", LocalDate.of(2020, 1, 1), 120, MpaRating.PG_13);
        film2 = createTestFilm("Film 2", "Description 2", LocalDate.of(2021, 6, 15), 90, MpaRating.R);
        film3 = createTestFilm("Film 3", "Description 3", LocalDate.of(2022, 12, 25), 150, MpaRating.G);
    }

    private Film createTestFilm(String name, String description, LocalDate releaseDate, Integer duration, MpaRating mpa) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(mpa);
        return filmStorage.create(film);
    }

    @Test
    void testAddLike() {
        // Добавляем лайк
        likesStorage.addLike(film1.getId(), userId1);

        // Проверяем, что лайк добавлен в БД
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, film1.getId(), userId1);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testAddLikeDuplicate() {
        // Добавляем лайк дважды
        likesStorage.addLike(film1.getId(), userId1);
        likesStorage.addLike(film1.getId(), userId1);

        // Проверяем, что лайк добавлен только один раз
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, film1.getId(), userId1);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testRemoveLike() {
        // Добавляем лайк
        likesStorage.addLike(film1.getId(), userId1);

        // Удаляем лайк
        likesStorage.removeLike(film1.getId(), userId1);

        // Проверяем, что лайк удален
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, film1.getId(), userId1);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testRemoveNonExistentLike() {
        // Пытаемся удалить несуществующий лайк - должно пройти без исключения (идемпотентная операция)
        assertThatCode(() -> likesStorage.removeLike(film1.getId(), userId1))
                .doesNotThrowAnyException();

        // Проверяем, что лайк действительно не существует
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, film1.getId(), userId1);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testGetLikesCount() {
        // Добавляем несколько лайков
        likesStorage.addLike(film1.getId(), userId1);
        likesStorage.addLike(film1.getId(), userId2);
        likesStorage.addLike(film1.getId(), userId3);

        // Получаем количество лайков
        Integer count = likesStorage.getLikesCount(film1.getId());

        // Проверяем количество
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testGetLikesCountZero() {
        // Получаем количество лайков у фильма без лайков
        Integer count = likesStorage.getLikesCount(film1.getId());

        // Проверяем, что количество равно 0
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testGetPopularFilms() {
        // Добавляем лайки фильмам
        likesStorage.addLike(film1.getId(), userId1);
        likesStorage.addLike(film1.getId(), userId2);
        likesStorage.addLike(film1.getId(), userId3);

        likesStorage.addLike(film2.getId(), userId1);
        likesStorage.addLike(film2.getId(), userId2);

        likesStorage.addLike(film3.getId(), userId1);

        // Получаем популярные фильмы
        Collection<Film> popularFilms = likesStorage.getPopularFilms(3);

        // Проверяем порядок и количество
        assertThat(popularFilms).hasSize(3);

        // Проверяем, что фильмы отсортированы по количеству лайков
        Film[] filmsArray = popularFilms.toArray(new Film[0]);
        assertThat(filmsArray[0].getName()).isEqualTo("Film 1"); // 3 лайка
        assertThat(filmsArray[1].getName()).isEqualTo("Film 2"); // 2 лайка
        assertThat(filmsArray[2].getName()).isEqualTo("Film 3"); // 1 лайк
    }

    @Test
    void testGetPopularFilmsWithLimit() {
        // Добавляем лайки всем фильмам
        likesStorage.addLike(film1.getId(), userId1);
        likesStorage.addLike(film2.getId(), userId1);
        likesStorage.addLike(film3.getId(), userId1);

        // Получаем только 2 популярных фильма
        Collection<Film> popularFilms = likesStorage.getPopularFilms(2);

        // Проверяем, что возвращено только 2 фильма
        assertThat(popularFilms).hasSize(2);
    }

    @Test
    void testGetPopularFilmsEmpty() {
        // Получаем популярные фильмы без лайков
        Collection<Film> popularFilms = likesStorage.getPopularFilms(3);

        // Проверяем, что возвращены все фильмы (с 0 лайками)
        assertThat(popularFilms).hasSize(3);
    }

    @Test
    void testHasUserLikedFilm() {
        // Добавляем лайк
        likesStorage.addLike(film1.getId(), userId1);

        // Проверяем, что пользователь поставил лайк
        boolean hasLiked = likesStorage.hasUserLikedFilm(film1.getId(), userId1);
        assertThat(hasLiked).isTrue();

        // Проверяем, что другой пользователь не ставил лайк
        boolean hasNotLiked = likesStorage.hasUserLikedFilm(film1.getId(), userId2);
        assertThat(hasNotLiked).isFalse();
    }

    @Test
    void testHasUserLikedFilmFalse() {
        // Проверяем, что пользователь не ставил лайк
        boolean hasLiked = likesStorage.hasUserLikedFilm(film1.getId(), userId1);
        assertThat(hasLiked).isFalse();
    }

    @Test
    void testMultipleUsersLikingSameFilm() {
        // Несколько пользователей ставят лайк одному фильму
        likesStorage.addLike(film1.getId(), userId1);
        likesStorage.addLike(film1.getId(), userId2);
        likesStorage.addLike(film1.getId(), userId3);
        likesStorage.addLike(film1.getId(), userId4);
        likesStorage.addLike(film1.getId(), userId5);

        // Проверяем количество лайков
        Integer count = likesStorage.getLikesCount(film1.getId());
        assertThat(count).isEqualTo(5);

        // Проверяем, что все пользователи поставили лайк
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId1)).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId2)).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId3)).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId4)).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId5)).isTrue();
    }

    @Test
    void testUserLikingMultipleFilms() {
        // Один пользователь ставит лайк нескольким фильмам
        likesStorage.addLike(film1.getId(), userId1);
        likesStorage.addLike(film2.getId(), userId1);
        likesStorage.addLike(film3.getId(), userId1);

        // Проверяем, что пользователь поставил лайк всем фильмам
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId1)).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film2.getId(), userId1)).isTrue();
        assertThat(likesStorage.hasUserLikedFilm(film3.getId(), userId1)).isTrue();

        // Проверяем количество лайков у каждого фильма
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(1);
        assertThat(likesStorage.getLikesCount(film2.getId())).isEqualTo(1);
        assertThat(likesStorage.getLikesCount(film3.getId())).isEqualTo(1);
    }

    @Test
    void testLikeAndUnlikeCycle() {
        // Добавляем лайк
        likesStorage.addLike(film1.getId(), userId1);
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId1)).isTrue();
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(1);

        // Удаляем лайк
        likesStorage.removeLike(film1.getId(), userId1);
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId1)).isFalse();
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(0);

        // Добавляем лайк снова
        likesStorage.addLike(film1.getId(), userId1);
        assertThat(likesStorage.hasUserLikedFilm(film1.getId(), userId1)).isTrue();
        assertThat(likesStorage.getLikesCount(film1.getId())).isEqualTo(1);
    }
}

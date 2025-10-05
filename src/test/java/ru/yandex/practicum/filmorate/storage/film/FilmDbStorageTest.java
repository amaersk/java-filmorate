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
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.sql.init.mode=embedded",
        "spring.sql.init.data-locations=classpath:data-test.sql"
})
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;
    private final JdbcTemplate jdbcTemplate;

    private Film testFilm1;
    private Film testFilm2;

    @BeforeEach
    void setUp() {
        // Создаем тестовые фильмы
        testFilm1 = new Film();
        testFilm1.setName("Test Film 1");
        testFilm1.setDescription("Description for test film 1");
        testFilm1.setReleaseDate(LocalDate.of(2020, 1, 1));
        testFilm1.setDuration(120);
        testFilm1.setMpa(MpaRating.PG_13);
        testFilm1.setGenres(List.of(Genre.COMEDY, Genre.DRAMA));

        testFilm2 = new Film();
        testFilm2.setName("Test Film 2");
        testFilm2.setDescription("Description for test film 2");
        testFilm2.setReleaseDate(LocalDate.of(2021, 6, 15));
        testFilm2.setDuration(90);
        testFilm2.setMpa(MpaRating.R);
        testFilm2.setGenres(List.of(Genre.ACTION, Genre.THRILLER));
    }

    @Test
    void testCreateFilm() {
        // Создаем фильм
        Film createdFilm = filmStorage.create(testFilm1);

        // Проверяем, что фильм создан с ID
        assertThat(createdFilm.getId()).isNotNull();
        assertThat(createdFilm.getName()).isEqualTo("Test Film 1");
        assertThat(createdFilm.getDescription()).isEqualTo("Description for test film 1");
        assertThat(createdFilm.getReleaseDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(createdFilm.getDuration()).isEqualTo(120);
        assertThat(createdFilm.getMpa()).isEqualTo(MpaRating.PG_13);
        assertThat(createdFilm.getGenres()).containsExactly(Genre.COMEDY, Genre.DRAMA);

        // Проверяем, что фильм сохранен в БД
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, createdFilm.getId());
        assertThat(count).isEqualTo(1);

        // Проверяем жанры в БД
        String genresSql = "SELECT COUNT(*) FROM film_genres WHERE film_id = ?";
        Integer genresCount = jdbcTemplate.queryForObject(genresSql, Integer.class, createdFilm.getId());
        assertThat(genresCount).isEqualTo(2);
    }

    @Test
    void testCreateFilmWithoutGenres() {
        // Создаем фильм без жанров
        testFilm1.setGenres(List.of());
        Film createdFilm = filmStorage.create(testFilm1);

        // Проверяем, что фильм создан
        assertThat(createdFilm.getId()).isNotNull();
        assertThat(createdFilm.getGenres()).isEmpty();

        // Проверяем, что жанры не добавлены в БД
        String genresSql = "SELECT COUNT(*) FROM film_genres WHERE film_id = ?";
        Integer genresCount = jdbcTemplate.queryForObject(genresSql, Integer.class, createdFilm.getId());
        assertThat(genresCount).isEqualTo(0);
    }

    @Test
    void testUpdateFilm() {
        // Создаем фильм
        Film createdFilm = filmStorage.create(testFilm1);
        Integer filmId = createdFilm.getId();

        // Обновляем фильм
        createdFilm.setName("Updated Film Name");
        createdFilm.setDescription("Updated description");
        createdFilm.setGenres(List.of(Genre.ACTION));
        Film updatedFilm = filmStorage.update(createdFilm);

        // Проверяем обновления
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film Name");
        assertThat(updatedFilm.getDescription()).isEqualTo("Updated description");
        assertThat(updatedFilm.getId()).isEqualTo(filmId);
        assertThat(updatedFilm.getGenres()).containsExactly(Genre.ACTION);

        // Проверяем в БД
        String sql = "SELECT name, description FROM films WHERE id = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, filmId);
        assertThat(result.get("name")).isEqualTo("Updated Film Name");
        assertThat(result.get("description")).isEqualTo("Updated description");

        // Проверяем обновленные жанры
        String genresSql = "SELECT COUNT(*) FROM film_genres WHERE film_id = ?";
        Integer genresCount = jdbcTemplate.queryForObject(genresSql, Integer.class, filmId);
        assertThat(genresCount).isEqualTo(1);
    }

    @Test
    void testUpdateNonExistentFilm() {
        // Пытаемся обновить несуществующий фильм
        testFilm1.setId(999);

        assertThatThrownBy(() -> filmStorage.update(testFilm1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Фильм с id=999 не найден");
    }

    @Test
    void testGetFilmById() {
        // Создаем фильм
        Film createdFilm = filmStorage.create(testFilm1);
        Integer filmId = createdFilm.getId();

        // Получаем фильм по ID
        Film retrievedFilm = filmStorage.getById(filmId);

        // Проверяем данные
        assertThat(retrievedFilm.getId()).isEqualTo(filmId);
        assertThat(retrievedFilm.getName()).isEqualTo("Test Film 1");
        assertThat(retrievedFilm.getDescription()).isEqualTo("Description for test film 1");
        assertThat(retrievedFilm.getReleaseDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(retrievedFilm.getDuration()).isEqualTo(120);
        assertThat(retrievedFilm.getMpa()).isEqualTo(MpaRating.PG_13);
        assertThat(retrievedFilm.getGenres()).containsExactly(Genre.COMEDY, Genre.DRAMA);
    }

    @Test
    void testGetNonExistentFilmById() {
        // Пытаемся получить несуществующий фильм
        assertThatThrownBy(() -> filmStorage.getById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Фильм с id=999 не найден");
    }

    @Test
    void testGetAllFilms() {
        // Создаем несколько фильмов
        Film film1 = filmStorage.create(testFilm1);
        Film film2 = filmStorage.create(testFilm2);

        // Получаем все фильмы
        Collection<Film> allFilms = filmStorage.getAll();

        // Проверяем количество и содержимое
        assertThat(allFilms).hasSize(2);
        assertThat(allFilms).extracting(Film::getId).contains(film1.getId(), film2.getId());
        assertThat(allFilms).extracting(Film::getName).contains("Test Film 1", "Test Film 2");
    }

    @Test
    void testGetAllFilmsWhenEmpty() {
        // Получаем все фильмы из пустой БД
        Collection<Film> allFilms = filmStorage.getAll();

        // Проверяем, что список пуст
        assertThat(allFilms).isEmpty();
    }

    @Test
    void testFilmLikesLoading() {
        // Создаем тестовых пользователей
        String insertUserSql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertUserSql, "user1@test.com", "user1", "Test User 1", "1990-01-01");
        jdbcTemplate.update(insertUserSql, "user2@test.com", "user2", "Test User 2", "1992-05-15");

        // Получаем ID созданных пользователей
        String getUserSql = "SELECT id FROM users WHERE login = ?";
        Integer userId1 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user1");
        Integer userId2 = jdbcTemplate.queryForObject(getUserSql, Integer.class, "user2");

        // Создаем фильм
        Film createdFilm = filmStorage.create(testFilm1);
        Integer filmId = createdFilm.getId();

        // Добавляем лайки в БД напрямую
        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId1);
        jdbcTemplate.update(sql, filmId, userId2);

        // Получаем фильм с лайками
        Film filmWithLikes = filmStorage.getById(filmId);

        // Проверяем, что лайки загружены
        assertThat(filmWithLikes.getLikes()).hasSize(2);
        assertThat(filmWithLikes.getLikes()).contains(userId1, userId2);
    }

    @Test
    void testFilmWithMultipleGenres() {
        // Создаем фильм с несколькими жанрами
        testFilm1.setGenres(List.of(Genre.COMEDY, Genre.DRAMA, Genre.ACTION));
        Film createdFilm = filmStorage.create(testFilm1);

        // Получаем фильм
        Film retrievedFilm = filmStorage.getById(createdFilm.getId());

        // Проверяем жанры
        assertThat(retrievedFilm.getGenres()).hasSize(3);
        assertThat(retrievedFilm.getGenres()).containsExactly(Genre.COMEDY, Genre.DRAMA, Genre.ACTION);
    }

    @Test
    void testFilmMpaRatingMapping() {
        // Создаем фильм с разными MPA рейтингами
        testFilm1.setMpa(MpaRating.G);
        Film film1 = filmStorage.create(testFilm1);

        testFilm2.setMpa(MpaRating.NC_17);
        Film film2 = filmStorage.create(testFilm2);

        // Получаем фильмы
        Film retrievedFilm1 = filmStorage.getById(film1.getId());
        Film retrievedFilm2 = filmStorage.getById(film2.getId());

        // Проверяем MPA рейтинги
        assertThat(retrievedFilm1.getMpa()).isEqualTo(MpaRating.G);
        assertThat(retrievedFilm2.getMpa()).isEqualTo(MpaRating.NC_17);
    }
}

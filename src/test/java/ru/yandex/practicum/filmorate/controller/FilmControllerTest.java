package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(FilmService.class)
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FilmStorage filmStorage;

    @MockBean
    private UserStorage userStorage;

    @Test
    @DisplayName("POST /films — 400 при пустом теле запроса")
    void addFilm_emptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при пустом названии")
    void addFilm_emptyName_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", " ",
                "description", "desc",
                "releaseDate", LocalDate.of(2000, 1, 1).toString(),
                "duration", 100
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при длинном описании > 200")
    void addFilm_longDescription_returnsBadRequest() throws Exception {
        String longDesc = "a".repeat(201);
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Name",
                "description", longDesc,
                "releaseDate", LocalDate.of(2000, 1, 1).toString(),
                "duration", 100
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при дате релиза до 1895-12-28")
    void addFilm_tooEarlyDate_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Name",
                "description", "desc",
                "releaseDate", LocalDate.of(1895, 12, 27).toString(),
                "duration", 100
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при неположительной длительности")
    void addFilm_nonPositiveDuration_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Name",
                "description", "desc",
                "releaseDate", LocalDate.of(2000, 1, 1).toString(),
                "duration", 0
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 200 при валидных данных на границе (desc=200, date=1895-12-28, duration=1)")
    void addFilm_validBoundary_returnsOkAndEcho() throws Exception {
        when(filmStorage.create(any(Film.class))).thenAnswer(invocation -> {
            Film f = invocation.getArgument(0);
            Film res = new Film();
            res.setId(1);
            res.setName(f.getName());
            res.setDescription(f.getDescription());
            res.setReleaseDate(f.getReleaseDate());
            res.setDuration(f.getDuration());
            return res;
        });
        String desc200 = "a".repeat(200);
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Name",
                "description", desc200,
                "releaseDate", LocalDate.of(1895, 12, 28).toString(),
                "duration", 1
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Name"));
    }

    @Test
    @DisplayName("GET /films — 200 и массив (может быть пустым)")
    void getFilms_returnsArray() throws Exception {
        when(filmStorage.getAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /films — 200 при обновлении только с id (остальные поля не обновляются)")
    void updateFilm_onlyId_returnsOk() throws Exception {
        when(filmStorage.create(any(Film.class))).thenAnswer(invocation -> {
            Film f = invocation.getArgument(0);
            Film res = new Film();
            res.setId(1);
            res.setName(f.getName());
            res.setDescription(f.getDescription());
            res.setReleaseDate(f.getReleaseDate());
            res.setDuration(f.getDuration());
            return res;
        });
        when(filmStorage.update(any(Film.class))).thenReturn(new Film() {
            {
                setId(1);
                setName("Original Name");
                setDescription("Original Description");
                setReleaseDate(LocalDate.of(2000, 1, 1));
                setDuration(100);
            }
        });
        // Сначала создаем фильм
        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Original Name",
                "description", "Original Description",
                "releaseDate", LocalDate.of(2000, 1, 1).toString(),
                "duration", 100
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // Теперь обновляем только с id
        String updateBody = objectMapper.writeValueAsString(Map.of("id", 1));
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Original Name"))
                .andExpect(jsonPath("$.description").value("Original Description"));
    }

    @Test
    @DisplayName("PUT /films — 200 при обновлении с валидными полями")
    void updateFilm_validFields_returnsOk() throws Exception {
        when(filmStorage.create(any(Film.class))).thenAnswer(invocation -> {
            Film f = invocation.getArgument(0);
            Film res = new Film();
            res.setId(1);
            res.setName(f.getName());
            res.setDescription(f.getDescription());
            res.setReleaseDate(f.getReleaseDate());
            res.setDuration(f.getDuration());
            return res;
        });
        when(filmStorage.update(any(Film.class))).thenReturn(new Film() {
            {
                setId(1);
                setName("Updated Name");
                setDescription("Original Description");
                setReleaseDate(LocalDate.of(2000, 1, 1));
                setDuration(100);
            }
        });
        // Сначала создаем фильм.
        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Original Name",
                "description", "Original Description",
                "releaseDate", LocalDate.of(2000, 1, 1).toString(),
                "duration", 100
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // Теперь обновляем с новым именем
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "id", 1,
                "name", "Updated Name"
        ));
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Original Description"));
    }

    @Test
    @DisplayName("PUT /films — 404 при несуществующем id")
    void updateFilm_nonExistentId_returnsNotFound() throws Exception {
        when(filmStorage.update(any(Film.class))).thenThrow(new NotFoundException("not found"));
        String updateBody = objectMapper.writeValueAsString(Map.of("id", 999));
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }
}



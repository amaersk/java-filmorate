package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dto.FilmRequestDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FilmService filmService;

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
        FilmRequestDto filmDto = new FilmRequestDto();
        filmDto.setName(" ");
        filmDto.setDescription("desc");
        filmDto.setReleaseDate(LocalDate.of(2000, 1, 1));
        filmDto.setDuration(100);
        
        String body = objectMapper.writeValueAsString(filmDto);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при длинном описании > 200")
    void addFilm_longDescription_returnsBadRequest() throws Exception {
        String longDesc = "a".repeat(201);
        FilmRequestDto filmDto = new FilmRequestDto();
        filmDto.setName("Name");
        filmDto.setDescription(longDesc);
        filmDto.setReleaseDate(LocalDate.of(2000, 1, 1));
        filmDto.setDuration(100);
        
        String body = objectMapper.writeValueAsString(filmDto);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при дате релиза до 1895-12-28")
    void addFilm_tooEarlyDate_returnsBadRequest() throws Exception {
        FilmRequestDto filmDto = new FilmRequestDto();
        filmDto.setName("Name");
        filmDto.setDescription("desc");
        filmDto.setReleaseDate(LocalDate.of(1895, 12, 27));
        filmDto.setDuration(100);
        
        String body = objectMapper.writeValueAsString(filmDto);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 400 при неположительной длительности")
    void addFilm_nonPositiveDuration_returnsBadRequest() throws Exception {
        FilmRequestDto filmDto = new FilmRequestDto();
        filmDto.setName("Name");
        filmDto.setDescription("desc");
        filmDto.setReleaseDate(LocalDate.of(2000, 1, 1));
        filmDto.setDuration(0);
        
        String body = objectMapper.writeValueAsString(filmDto);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films — 200 при валидных данных на границе (desc=200, date=1895-12-28, duration=1)")
    void addFilm_validBoundary_returnsOkAndEcho() throws Exception {
        when(filmService.create(any(Film.class))).thenAnswer(invocation -> {
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
        FilmRequestDto filmDto = new FilmRequestDto();
        filmDto.setName("Name");
        filmDto.setDescription(desc200);
        filmDto.setReleaseDate(LocalDate.of(1895, 12, 28));
        filmDto.setDuration(1);
        
        String body = objectMapper.writeValueAsString(filmDto);
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
        when(filmService.getAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /films — 200 при обновлении только с id (остальные поля не обновляются)")
    void updateFilm_onlyId_returnsOk() throws Exception {
        when(filmService.create(any(Film.class))).thenAnswer(invocation -> {
            Film f = invocation.getArgument(0);
            Film res = new Film();
            res.setId(1);
            res.setName(f.getName());
            res.setDescription(f.getDescription());
            res.setReleaseDate(f.getReleaseDate());
            res.setDuration(f.getDuration());
            return res;
        });
        Film film = new Film();
        film.setId(1);
        film.setName("Original Name");
        film.setDescription("Original Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        when(filmService.update(any(Film.class))).thenReturn(film);
        // Сначала создаем фильм
        FilmRequestDto createDto = new FilmRequestDto();
        createDto.setName("Original Name");
        createDto.setDescription("Original Description");
        createDto.setReleaseDate(LocalDate.of(2000, 1, 1));
        createDto.setDuration(100);
        
        String createBody = objectMapper.writeValueAsString(createDto);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // Теперь обновляем только с id
        FilmRequestDto updateDto = new FilmRequestDto();
        updateDto.setId(1);
        
        String updateBody = objectMapper.writeValueAsString(updateDto);
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
        when(filmService.create(any(Film.class))).thenAnswer(invocation -> {
            Film f = invocation.getArgument(0);
            Film res = new Film();
            res.setId(1);
            res.setName(f.getName());
            res.setDescription(f.getDescription());
            res.setReleaseDate(f.getReleaseDate());
            res.setDuration(f.getDuration());
            return res;
        });
        Film film = new Film();
        film.setId(1);
        film.setName("Updated Name");
        film.setDescription("Original Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        when(filmService.update(any(Film.class))).thenReturn(film);

        // Сначала создаем фильм
        FilmRequestDto createDto = new FilmRequestDto();
        createDto.setName("Original Name");
        createDto.setDescription("Original Description");
        createDto.setReleaseDate(LocalDate.of(2000, 1, 1));
        createDto.setDuration(100);
        
        String createBody = objectMapper.writeValueAsString(createDto);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // Теперь обновляем с новым именем
        FilmRequestDto updateDto = new FilmRequestDto();
        updateDto.setId(1);
        updateDto.setName("Updated Name");
        
        String updateBody = objectMapper.writeValueAsString(updateDto);
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
        when(filmService.update(any(Film.class))).thenThrow(new NotFoundException("not found"));
        FilmRequestDto updateDto = new FilmRequestDto();
        updateDto.setId(999);
        
        String updateBody = objectMapper.writeValueAsString(updateDto);
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }
}



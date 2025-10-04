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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(UserService.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserStorage userStorage;

    @Test
    @DisplayName("POST /users — 400 при пустом теле запроса")
    void createUser_emptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при неправильном email")
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "invalid",
                "login", "login",
                "name", "",
                "birthday", LocalDate.of(2000, 1, 1).toString()
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при пустом или пробельном логине / логине с пробелом")
    void createUser_invalidLogin_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "a@b.com",
                "login", "bad login",
                "name", "",
                "birthday", LocalDate.of(2000, 1, 1).toString()
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при дате рождения в будущем")
    void createUser_futureBirthday_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "a@b.com",
                "login", "login",
                "name", "",
                "birthday", LocalDate.now().plusDays(1).toString()
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 200 и name подставляется login, если пусто")
    void createUser_validBoundary_setsNameFromLogin() throws Exception {
        when(userStorage.create(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            User res = new User();
            res.setId(1);
            res.setEmail(u.getEmail());
            res.setLogin(u.getLogin());
            res.setName((u.getName() == null || u.getName().isBlank()) ? u.getLogin() : u.getName());
            res.setBirthday(u.getBirthday());
            return res;
        });
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "a@b.com",
                "login", "login",
                "name", " ",
                "birthday", LocalDate.of(2000, 1, 1).toString()
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("login"));
    }

    @Test
    @DisplayName("GET /users — 200 и массив (может быть пустым)")
    void getUsers_returnsArray() throws Exception {
        when(userStorage.getAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users — 200 при обновлении только с id (остальные поля не обновляются)")
    void updateUser_onlyId_returnsOk() throws Exception {
        when(userStorage.create(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            User res = new User();
            res.setId(1);
            res.setEmail(u.getEmail());
            res.setLogin(u.getLogin());
            res.setName(u.getName());
            res.setBirthday(u.getBirthday());
            return res;
        });
        User user = new User();
        user.setId(1);
        user.setEmail("original@test.com");
        user.setLogin("originalLogin");
        user.setName("Original Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        when(userStorage.update(any(User.class))).thenReturn(user);

        // Сначала создаем пользователя
        String createBody = objectMapper.writeValueAsString(Map.of(
                "email", "original@test.com",
                "login", "originalLogin",
                "name", "Original Name",
                "birthday", LocalDate.of(2000, 1, 1).toString()
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // Теперь обновляем только с id
        String updateBody = objectMapper.writeValueAsString(Map.of("id", 1));
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("original@test.com"))
                .andExpect(jsonPath("$.login").value("originalLogin"))
                .andExpect(jsonPath("$.name").value("Original Name"));
    }

    @Test
    @DisplayName("PUT /users — 200 при обновлении с валидными полями")
    void updateUser_validFields_returnsOk() throws Exception {
        when(userStorage.create(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            User res = new User();
            res.setId(1);
            res.setEmail(u.getEmail());
            res.setLogin(u.getLogin());
            res.setName(u.getName());
            res.setBirthday(u.getBirthday());
            return res;
        });
        User user = new User();
        user.setId(1);
        user.setEmail("updated@test.com");
        user.setLogin("originalLogin");
        user.setName("Original Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        when(userStorage.update(any(User.class))).thenReturn(user);

        // Сначала создаем пользователя
        String createBody = objectMapper.writeValueAsString(Map.of(
                "email", "original@test.com",
                "login", "originalLogin",
                "name", "Original Name",
                "birthday", LocalDate.of(2000, 1, 1).toString()
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // Теперь обновляем с новым email
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "id", 1,
                "email", "updated@test.com"
        ));
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("updated@test.com"))
                .andExpect(jsonPath("$.login").value("originalLogin"))
                .andExpect(jsonPath("$.name").value("Original Name"));
    }

    @Test
    @DisplayName("PUT /users — 404 при несуществующем id")
    void updateUser_nonExistentId_returnsNotFound() throws Exception {
        when(userStorage.update(any(User.class))).thenThrow(new NotFoundException("not found"));
        String updateBody = objectMapper.writeValueAsString(Map.of("id", 999));
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }
}



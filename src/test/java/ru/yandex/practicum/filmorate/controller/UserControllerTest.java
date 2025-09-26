package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /users — 400 при пустом теле запроса")
    void createUser_emptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при неправильном email")
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "invalid", "login", "login", "name", "", "birthday", LocalDate.of(2000, 1, 1).toString()));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при пустом или пробельном логине / логине с пробелом")
    void createUser_invalidLogin_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "a@b.com", "login", "bad login", "name", "", "birthday", LocalDate.of(2000, 1, 1).toString()));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при дате рождения в будущем")
    void createUser_futureBirthday_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "a@b.com", "login", "login", "name", "", "birthday", LocalDate.now().plusDays(1).toString()));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 200 и name подставляется login, если пусто")
    void createUser_validBoundary_setsNameFromLogin() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "a@b.com", "login", "login", "name", " ", "birthday", LocalDate.of(2000, 1, 1).toString()));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.name").value("login"));
    }

    @Test
    @DisplayName("GET /users — 200 и массив (может быть пустым)")
    void getUsers_returnsArray() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isOk());
    }
}



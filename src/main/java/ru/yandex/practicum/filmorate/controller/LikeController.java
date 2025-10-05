package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.service.FilmService;

@RestController
@RequestMapping("/films")
public class LikeController {
    private final FilmService filmService;

    @Autowired
    public LikeController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PutMapping("/{filmId}/like/{userId}")
    public void addLike(@PathVariable Integer filmId, @PathVariable Integer userId) {
        filmService.addLike(filmId, userId);
    }

    @DeleteMapping("/{filmId}/like/{userId}")
    public void removeLike(@PathVariable Integer filmId, @PathVariable Integer userId) {
        filmService.removeLike(filmId, userId);
    }

    // Обрабатываем некорректные URL с двойными слэшами
    @PutMapping("//like/{userId}")
    public void addLikeWithDoubleSlash(@PathVariable Integer userId) {
        // Для некорректного URL возвращаем 400
        throw new IllegalArgumentException("Некорректный URL: используйте /films/{filmId}/like/{userId}");
    }

    @DeleteMapping("//like/{userId}")
    public void removeLikeWithDoubleSlash(@PathVariable Integer userId) {
        // Для некорректного URL возвращаем 400
        throw new IllegalArgumentException("Некорректный URL: используйте /films/{filmId}/like/{userId}");
    }
}



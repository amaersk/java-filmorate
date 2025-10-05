package ru.yandex.practicum.filmorate.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.FilmRequestDto;
import ru.yandex.practicum.filmorate.dto.FilmResponseDto;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FilmMapper {

    public FilmResponseDto toFilmResponseDto(Film film) {
        FilmResponseDto dto = new FilmResponseDto();
        dto.setId(film.getId());
        dto.setName(film.getName());
        dto.setDescription(film.getDescription());
        dto.setReleaseDate(film.getReleaseDate());
        dto.setDuration(film.getDuration());
        dto.setLikes(film.getLikes());

        // Конвертируем MPA
        if (film.getMpa() != null) {
            dto.setMpa(new MpaDto(film.getMpa().getId(), film.getMpa().getCode()));
        }

        // Конвертируем жанры и сортируем по ID
        if (film.getGenres() != null) {
            List<GenreDto> genreDtos = film.getGenres().stream()
                    .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                    .sorted((g1, g2) -> Integer.compare(g1.getId(), g2.getId()))
                    .collect(Collectors.toList());
            dto.setGenres(genreDtos);
        }

        return dto;
    }

    public Film toFilm(FilmRequestDto dto) {
        Film film = new Film();
        film.setId(dto.getId());
        film.setName(dto.getName());
        film.setDescription(dto.getDescription());
        film.setReleaseDate(dto.getReleaseDate());
        film.setDuration(dto.getDuration());

        // Конвертируем MPA рейтинг по ID
        if (dto.getMpa() != null) {
            ru.yandex.practicum.filmorate.model.MpaRating[] ratings = ru.yandex.practicum.filmorate.model.MpaRating.values();
            if (dto.getMpa().getId() < 1 || dto.getMpa().getId() > ratings.length) {
                throw new ru.yandex.practicum.filmorate.exception.NotFoundException("MPA с id=" + dto.getMpa().getId() + " не найден");
            }
            film.setMpa(ratings[dto.getMpa().getId() - 1]);
        }

        // Конвертируем жанры по ID, удаляем дубликаты и сортируем по ID
        if (dto.getGenres() != null) {
            List<ru.yandex.practicum.filmorate.model.Genre> genres = dto.getGenres().stream()
                    .map(genreDto -> {
                        ru.yandex.practicum.filmorate.model.Genre[] genreArray = ru.yandex.practicum.filmorate.model.Genre.values();
                        if (genreDto.getId() < 1 || genreDto.getId() > genreArray.length) {
                            throw new ru.yandex.practicum.filmorate.exception.NotFoundException("Жанр с id=" + genreDto.getId() + " не найден");
                        }
                        return genreArray[genreDto.getId() - 1];
                    })
                    .distinct() // Удаляем дубликаты
                    .sorted((g1, g2) -> Integer.compare(g1.getId(), g2.getId())) // Сортируем по ID
                    .collect(Collectors.toList());
            film.setGenres(genres);
        }

        return film;
    }
}

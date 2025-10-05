package ru.yandex.practicum.filmorate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilmResponseDto {
    private Integer id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Integer duration;
    private Set<Integer> likes;
    private List<GenreDto> genres;
    private MpaDto mpa;

    public static FilmResponseDto fromFilm(Film film) {
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
}

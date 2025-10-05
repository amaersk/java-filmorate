package ru.yandex.practicum.filmorate.model;

/**
 * Рейтинг Ассоциации кинокомпаний (MPA).
 */
public enum MpaRating {
    G(1, "G", "у фильма нет возрастных ограничений"),
    PG(2, "PG", "детям рекомендуется смотреть фильм с родителями"),
    PG_13(3, "PG-13", "детям до 13 лет просмотр не желателен"),
    R(4, "R", "лицам до 17 лет просматривать фильм можно только в присутствии взрослого"),
    NC_17(5, "NC-17", "лицам до 18 лет просмотр запрещён");

    private final int id;
    private final String code;
    private final String description;

    MpaRating(int id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

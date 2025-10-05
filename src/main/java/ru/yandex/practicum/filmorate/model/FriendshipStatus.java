package ru.yandex.practicum.filmorate.model;

/**
 * Статус дружбы между пользователями.
 */
public enum FriendshipStatus {
    UNCONFIRMED("неподтверждённая"),
    CONFIRMED("подтверждённая");

    private final String description;

    FriendshipStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

package ru.yandex.practicum.filmorate.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NotBeforeValidator implements ConstraintValidator<NotBefore, LocalDate> {
    private LocalDate minDate;

    @Override
    public void initialize(NotBefore constraintAnnotation) {
        String dateString = constraintAnnotation.value();
        this.minDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null values are handled by @NotNull
        }
        return !value.isBefore(minDate);
    }
}


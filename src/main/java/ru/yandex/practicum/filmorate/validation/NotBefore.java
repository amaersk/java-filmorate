package ru.yandex.practicum.filmorate.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotBeforeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBefore {
    String message() default "Дата не может быть раньше указанного значения";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value();
}


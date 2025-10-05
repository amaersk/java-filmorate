package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FilmorateApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Проверяем, что Spring контекст загружается корректно
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void mainApplicationContextLoads() {
        // Дополнительная проверка, что все основные компоненты загружены
        assertThat(applicationContext.getBeanDefinitionCount()).isGreaterThan(0);
    }

}

package com.kenez92.plateplan;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "DATABASE_URL=jdbc:postgresql://127.0.0.1:1/plateplan",
    "DATABASE_USERNAME=dummy",
    "DATABASE_PASSWORD=dummy"
})
class ApplicationTest {

    private final DataSource dataSource;

    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    ApplicationTest(final DataSource dataSource, final EntityManagerFactory entityManagerFactory) {
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Test
    void shouldLoadContext() {
    }

    @Test
    void shouldCreateTheDataSourceAndEntityManagerFactory() {
        assertThat(this.dataSource).isNotNull();
        assertThat(this.entityManagerFactory).isNotNull();
    }

}

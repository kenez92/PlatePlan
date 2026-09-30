package com.kenez92.plateplan.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.core.io.DefaultResourceLoader;

@ExtendWith(MockitoExtension.class)
class LiquibaseConfigurationTest {

    @Test
    void shouldNotThrowWhenTheDatabaseIsUnreachable() throws SQLException {
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));
        final LiquibaseProperties properties = new LiquibaseProperties();
        properties.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        final LiquibaseConfiguration liquibase =
            new LiquibaseConfiguration(dataSource, properties, new DefaultResourceLoader());

        assertThatCode(liquibase::afterPropertiesSet).doesNotThrowAnyException();
    }

}

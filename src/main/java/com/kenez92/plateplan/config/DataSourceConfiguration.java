package com.kenez92.plateplan.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class DataSourceConfiguration {

    private static final String URL_PROPERTY = "spring.datasource.url";
    private static final String USERNAME_PROPERTY = "spring.datasource.username";
    private static final String PASSWORD_PROPERTY = "spring.datasource.password";

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties(final Environment environment) {
        // The binder leaves an unresolved ${...} as literal text; resolving here names the missing variable.
        environment.getRequiredProperty(URL_PROPERTY);
        environment.getRequiredProperty(USERNAME_PROPERTY);
        environment.getRequiredProperty(PASSWORD_PROPERTY);
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(final DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

}

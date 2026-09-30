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

	@Bean
	@Primary
	@ConfigurationProperties("spring.datasource")
	public DataSourceProperties dataSourceProperties(final Environment environment) {
		// The binder leaves an unresolved ${...} as literal text; resolving here names the missing variable.
		environment.getRequiredProperty("spring.datasource.url");
		environment.getRequiredProperty("spring.datasource.username");
		environment.getRequiredProperty("spring.datasource.password");
		return new DataSourceProperties();
	}

	@Bean
	@ConfigurationProperties("spring.datasource.hikari")
	public HikariDataSource dataSource(final DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

}

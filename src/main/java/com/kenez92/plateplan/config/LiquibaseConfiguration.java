package com.kenez92.plateplan.config;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ResourceLoader;

/**
 * Runs the Liquibase update once at start. A failed attempt is logged and skipped, so the
 * application still starts when the database is unreachable.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "spring.liquibase.enabled", matchIfMissing = true)
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseConfiguration extends SpringLiquibase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseConfiguration.class);

    private static final String CONTEXT_DELIMITER = ",";
    private static final String SKIPPED_LOG = "Liquibase migration skipped until the next start: {} (cause: {})";

    public LiquibaseConfiguration(final DataSource dataSource,
                                  final LiquibaseProperties properties,
                                  final ResourceLoader resourceLoader) {
        setDataSource(dataSource);
        setResourceLoader(resourceLoader);
        setChangeLog(properties.getChangeLog());
        setContexts(properties.getContexts() == null
                ? null
                : String.join(CONTEXT_DELIMITER, properties.getContexts()));
        setDefaultSchema(properties.getDefaultSchema());
        setLiquibaseSchema(properties.getLiquibaseSchema());
        setLiquibaseTablespace(properties.getLiquibaseTablespace());
        setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
        setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());
        setShouldRun(properties.isEnabled());
    }

    @Override
    public void afterPropertiesSet() {
        try {
            super.afterPropertiesSet();
        } catch (final Exception exception) {
            LOGGER.warn(SKIPPED_LOG,
                    exception.getClass().getName(),
                    NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName());
        }
    }
}

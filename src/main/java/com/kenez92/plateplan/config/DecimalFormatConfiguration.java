package com.kenez92.plateplan.config;

import java.math.BigDecimal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Binds decimal form fields through {@link DecimalFormatter} so a point and a comma are the same
 * number.
 */
@Configuration(proxyBeanMethods = false)
public class DecimalFormatConfiguration implements WebMvcConfigurer {

    private final DecimalFormatter decimalFormatter;

    public DecimalFormatConfiguration(final DecimalFormatter decimalFormatter) {
        this.decimalFormatter = decimalFormatter;
    }

    @Bean
    public static DecimalFormatter decimalFormatter() {
        return new DecimalFormatter();
    }

    @Override
    public void addFormatters(final FormatterRegistry registry) {
        registry.addFormatterForFieldType(BigDecimal.class, decimalFormatter);
    }
}

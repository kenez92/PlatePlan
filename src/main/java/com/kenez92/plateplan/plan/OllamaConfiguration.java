package com.kenez92.plateplan.plan;

import java.time.Duration;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Ollama Cloud client. The starter has no API-key property, so the Bearer header is set here from
 * {@code OLLAMA_API_KEY}. An empty key still starts the application.
 */
@Configuration(proxyBeanMethods = false)
public class OllamaConfiguration {

    private static final String BASE_URL_PROPERTY = "${spring.ai.ollama.base-url}";
    private static final String API_KEY_PROPERTY = "${OLLAMA_API_KEY:}";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    @Bean
    public OllamaApi ollamaApi(@Value(BASE_URL_PROPERTY) final String baseUrl,
                               @Value(API_KEY_PROPERTY) final String apiKey) {
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(READ_TIMEOUT);
        final RestClient.Builder restClient = RestClient.builder().requestFactory(requestFactory);
        if (!apiKey.isBlank()) {
            restClient.defaultHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + apiKey);
        }
        return OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(restClient)
                .build();
    }
}

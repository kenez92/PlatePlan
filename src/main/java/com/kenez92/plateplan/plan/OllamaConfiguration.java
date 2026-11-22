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
 * Local Ollama client at {@code spring.ai.ollama.base-url}. A non-blank
 * {@code spring.ai.ollama.api-key} is sent as a Bearer header; the local server needs none.
 * An empty key still starts the application. The read timeout is on this RestClient, not on
 * {@code spring.http.clients}. A ten-day answer from the local model can take half an hour.
 */
@Configuration(proxyBeanMethods = false)
public class OllamaConfiguration {

    private static final String BASE_URL_PROPERTY = "${spring.ai.ollama.base-url}";
    private static final String API_KEY_PROPERTY = "${spring.ai.ollama.api-key:}";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration READ_TIMEOUT = Duration.ofMinutes(35);

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

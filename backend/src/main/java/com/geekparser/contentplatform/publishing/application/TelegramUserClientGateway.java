package com.geekparser.contentplatform.publishing.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.geekparser.contentplatform.config.AppProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class TelegramUserClientGateway {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    public TelegramUserClientGateway(AppProperties appProperties, RestTemplateBuilder restTemplateBuilder) {
        this.appProperties = appProperties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isEnabled() {
        return appProperties.telegramUserClient().enabled();
    }

    public boolean isAvailable() {
        if (!isEnabled()) {
            return false;
        }
        try {
            HealthResponse response = restTemplate.getForObject(baseUrl() + "/health", HealthResponse.class);
            return response != null && response.authorized();
        } catch (RestClientException exception) {
            return false;
        }
    }

    public ScheduleResponse schedule(String chatId, String text, OffsetDateTime scheduledAt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text,
                "scheduled_at", scheduledAt.toString()
        );
        try {
            ResponseEntity<ScheduleResponse> response = restTemplate.postForEntity(
                    baseUrl() + "/api/schedule",
                    new HttpEntity<>(body, headers),
                    ScheduleResponse.class
            );
            ScheduleResponse result = response.getBody();
            if (result == null) {
                throw new IllegalStateException("Telegram user client returned empty response");
            }
            return result;
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Native Telegram scheduling failed. Ensure telegram-user-client is running and authorized: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String baseUrl() {
        return appProperties.telegramUserClient().baseUrl();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HealthResponse(String status, boolean authorized, boolean configured) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleResponse(
            @JsonProperty("scheduled_message_id") int scheduledMessageId,
            @JsonProperty("scheduled_at") OffsetDateTime scheduledAt,
            @JsonProperty("chat_id") String chatId
    ) {
    }
}

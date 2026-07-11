package com.geekparser.contentplatform.publishing.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.geekparser.contentplatform.config.AppProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Component
public class TelegramGateway {

    private final WebClient webClient;

    public TelegramGateway(AppProperties appProperties) {
        this.webClient = buildWebClient(appProperties.telegram());
    }

    public TelegramBotInfo validate(String token) {
        JsonNode response = webClient.get()
                .uri("/bot{token}/getMe", token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (!response.path("ok").asBoolean()) {
            throw new IllegalArgumentException("Telegram validation failed");
        }
        JsonNode result = response.path("result");
        return new TelegramBotInfo(result.path("username").asText(), result.path("id").asText());
    }

    public String sendMessage(String token, String chatId, String message) {
        return sendMessage(token, chatId, message, null, false);
    }

    public String sendMessage(String token, String chatId, String message, String parseMode) {
        return sendMessage(token, chatId, message, parseMode, false);
    }

    public String sendMessage(String token, String chatId, String message, String parseMode, boolean disableNotification) {
        Object body = buildSendMessageBody(chatId, message, parseMode, disableNotification);
        JsonNode response = webClient.post()
                .uri("/bot{token}/sendMessage", token)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (!response.path("ok").asBoolean()) {
            throw new IllegalArgumentException(response.path("description").asText("Telegram send failed"));
        }
        return response.path("result").path("message_id").asText();
    }

    public List<DiscoveredChat> discoverRecentPrivateChats(String token) {
        JsonNode response = webClient.get()
                .uri("/bot{token}/getUpdates?limit=50", token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (!response.path("ok").asBoolean()) {
            throw new IllegalArgumentException(response.path("description").asText("Telegram getUpdates failed"));
        }

        Map<String, DiscoveredChat> chats = new LinkedHashMap<>();
        for (JsonNode update : response.path("result")) {
            JsonNode message = update.path("message");
            if (message.isMissingNode()) {
                message = update.path("edited_message");
            }
            if (message.isMissingNode()) {
                continue;
            }
            JsonNode chat = message.path("chat");
            if (!"private".equals(chat.path("type").asText())) {
                continue;
            }
            String chatId = chat.path("id").asText();
            chats.putIfAbsent(chatId, new DiscoveredChat(
                    chatId,
                    blankToNull(chat.path("username").asText(null)),
                    blankToNull(chat.path("first_name").asText(null)),
                    blankToNull(chat.path("last_name").asText(null)),
                    blankToNull(message.path("text").asText(null))
            ));
        }
        return new ArrayList<>(chats.values());
    }

    private static Object buildSendMessageBody(String chatId, String message, String parseMode, boolean disableNotification) {
        if ((parseMode == null || parseMode.isBlank()) && !disableNotification) {
            return new SendMessageRequest(chatId, message);
        }
        if (parseMode == null || parseMode.isBlank()) {
            return new SendMessageRequestWithNotification(chatId, message, disableNotification);
        }
        if (!disableNotification) {
            return new SendMessageRequestWithMode(chatId, message, parseMode);
        }
        return new SendMessageRequestFull(chatId, message, parseMode, true);
    }

    private static WebClient buildWebClient(AppProperties.Telegram telegram) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30));

        if (telegram.proxyHost() != null && !telegram.proxyHost().isBlank() && telegram.proxyPort() > 0) {
            httpClient = httpClient.proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(telegram.proxyHost())
                    .port(telegram.proxyPort()));
        }

        return WebClient.builder()
                .baseUrl(telegram.apiBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record TelegramBotInfo(String username, String botId) {
    }

    public record DiscoveredChat(
            String chatId,
            String username,
            String firstName,
            String lastName,
            String lastMessageText
    ) {
    }

    private record SendMessageRequest(String chat_id, String text) {
    }

    private record SendMessageRequestWithMode(String chat_id, String text, String parse_mode) {
    }

    private record SendMessageRequestWithNotification(String chat_id, String text, boolean disable_notification) {
    }

    private record SendMessageRequestFull(String chat_id, String text, String parse_mode, boolean disable_notification) {
    }
}

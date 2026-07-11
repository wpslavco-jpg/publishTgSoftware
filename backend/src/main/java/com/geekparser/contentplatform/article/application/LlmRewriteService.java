package com.geekparser.contentplatform.article.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.geekparser.contentplatform.config.AppProperties;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class LlmRewriteService {

    private static final Pattern CYRILLIC_PATTERN = Pattern.compile(".*[А-Яа-яЁё].*");
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public LlmRewriteService(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
    }

    public RewriteResult rewrite(String title, String body) {
        ensureLlmConfigured();

        ChatRequest request = new ChatRequest(
                appProperties.llm().model(),
                List.of(
                        new ChatMessage("system", """
                                Ты редактор технологического телеграм-канала.
                                Твоя задача:
                                1. Полностью перевести статью на русский язык.
                                2. Сделать осмысленное краткое summary на русском языке максимум в 10 предложений.
                                3. Не выдумывать факты, имена, цитаты или детали, которых нет в исходнике.
                                4. Если текст сомнительный, неполный или нужен человеческий контроль, поставь needsManualReview=true.
                                5. Ответ верни строго JSON без markdown и без пояснений.

                                Формат ответа:
                                {"title":"...","translatedBody":"...","summaryBody":"...","needsManualReview":true|false}
                                """),
                        new ChatMessage("user", "TITLE: " + title + "\n\nBODY:\n" + body)
                )
        );

        JsonNode response = webClientBuilder.baseUrl(appProperties.llm().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.llm().apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String content = response.path("choices").path(0).path("message").path("content").asText();
        JsonNode payload = JsonHelper.parse(content);

        String rewrittenTitle = normalize(payload.path("title").asText());
        String translatedBody = normalize(payload.path("translatedBody").asText());
        String summaryBody = normalize(payload.path("summaryBody").asText());
        boolean needsManualReview = payload.path("needsManualReview").asBoolean(false);

        validateRussianText("title", rewrittenTitle);
        validateRussianText("translatedBody", translatedBody);
        validateRussianText("summaryBody", summaryBody);
        validateSentenceLimit(summaryBody);

        return new RewriteResult(rewrittenTitle, translatedBody, summaryBody, needsManualReview, appProperties.llm().model());
    }

    public String getConfiguredModel() {
        return appProperties.llm().model();
    }

    private void ensureLlmConfigured() {
        if (appProperties.llm().apiKey() == null || appProperties.llm().apiKey().isBlank()) {
            throw new IllegalStateException("LLM API key is required. Local fallback summarization is disabled.");
        }
    }

    private void validateRussianText(String field, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("LLM returned empty " + field);
        }
        if (!CYRILLIC_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("LLM returned non-Russian " + field);
        }
    }

    private void validateSentenceLimit(String summaryBody) {
        int sentences = 0;
        for (String part : SENTENCE_SPLIT_PATTERN.split(summaryBody.trim())) {
            if (!part.isBlank()) {
                sentences++;
            }
        }
        if (sentences == 0) {
            throw new IllegalArgumentException("LLM returned empty summaryBody");
        }
        if (sentences > 10) {
            throw new IllegalArgumentException("LLM returned summaryBody longer than 10 sentences");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private record ChatRequest(String model, List<ChatMessage> messages) {
    }

    private record ChatMessage(String role, String content) {
    }

    public record RewriteResult(String title, String translatedBody, String summaryBody, boolean needsManualReview,
                                String model) {
    }
}

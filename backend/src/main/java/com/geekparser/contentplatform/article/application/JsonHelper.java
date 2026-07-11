package com.geekparser.contentplatform.article.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonHelper() {
    }

    static JsonNode parse(String content) {
        try {
            return OBJECT_MAPPER.readTree(content);
        } catch (Exception exception) {
            throw new IllegalArgumentException("LLM response is not valid JSON", exception);
        }
    }
}

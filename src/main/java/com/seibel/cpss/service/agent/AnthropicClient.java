package com.seibel.cpss.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Thin wrapper over the Anthropic Messages API.
 *
 * <p>Deliberately minimal: it sends a message array and returns the raw
 * response body. Turn management, tool dispatch, and budget enforcement belong
 * to the caller ({@link FoodResearchAgent}), not here — keeping this class
 * dumb is what makes the agent loop testable with a stubbed client.
 */
@Slf4j
@Component
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicClient(
            @Value("${app.agent.anthropic.api-key:${ANTHROPIC_API_KEY:}}") String apiKey,
            @Value("${app.agent.anthropic.model:claude-sonnet-4-5}") String model,
            @Value("${app.agent.anthropic.max-tokens:2048}") int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.restClient = RestClient.builder()
                .baseUrl(API_URL)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    /**
     * One API turn. {@code messages} is the full conversation so far;
     * {@code tools} may be empty for a plain classification call.
     *
     * @return the parsed response body, or null if the call failed after retries
     */
    public JsonNode send(String systemPrompt, ArrayNode messages, ArrayNode tools) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY is not set — the food-warning agent cannot run without it");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", systemPrompt);
        body.set("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
        }

        return sendWithRetry(body);
    }

    private JsonNode sendWithRetry(ObjectNode body) {
        int attempts = 3;
        Exception last = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                String response = restClient.post()
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", API_VERSION)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                return objectMapper.readTree(response);

            } catch (Exception e) {
                last = e;
                log.warn("Anthropic API call failed (attempt {}/{}): {}", attempt, attempts, e.getMessage());
                if (attempt < attempts) {
                    sleep(Duration.ofSeconds(2L * attempt));
                }
            }
        }

        log.error("Anthropic API call failed after {} attempts", attempts, last);
        return null;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- small helpers for building request payloads ----

    public ObjectNode newObject() {
        return objectMapper.createObjectNode();
    }

    public ArrayNode newArray() {
        return objectMapper.createArrayNode();
    }

    public ObjectNode userMessage(String text) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", text);
        return msg;
    }

    /**
     * Extracts the first text block from a response, ignoring tool_use blocks.
     * Returns null when the model produced no prose this turn.
     */
    public String firstText(JsonNode response) {
        if (response == null || !response.has("content")) {
            return null;
        }
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                return block.path("text").asText();
            }
        }
        return null;
    }

    /** All tool_use blocks in a response, in order. */
    public List<JsonNode> toolUses(JsonNode response) {
        if (response == null || !response.has("content")) {
            return List.of();
        }
        return java.util.stream.StreamSupport
                .stream(response.get("content").spliterator(), false)
                .filter(b -> "tool_use".equals(b.path("type").asText()))
                .toList();
    }

    public ObjectMapper mapper() {
        return objectMapper;
    }
}

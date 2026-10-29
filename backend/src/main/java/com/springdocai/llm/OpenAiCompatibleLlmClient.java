package com.springdocai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/** Cliente para endpoints compatíveis com {@code POST {baseUrl}/chat/completions}. */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties properties;

    public OpenAiCompatibleLlmClient(RestClient llmRestClient, LlmProperties properties) {
        this.restClient = llmRestClient;
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, String userContent, String apiKey) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)));

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            // Mensagem sem corpo/headers para nunca vazar a chave.
            throw new LlmApiException("A API da LLM respondeu HTTP " + status,
                    status, status == 429 || status >= 500);
        } catch (ResourceAccessException e) {
            throw new LlmApiException("Falha de rede ao chamar a API da LLM", -1, true);
        }

        String content = response == null ? null
                : response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new LlmApiException("A API da LLM retornou uma resposta vazia", 200, true);
        }
        return content;
    }
}


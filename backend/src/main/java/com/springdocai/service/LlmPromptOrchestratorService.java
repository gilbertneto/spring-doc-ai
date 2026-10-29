package com.springdocai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdocai.ast.dto.ProjectAstSummary;
import com.springdocai.domain.enums.DocumentType;
import com.springdocai.llm.LlmApiException;
import com.springdocai.llm.LlmClient;
import com.springdocai.llm.LlmGenerationException;
import com.springdocai.llm.LlmPrompts;
import com.springdocai.llm.LlmProperties;
import com.springdocai.llm.MockDocumentGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquestra a geração de cada documento: resolve a chave (BYOK &gt; padrão &gt; mock), monta o prompt
 * a partir do {@link ProjectAstSummary} e aplica retentativa com backoff exponencial (EC04).
 */
@Service
public class LlmPromptOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(LlmPromptOrchestratorService.class);

    private final LlmClient llmClient;
    private final MockDocumentGenerator mockGenerator;
    private final ObjectMapper objectMapper;
    private final LlmProperties properties;

    public LlmPromptOrchestratorService(LlmClient llmClient, MockDocumentGenerator mockGenerator,
                                        ObjectMapper objectMapper, LlmProperties properties) {
        this.llmClient = llmClient;
        this.mockGenerator = mockGenerator;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Chave efetiva: header BYOK, senão chave padrão do ambiente, senão {@code null} (modo mock). */
    public String resolveApiKey(String headerKey) {
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey.trim();
        }
        String defaultKey = properties.getApiKey();
        return defaultKey != null && !defaultKey.isBlank() ? defaultKey.trim() : null;
    }

    /**
     * Gera o Markdown de um documento.
     *
     * @throws LlmGenerationException quando todas as tentativas falham ou o erro não é retentável
     */
    public String generate(DocumentType type, ProjectAstSummary summary, String headerKey) {
        String apiKey = resolveApiKey(headerKey);
        if (apiKey == null) {
            log.info("Gerando {} em modo MOCK (sem chave de LLM)", type);
            return mockGenerator.generate(type, summary);
        }

        String userContent = "ProjectAstSummary (JSON):\n" + toJson(summary);
        String systemPrompt = LlmPrompts.systemPrompt(type);
        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        long delay = Math.max(0, properties.getRetry().getInitialDelayMs());

        for (int attempt = 1; ; attempt++) {
            try {
                return llmClient.complete(systemPrompt, userContent, apiKey);
            } catch (LlmApiException e) {
                if (!e.isRetryable() || attempt >= maxAttempts) {
                    throw new LlmGenerationException(type, attempt,
                            "Falha ao gerar " + type.getFileName() + " após " + attempt
                                    + " tentativa(s): " + e.getMessage());
                }
                log.warn("Tentativa {}/{} de {} falhou ({}); nova tentativa em {} ms",
                        attempt, maxAttempts, type, e.getMessage(), delay);
                sleep(delay);
                delay *= 2;
            }
        }
    }

    private String toJson(ProjectAstSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o ProjectAstSummary", e);
        }
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


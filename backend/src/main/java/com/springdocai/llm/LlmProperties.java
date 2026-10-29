package com.springdocai.llm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração do provedor de LLM (endpoint compatível com OpenAI Chat Completions). */
@ConfigurationProperties(prefix = "springdocai.llm")
@Getter
@Setter
public class LlmProperties {

    /** Chave padrão (variável de ambiente LLM_API_KEY). Vazia => modo MOCK quando não há header BYOK. */
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private int timeoutSeconds = 120;
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
    }
}


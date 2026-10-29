package com.springdocai.llm;

/** Contrato mínimo de chamada à LLM. Substituível por mocks nos testes. */
public interface LlmClient {

    /**
     * @param systemPrompt prompt de sistema
     * @param userContent  conteúdo do usuário (JSON do ProjectAstSummary)
     * @param apiKey       chave efetiva (BYOK ou padrão); nunca nula/vazia
     * @return texto Markdown retornado pela LLM
     * @throws LlmApiException em qualquer falha da chamada
     */
    String complete(String systemPrompt, String userContent, String apiKey);
}


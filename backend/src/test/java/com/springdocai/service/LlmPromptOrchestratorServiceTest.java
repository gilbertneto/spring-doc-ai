package com.springdocai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdocai.ast.dto.ProjectAstSummary;
import com.springdocai.ast.service.JavaParserAstExtractorService;
import com.springdocai.domain.enums.DocumentType;
import com.springdocai.llm.LlmApiException;
import com.springdocai.llm.LlmClient;
import com.springdocai.llm.LlmGenerationException;
import com.springdocai.llm.LlmPrompts;
import com.springdocai.llm.LlmProperties;
import com.springdocai.llm.MockDocumentGenerator;
import com.springdocai.testsupport.SampleProjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LlmPromptOrchestratorServiceTest {

    private final ProjectAstSummary summary =
            new JavaParserAstExtractorService().extract(SampleProjects.orderProject());

    private LlmClient client;
    private LlmProperties properties;
    private LlmPromptOrchestratorService service;

    @BeforeEach
    void setUp() {
        client = mock(LlmClient.class);
        properties = new LlmProperties();
        properties.getRetry().setInitialDelayMs(0);
        ObjectMapper mapper = new ObjectMapper();
        service = new LlmPromptOrchestratorService(client, new MockDocumentGenerator(mapper), mapper, properties);
    }

    @Test
    void withoutAnyKeyUsesMockModeAndNeverCallsTheLlm() {
        String api = service.generate(DocumentType.API_REFERENCE, summary, null);
        String guide = service.generate(DocumentType.INTEGRATION_GUIDE, summary, "  ");
        String arch = service.generate(DocumentType.ARCHITECTURE_OVERVIEW, summary, null);

        assertThat(api).contains("MOCK", "/api/v1/orders/{id}", "@NotBlank", "OrderNotFoundException", "404");
        assertThat(guide).contains("curl -X POST", "/api/v1/orders/1", "\"customer\":\"string\"");
        assertThat(arch).contains("flowchart LR", "OrderController --> OrderService", "erDiagram",
                "Order }o--|| Customer", "spring-boot-starter-web", "com.acme.web");
        verifyNoInteractions(client);
    }

    @Test
    void byokHeaderTakesPrecedenceOverDefaultKey() {
        properties.setApiKey("default-key");
        when(client.complete(anyString(), anyString(), eq("byok-key"))).thenReturn("# generated");

        String result = service.generate(DocumentType.API_REFERENCE, summary, " byok-key ");

        assertThat(result).isEqualTo("# generated");
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(client).complete(eq(LlmPrompts.systemPrompt(DocumentType.API_REFERENCE)), user.capture(), eq("byok-key"));
        assertThat(user.getValue()).contains("OrderController", "/api/v1/orders/{id}", "ProjectAstSummary");
    }

    @Test
    void usesDefaultKeyWhenHeaderIsAbsent() {
        properties.setApiKey("default-key");
        when(client.complete(anyString(), anyString(), eq("default-key"))).thenReturn("ok");

        assertThat(service.generate(DocumentType.ARCHITECTURE_OVERVIEW, summary, null)).isEqualTo("ok");
    }

    @Test
    void ec04_retriesOnRateLimitThenSucceeds() {
        properties.setApiKey("k");
        when(client.complete(any(), any(), any()))
                .thenThrow(new LlmApiException("HTTP 429", 429, true))
                .thenThrow(new LlmApiException("HTTP 503", 503, true))
                .thenReturn("finally");

        assertThat(service.generate(DocumentType.INTEGRATION_GUIDE, summary, null)).isEqualTo("finally");
        verify(client, times(3)).complete(any(), any(), any());
    }

    @Test
    void ec04_failsAfterMaxAttemptsWithoutLeakingTheKey() {
        when(client.complete(any(), any(), any())).thenThrow(new LlmApiException("HTTP 429", 429, true));

        assertThatThrownBy(() -> service.generate(DocumentType.API_REFERENCE, summary, "secret-key"))
                .isInstanceOfSatisfying(LlmGenerationException.class, e -> {
                    assertThat(e.getAttempts()).isEqualTo(3);
                    assertThat(e.getDocumentType()).isEqualTo(DocumentType.API_REFERENCE);
                    assertThat(e.getMessage()).contains("API-REFERENCE.md").doesNotContain("secret-key");
                });
        verify(client, times(3)).complete(any(), any(), any());
    }

    @Test
    void nonRetryableErrorsFailImmediately() {
        when(client.complete(any(), any(), any())).thenThrow(new LlmApiException("HTTP 401", 401, false));

        assertThatThrownBy(() -> service.generate(DocumentType.API_REFERENCE, summary, "bad-key"))
                .isInstanceOf(LlmGenerationException.class);
        verify(client, times(1)).complete(any(), any(), any());
    }

    @Test
    void eachSystemPromptTargetsItsOwnFile() {
        for (DocumentType type : DocumentType.values()) {
            assertThat(LlmPrompts.systemPrompt(type)).contains(type.getFileName(), "ProjectAstSummary");
        }
    }
}


package com.springdocai.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleLlmClientTest {

    private MockRestServiceServer server;
    private OpenAiCompatibleLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://llm.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleLlmClient(builder.build(), new LlmProperties());
    }

    @Test
    void sendsBearerKeyAndMessagesAndReturnsContent() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("sys"))
                .andExpect(jsonPath("$.messages[1].content").value("user"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"# Hello\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.complete("sys", "user", "sk-test")).isEqualTo("# Hello");
        server.verify();
    }

    @Test
    void rateLimitAndServerErrorsAreRetryable() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.complete("s", "u", "k"))
                .isInstanceOfSatisfying(LlmApiException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(429);
                    assertThat(e.isRetryable()).isTrue();
                });
        assertThatThrownBy(() -> client.complete("s", "u", "k"))
                .isInstanceOfSatisfying(LlmApiException.class, e -> assertThat(e.isRetryable()).isTrue());
    }

    @Test
    void authErrorsAreNotRetryableAndDoNotLeakTheKey() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.complete("s", "u", "sk-secret"))
                .isInstanceOfSatisfying(LlmApiException.class, e -> {
                    assertThat(e.isRetryable()).isFalse();
                    assertThat(e.getMessage()).doesNotContain("sk-secret");
                });
    }

    @Test
    void emptyChoicesIsTreatedAsRetryableFailure() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("s", "u", "k"))
                .isInstanceOfSatisfying(LlmApiException.class, e -> assertThat(e.isRetryable()).isTrue());
    }
}


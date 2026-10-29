package com.springdocai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdocai.testsupport.SampleProjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo ponta a ponta (H2 + executor assíncrono real + modo MOCK, sem chave de LLM). */
@SpringBootTest(properties = "springdocai.llm.api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GenerationFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void uploadGeneratesThreeDocumentsAsynchronouslyInMockMode() throws Exception {
        String body = mapper.writeValueAsString(
                Map.of("projectName", "orders", "files", SampleProjects.orderProject()));

        String created = mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String jobId = mapper.readTree(created).get("id").asText();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonNode detail = getJson("/api/v1/generations/" + jobId);
            assertThat(detail.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(detail.get("endpointsCount").asInt()).isEqualTo(3);
            assertThat(detail.get("documents")).hasSize(3);
            assertThat(detail.get("documents").get(0).get("fileName").asText()).isEqualTo("API-REFERENCE.md");
            assertThat(detail.get("documents").get(1).get("fileName").asText()).isEqualTo("INTEGRATION-GUIDE.md");
            assertThat(detail.get("documents").get(2).get("content").asText()).contains("erDiagram");
        });

        JsonNode history = getJson("/api/v1/generations/history");
        assertThat(history.isArray()).isTrue();
        assertThat(history.findValuesAsText("id")).contains(jobId);
    }

    @Test
    void projectWithoutEndpointsIsRejectedWith422() throws Exception {
        String body = mapper.writeValueAsString(
                Map.of("projectName", "lib", "files", SampleProjects.noEndpointsProject()));

        mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode getJson(String url) throws Exception {
        return mapper.readTree(mvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }
}


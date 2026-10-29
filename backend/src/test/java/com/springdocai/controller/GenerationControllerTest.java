package com.springdocai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdocai.ast.exception.NoEndpointsFoundException;
import com.springdocai.config.CorsConfig;
import com.springdocai.domain.entity.GeneratedDocument;
import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.DocumentType;
import com.springdocai.domain.enums.JobStatus;
import com.springdocai.domain.enums.SourceType;
import com.springdocai.service.GenerationWorkflowService;
import com.springdocai.service.GithubIngestionException;
import com.springdocai.service.GithubTarballInMemoryService;
import com.springdocai.service.GithubTarballInMemoryService.GithubRepoRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GenerationController.class)
@Import(CorsConfig.class)
class GenerationControllerTest {

    private static final UUID JOB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private GenerationWorkflowService workflow;
    @MockBean
    private GithubTarballInMemoryService github;

    private GenerationJob job(SourceType type, JobStatus status) {
        return GenerationJob.builder().id(JOB_ID).projectName("orders").sourceType(type)
                .status(status).endpointsCount(3).classesAnalyzedCount(7)
                .createdAt(LocalDateTime.of(2025, 1, 31, 12, 0)).build();
    }

    private String uploadBody(String name, Map<String, String> files) throws Exception {
        return mapper.writeValueAsString(Map.of("projectName", name, "files", files));
    }

    @Test
    void uploadReturns201WithLocationAndForwardsByokHeader() throws Exception {
        given(workflow.start(eq("orders"), eq(SourceType.ZIP_UPLOAD), isNull(), anyMap(), eq("my-key")))
                .willReturn(job(SourceType.ZIP_UPLOAD, JobStatus.GENERATING_DOCS));

        mvc.perform(post("/api/v1/generations/upload")
                        .header("X-LLM-API-Key", "my-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody("orders", Map.of("src/main/java/A.java", "class A {}"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/generations/" + JOB_ID))
                .andExpect(jsonPath("$.id").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.status").value("GENERATING_DOCS"))
                .andExpect(jsonPath("$.endpointsCount").value(3))
                .andExpect(jsonPath("$.sourceType").value("ZIP_UPLOAD"));
    }

    @Test
    void uploadWithoutHeaderPassesNullKey() throws Exception {
        given(workflow.start(eq("orders"), eq(SourceType.ZIP_UPLOAD), isNull(), anyMap(), isNull()))
                .willReturn(job(SourceType.ZIP_UPLOAD, JobStatus.GENERATING_DOCS));

        mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody("orders", Map.of("A.java", "class A {}"))))
                .andExpect(status().isCreated());
    }

    @Test
    void uploadValidationErrorsReturn400() throws Exception {
        mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody(" ", Map.of("A.java", "class A {}"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody("orders", Map.of())))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void ec01_projectWithoutEndpointsReturns422() throws Exception {
        given(workflow.start(eq("lib"), eq(SourceType.ZIP_UPLOAD), isNull(), anyMap(), isNull()))
                .willThrow(new NoEndpointsFoundException(
                        "Nenhum endpoint Spring Web (@RestController) foi encontrado neste projeto."));

        mvc.perform(post("/api/v1/generations/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody("lib", Map.of("Util.java", "class Util {}"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NO_ENDPOINTS_FOUND"))
                .andExpect(jsonPath("$.message").value(
                        "Nenhum endpoint Spring Web (@RestController) foi encontrado neste projeto."));
    }

    @Test
    void githubReturns201() throws Exception {
        String url = "https://github.com/acme/orders";
        given(github.parse(url)).willReturn(new GithubRepoRef("acme", "orders", null));
        given(github.download(url)).willReturn(Map.of("A.java", "class A {}"));
        given(workflow.start(eq("orders"), eq(SourceType.GITHUB_URL), eq(url), anyMap(), isNull()))
                .willReturn(job(SourceType.GITHUB_URL, JobStatus.GENERATING_DOCS));

        mvc.perform(post("/api/v1/generations/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repositoryUrl\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("GITHUB_URL"));
    }

    @Test
    void githubInvalidUrlReturns400AndDownloadFailureReturns422() throws Exception {
        given(github.parse("https://gitlab.com/a/b")).willThrow(new IllegalArgumentException("URL inválida."));
        mvc.perform(post("/api/v1/generations/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repositoryUrl\":\"https://gitlab.com/a/b\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("URL inválida."));

        String url = "https://github.com/acme/private";
        given(github.parse(url)).willReturn(new GithubRepoRef("acme", "private", null));
        given(github.download(url)).willThrow(new GithubIngestionException("Repositório não encontrado ou não é público."));
        mvc.perform(post("/api/v1/generations/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repositoryUrl\":\"" + url + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("GITHUB_INGESTION_FAILED"));
    }

    @Test
    void getReturnsStatusMetricsAndDocuments() throws Exception {
        GenerationJob completed = job(SourceType.ZIP_UPLOAD, JobStatus.COMPLETED);
        given(workflow.findJob(JOB_ID)).willReturn(Optional.of(completed));
        given(workflow.findDocuments(JOB_ID)).willReturn(List.of(
                GeneratedDocument.builder().docType(DocumentType.API_REFERENCE)
                        .fileName("API-REFERENCE.md").content("# API").build()));

        mvc.perform(get("/api/v1/generations/{id}", JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endpointsCount").value(3))
                .andExpect(jsonPath("$.classesAnalyzedCount").value(7))
                .andExpect(jsonPath("$.documents.length()").value(1))
                .andExpect(jsonPath("$.documents[0].docType").value("API_REFERENCE"))
                .andExpect(jsonPath("$.documents[0].fileName").value("API-REFERENCE.md"))
                .andExpect(jsonPath("$.documents[0].content").value("# API"));
    }

    @Test
    void getUnknownJobReturns404() throws Exception {
        given(workflow.findJob(JOB_ID)).willReturn(Optional.empty());

        mvc.perform(get("/api/v1/generations/{id}", JOB_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("JOB_NOT_FOUND"));
    }

    @Test
    void historyReturnsLatestJobs() throws Exception {
        given(workflow.history()).willReturn(List.of(
                job(SourceType.ZIP_UPLOAD, JobStatus.COMPLETED), job(SourceType.GITHUB_URL, JobStatus.FAILED)));

        mvc.perform(get("/api/v1/generations/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].status").value("FAILED"));
    }

    @Test
    void corsAllowsLocalhost3000Only() throws Exception {
        mvc.perform(options("/api/v1/generations/upload")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-llm-api-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mvc.perform(options("/api/v1/generations/upload")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}


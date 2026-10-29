package com.springdocai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springdocai.ast.exception.NoEndpointsFoundException;
import com.springdocai.ast.service.JavaParserAstExtractorService;
import com.springdocai.domain.entity.GeneratedDocument;
import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.DocumentType;
import com.springdocai.domain.enums.JobStatus;
import com.springdocai.domain.enums.SourceType;
import com.springdocai.llm.LlmApiException;
import com.springdocai.llm.LlmClient;
import com.springdocai.llm.LlmProperties;
import com.springdocai.llm.MockDocumentGenerator;
import com.springdocai.repository.GeneratedDocumentRepository;
import com.springdocai.repository.GenerationJobRepository;
import com.springdocai.testsupport.SampleProjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class GenerationWorkflowServiceTest {

    @Autowired
    private GenerationJobRepository jobRepository;
    @Autowired
    private GeneratedDocumentRepository documentRepository;

    private final LlmProperties properties = new LlmProperties();

    @BeforeEach
    void setUp() {
        properties.getRetry().setMaxAttempts(1);
        properties.getRetry().setInitialDelayMs(0);
    }

    private GenerationWorkflowService workflow(LlmClient client) {
        ObjectMapper mapper = new ObjectMapper();
        LlmPromptOrchestratorService orchestrator = new LlmPromptOrchestratorService(
                client, new MockDocumentGenerator(mapper), mapper, properties);
        // Executor síncrono: a geração roda dentro de start(), tornando o teste determinístico.
        return new GenerationWorkflowService(jobRepository, documentRepository,
                new JavaParserAstExtractorService(), orchestrator, Runnable::run);
    }

    @Test
    void completesJobAndPersistsOnlyTheThreeMarkdownDocuments() {
        GenerationWorkflowService workflow = workflow((s, u, k) -> {
            throw new AssertionError("LLM não deve ser chamada em modo mock");
        });

        GenerationJob started = workflow.start("orders", SourceType.ZIP_UPLOAD, null,
                SampleProjects.orderProject(), null);

        GenerationJob job = jobRepository.findById(started.getId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getEndpointsCount()).isEqualTo(3);
        assertThat(job.getClassesAnalyzedCount()).isEqualTo(7);
        assertThat(job.getCompletedAt()).isNotNull();

        List<GeneratedDocument> docs = workflow.findDocuments(job.getId());
        assertThat(docs).extracting(GeneratedDocument::getDocType)
                .containsExactly(DocumentType.API_REFERENCE, DocumentType.INTEGRATION_GUIDE,
                        DocumentType.ARCHITECTURE_OVERVIEW);
        assertThat(docs).extracting(GeneratedDocument::getFileName)
                .containsExactly("API-REFERENCE.md", "INTEGRATION-GUIDE.md", "ARCHITECTURE.md");
        // Zero-Retention: nenhum trecho do código-fonte original é persistido
        assertThat(docs).allSatisfy(d -> assertThat(d.getContent())
                .doesNotContain("ResponseEntity.ok(").doesNotContain("orderService.find"));
    }

    @Test
    void ec01_marksJobAsFailedAndRethrows() {
        GenerationWorkflowService workflow = workflow((s, u, k) -> "unused");

        assertThatThrownBy(() -> workflow.start("lib", SourceType.ZIP_UPLOAD, null,
                SampleProjects.noEndpointsProject(), null))
                .isInstanceOf(NoEndpointsFoundException.class);

        GenerationJob job = workflow.history().get(0);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Nenhum endpoint Spring Web");
        assertThat(documentRepository.findByJobId(job.getId())).isEmpty();
    }

    @Test
    void ec04_failureOnSecondDocumentKeepsFirstAndFailsJob() {
        properties.setApiKey("k");
        AtomicInteger calls = new AtomicInteger();
        GenerationWorkflowService workflow = workflow((system, user, key) -> {
            if (calls.incrementAndGet() == 2) {
                throw new LlmApiException("HTTP 429", 429, true);
            }
            return "# ok " + calls.get();
        });

        GenerationJob started = workflow.start("orders", SourceType.GITHUB_URL,
                "https://github.com/acme/orders", SampleProjects.orderProject(), null);

        GenerationJob job = jobRepository.findById(started.getId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("INTEGRATION-GUIDE.md");
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(workflow.findDocuments(job.getId()))
                .extracting(GeneratedDocument::getDocType).containsExactly(DocumentType.API_REFERENCE);
    }
}


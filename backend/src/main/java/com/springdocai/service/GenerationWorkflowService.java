package com.springdocai.service;

import com.springdocai.ast.dto.ProjectAstSummary;
import com.springdocai.ast.exception.NoEndpointsFoundException;
import com.springdocai.ast.service.JavaParserAstExtractorService;
import com.springdocai.domain.entity.GeneratedDocument;
import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.DocumentType;
import com.springdocai.domain.enums.JobStatus;
import com.springdocai.domain.enums.SourceType;
import com.springdocai.repository.GeneratedDocumentRepository;
import com.springdocai.repository.GenerationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * Fluxo completo de uma geração. O código-fonte só existe em memória durante {@link #start}: a
 * AST é extraída de forma síncrona e a geração assíncrona recebe apenas o {@link ProjectAstSummary}.
 */
@Service
public class GenerationWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(GenerationWorkflowService.class);
    private static final int MAX_ERROR_LENGTH = 1000;

    private final GenerationJobRepository jobRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final JavaParserAstExtractorService extractor;
    private final LlmPromptOrchestratorService orchestrator;
    private final TaskExecutor executor;

    public GenerationWorkflowService(GenerationJobRepository jobRepository,
                                     GeneratedDocumentRepository documentRepository,
                                     JavaParserAstExtractorService extractor,
                                     LlmPromptOrchestratorService orchestrator,
                                     @Qualifier("generationExecutor") TaskExecutor executor) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.extractor = extractor;
        this.orchestrator = orchestrator;
        this.executor = executor;
    }

    /**
     * Cria o job, extrai a AST (síncrono) e agenda a geração dos 3 documentos (assíncrono).
     *
     * @throws NoEndpointsFoundException EC01 (o job é gravado como FAILED)
     */
    public GenerationJob start(String projectName, SourceType sourceType, String repositoryUrl,
                               Map<String, String> files, String apiKeyHeader) {
        GenerationJob job = jobRepository.save(GenerationJob.builder()
                .projectName(projectName)
                .sourceType(sourceType)
                .repositoryUrl(repositoryUrl)
                .status(JobStatus.PARSING_AST)
                .build());

        ProjectAstSummary summary;
        try {
            summary = extractor.extract(files);
        } catch (NoEndpointsFoundException e) {
            fail(job, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            fail(job, "Falha ao extrair a AST do projeto.");
            throw e;
        }

        job.setEndpointsCount(summary.totalEndpoints());
        job.setClassesAnalyzedCount(summary.classesAnalyzed());
        job.setStatus(JobStatus.GENERATING_DOCS);
        job = jobRepository.save(job);

        UUID jobId = job.getId();
        try {
            executor.execute(() -> generateDocuments(jobId, summary, apiKeyHeader));
        } catch (RejectedExecutionException e) {
            fail(job, "Servidor ocupado. Tente novamente em instantes.");
        }
        return job;
    }

    /**
     * Gera e persiste os 3 documentos em sequência. Cada documento é salvo assim que concluído; se
     * um deles falhar (EC04) o job vira FAILED preservando os anteriores.
     */
    public void generateDocuments(UUID jobId, ProjectAstSummary summary, String apiKeyHeader) {
        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        try {
            for (DocumentType type : DocumentType.values()) {
                String content = orchestrator.generate(type, summary, apiKeyHeader);
                documentRepository.save(GeneratedDocument.builder()
                        .job(job)
                        .docType(type)
                        .fileName(type.getFileName())
                        .content(content)
                        .build());
            }
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        } catch (RuntimeException e) {
            log.warn("Job {} falhou: {}", jobId, e.getMessage());
            fail(job, e.getMessage() != null ? e.getMessage() : "Erro inesperado na geração.");
        }
    }

    public Optional<GenerationJob> findJob(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    public List<GeneratedDocument> findDocuments(UUID jobId) {
        return documentRepository.findByJobId(jobId).stream()
                .sorted(Comparator.comparing(GeneratedDocument::getDocType))
                .toList();
    }

    public List<GenerationJob> history() {
        return jobRepository.findTop20ByOrderByCreatedAtDesc();
    }

    private void fail(GenerationJob job, String message) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }
}


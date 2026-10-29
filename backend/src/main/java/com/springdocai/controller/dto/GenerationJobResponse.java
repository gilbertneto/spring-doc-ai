package com.springdocai.controller.dto;

import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.JobStatus;
import com.springdocai.domain.enums.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resumo de um job (resposta do POST e itens do histórico). */
public record GenerationJobResponse(
        UUID id,
        String projectName,
        SourceType sourceType,
        String repositoryUrl,
        JobStatus status,
        Integer endpointsCount,
        Integer classesAnalyzedCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public static GenerationJobResponse from(GenerationJob job) {
        return new GenerationJobResponse(job.getId(), job.getProjectName(), job.getSourceType(),
                job.getRepositoryUrl(), job.getStatus(), job.getEndpointsCount(),
                job.getClassesAnalyzedCount(), job.getErrorMessage(), job.getCreatedAt(), job.getCompletedAt());
    }
}


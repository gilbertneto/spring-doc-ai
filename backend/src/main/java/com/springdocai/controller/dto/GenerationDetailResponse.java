package com.springdocai.controller.dto;

import com.springdocai.domain.entity.GeneratedDocument;
import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.JobStatus;
import com.springdocai.domain.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Detalhe de um job: status, métricas da AST e os documentos gerados até o momento. */
public record GenerationDetailResponse(
        UUID id,
        String projectName,
        SourceType sourceType,
        String repositoryUrl,
        JobStatus status,
        Integer endpointsCount,
        Integer classesAnalyzedCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<GeneratedDocumentResponse> documents) {

    public static GenerationDetailResponse from(GenerationJob job, List<GeneratedDocument> documents) {
        return new GenerationDetailResponse(job.getId(), job.getProjectName(), job.getSourceType(),
                job.getRepositoryUrl(), job.getStatus(), job.getEndpointsCount(),
                job.getClassesAnalyzedCount(), job.getErrorMessage(), job.getCreatedAt(), job.getCompletedAt(),
                documents.stream().map(GeneratedDocumentResponse::from).toList());
    }
}


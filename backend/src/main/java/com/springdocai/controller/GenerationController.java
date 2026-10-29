package com.springdocai.controller;

import com.springdocai.controller.dto.GenerationDetailResponse;
import com.springdocai.controller.dto.GenerationJobResponse;
import com.springdocai.controller.dto.GithubGenerationRequest;
import com.springdocai.controller.dto.UploadGenerationRequest;
import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.SourceType;
import com.springdocai.service.GenerationWorkflowService;
import com.springdocai.service.GithubTarballInMemoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/generations")
public class GenerationController {

    public static final String API_KEY_HEADER = "X-LLM-API-Key";

    private final GenerationWorkflowService workflow;
    private final GithubTarballInMemoryService github;

    public GenerationController(GenerationWorkflowService workflow, GithubTarballInMemoryService github) {
        this.workflow = workflow;
        this.github = github;
    }

    @PostMapping("/upload")
    public ResponseEntity<GenerationJobResponse> upload(
            @Valid @RequestBody UploadGenerationRequest request,
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey) {
        GenerationJob job = workflow.start(request.projectName(), SourceType.ZIP_UPLOAD, null,
                request.files(), apiKey);
        return created(job);
    }

    @PostMapping("/github")
    public ResponseEntity<GenerationJobResponse> github(
            @Valid @RequestBody GithubGenerationRequest request,
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey) {
        String repoName = github.parse(request.repositoryUrl()).repo(); // 400 se a URL for inválida
        Map<String, String> files = github.download(request.repositoryUrl());
        GenerationJob job = workflow.start(repoName, SourceType.GITHUB_URL, request.repositoryUrl(),
                files, apiKey);
        return created(job);
    }

    @GetMapping("/history")
    public List<GenerationJobResponse> history() {
        return workflow.history().stream().map(GenerationJobResponse::from).toList();
    }

    @GetMapping("/{jobId}")
    public GenerationDetailResponse get(@PathVariable UUID jobId) {
        GenerationJob job = workflow.findJob(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        return GenerationDetailResponse.from(job, workflow.findDocuments(jobId));
    }

    private ResponseEntity<GenerationJobResponse> created(GenerationJob job) {
        return ResponseEntity.created(URI.create("/api/v1/generations/" + job.getId()))
                .body(GenerationJobResponse.from(job));
    }
}


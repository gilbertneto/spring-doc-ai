package com.springdocai.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GithubGenerationRequest(@NotBlank @Size(max = 500) String repositoryUrl) {
}


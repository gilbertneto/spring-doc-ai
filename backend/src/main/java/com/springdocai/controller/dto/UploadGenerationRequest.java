package com.springdocai.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Payload do upload: arquivos já filtrados no cliente (JSZip).
 *
 * @param projectName nome do projeto (ex.: nome do .zip)
 * @param files       mapa {@code caminho relativo -> conteúdo UTF-8} de {@code .java} e pom.xml/build.gradle
 */
public record UploadGenerationRequest(
        @NotBlank @Size(max = 255) String projectName,
        @NotEmpty @Size(max = 5000) Map<String, String> files) {
}


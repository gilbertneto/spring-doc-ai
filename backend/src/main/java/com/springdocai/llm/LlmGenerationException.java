package com.springdocai.llm;

import com.springdocai.domain.enums.DocumentType;

/** A geração de um documento falhou definitivamente (após as retentativas do EC04). */
public class LlmGenerationException extends RuntimeException {

    private final DocumentType documentType;
    private final int attempts;

    public LlmGenerationException(DocumentType documentType, int attempts, String message) {
        super(message);
        this.documentType = documentType;
        this.attempts = attempts;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public int getAttempts() {
        return attempts;
    }
}


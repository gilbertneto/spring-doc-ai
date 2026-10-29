package com.springdocai.controller.dto;

import com.springdocai.domain.entity.GeneratedDocument;
import com.springdocai.domain.enums.DocumentType;

public record GeneratedDocumentResponse(DocumentType docType, String fileName, String content) {

    public static GeneratedDocumentResponse from(GeneratedDocument doc) {
        return new GeneratedDocumentResponse(doc.getDocType(), doc.getFileName(), doc.getContent());
    }
}


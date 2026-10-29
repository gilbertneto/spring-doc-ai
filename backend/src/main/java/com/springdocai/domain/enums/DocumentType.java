package com.springdocai.domain.enums;

public enum DocumentType {
    API_REFERENCE("API-REFERENCE.md"),
    INTEGRATION_GUIDE("INTEGRATION-GUIDE.md"),
    ARCHITECTURE_OVERVIEW("ARCHITECTURE.md");

    private final String fileName;

    DocumentType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}

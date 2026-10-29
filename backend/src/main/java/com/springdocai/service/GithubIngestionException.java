package com.springdocai.service;

/** Falha ao baixar/ler o repositório do GitHub (mapeada para HTTP 422). */
public class GithubIngestionException extends RuntimeException {

    public GithubIngestionException(String message) {
        super(message);
    }

    public GithubIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}


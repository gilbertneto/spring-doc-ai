package com.springdocai.llm;

/** Falha numa chamada HTTP à LLM. */
public class LlmApiException extends RuntimeException {

    private final int statusCode;
    private final boolean retryable;

    public LlmApiException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}


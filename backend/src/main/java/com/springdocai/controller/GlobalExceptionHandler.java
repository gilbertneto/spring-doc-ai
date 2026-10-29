package com.springdocai.controller;

import com.springdocai.ast.exception.NoEndpointsFoundException;
import com.springdocai.controller.dto.ErrorResponse;
import com.springdocai.service.GithubIngestionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> unreadable(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Requisição malformada.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /** EC01 */
    @ExceptionHandler(NoEndpointsFoundException.class)
    public ResponseEntity<ErrorResponse> noEndpoints(NoEndpointsFoundException e) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "NO_ENDPOINTS_FOUND", e.getMessage());
    }

    @ExceptionHandler(GithubIngestionException.class)
    public ResponseEntity<ErrorResponse> github(GithubIngestionException e) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "GITHUB_INGESTION_FAILED", e.getMessage());
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(JobNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", e.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}


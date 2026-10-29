package com.springdocai.controller;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID jobId) {
        super("Job não encontrado: " + jobId);
    }
}


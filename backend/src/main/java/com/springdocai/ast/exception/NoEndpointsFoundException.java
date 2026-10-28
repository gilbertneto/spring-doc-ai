package com.springdocai.ast.exception;

/** EC01: projeto sem nenhum endpoint Spring Web. */
public class NoEndpointsFoundException extends RuntimeException {

    public NoEndpointsFoundException(String message) {
        super(message);
    }
}

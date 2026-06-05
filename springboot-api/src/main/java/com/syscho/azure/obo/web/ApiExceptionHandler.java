package com.syscho.azure.obo.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WebClientResponseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handleWebClientResponseException(WebClientResponseException ex) {
        return Map.of(
                "message", "Downstream call failed",
                "status", ex.getStatusCode().value(),
                "body", ex.getResponseBodyAsString()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception ex) {
        return Map.of(
                "message", ex.getMessage() == null ? "Unexpected error" : ex.getMessage()
        );
    }
}

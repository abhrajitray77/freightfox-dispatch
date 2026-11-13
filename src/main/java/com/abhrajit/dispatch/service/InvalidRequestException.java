package com.abhrajit.dispatch.service;

import java.util.List;

/** A request that passed field validation but breaks a business rule (for example duplicate ids). */
public class InvalidRequestException extends RuntimeException {

    private final List<String> errors;

    public InvalidRequestException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}

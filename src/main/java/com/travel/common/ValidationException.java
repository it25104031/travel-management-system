package com.travel.common;

import java.util.List;

/** A business-rule failure carrying one or more messages that can be shown to the user. */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(String error) {
        this(List.of(error));
    }

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() { return errors; }
}

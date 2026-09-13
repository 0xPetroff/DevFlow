package com.devflow.exception;

/** A syntactically valid request whose parameters do not make sense. Surfaces as 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

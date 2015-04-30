package com.example.daryl.go.helpers.exception;

/**
 * An obj representing a HTTP 401 Unauthorized error
 */
public class UnauthorizedException extends Exception {

    public UnauthorizedException() {
        super("HTTP 401 error");
    }
}
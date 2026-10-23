package com.ruralhealthcare.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message, "INVALID_ARGUMENT", HttpStatus.BAD_REQUEST);
    }
}

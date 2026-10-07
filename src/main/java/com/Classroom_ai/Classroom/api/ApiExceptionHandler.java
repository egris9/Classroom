package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.auth.EmailAlreadyTakenException;
import com.Classroom_ai.Classroom.auth.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The one place that maps exception types to a status and a {code, message} body. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorBody> invalidCredentials(InvalidCredentialsException e) {
        return respond(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<ErrorBody> emailTaken(EmailAlreadyTakenException e) {
        return respond(HttpStatus.CONFLICT, "EMAIL_TAKEN", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalidBody(MethodArgumentNotValidException e) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "The request body is invalid.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> unexpected(Exception e) {
        log.error("Unexpected error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error.");
    }

    private static ResponseEntity<ErrorBody> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message));
    }
}

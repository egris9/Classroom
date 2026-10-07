package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.auth.EmailAlreadyTakenException;
import com.Classroom_ai.Classroom.auth.InvalidCredentialsException;
import com.Classroom_ai.Classroom.course.AlreadyTeacherException;
import com.Classroom_ai.Classroom.course.CourseFileNotFoundException;
import com.Classroom_ai.Classroom.course.CourseNameTakenException;
import com.Classroom_ai.Classroom.course.CourseNotFoundException;
import com.Classroom_ai.Classroom.material.FileTooLargeException;
import com.Classroom_ai.Classroom.material.StoredFileMissingException;
import com.Classroom_ai.Classroom.material.UnsupportedFileTypeException;
import com.Classroom_ai.Classroom.membership.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorBody> badRequest(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request is malformed.");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorBody> forbidden(ForbiddenException e) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorBody> courseNotFound(CourseNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(CourseFileNotFoundException.class)
    public ResponseEntity<ErrorBody> fileNotFound(CourseFileNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler({FileTooLargeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorBody> fileTooLarge(Exception e) {
        String message = e instanceof FileTooLargeException ? e.getMessage() : "The file is too large.";
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", message);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ErrorBody> unsupportedType(UnsupportedFileTypeException e) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", e.getMessage());
    }

    @ExceptionHandler(StoredFileMissingException.class)
    public ResponseEntity<ErrorBody> storedFileMissing(StoredFileMissingException e) {
        return respond(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(CourseNameTakenException.class)
    public ResponseEntity<ErrorBody> courseNameTaken(CourseNameTakenException e) {
        return respond(HttpStatus.CONFLICT, "COURSE_NAME_TAKEN", e.getMessage());
    }

    @ExceptionHandler(AlreadyTeacherException.class)
    public ResponseEntity<ErrorBody> alreadyTeacher(AlreadyTeacherException e) {
        return respond(HttpStatus.CONFLICT, "ALREADY_TEACHER", e.getMessage());
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

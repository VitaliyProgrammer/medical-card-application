package com.vitaliy.medcard.handler;

import com.vitaliy.medcard.exception.AllergyNotFoundException;
import com.vitaliy.medcard.exception.CareLinkAlreadyExistsException;
import com.vitaliy.medcard.exception.CareLinkNotFoundException;
import com.vitaliy.medcard.exception.ConditionNotFoundException;
import com.vitaliy.medcard.exception.DocumentNotFoundException;
import com.vitaliy.medcard.exception.FileStorageException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.InvalidCredentialsException;
import com.vitaliy.medcard.exception.InvalidPasswordResetTokenException;
import com.vitaliy.medcard.exception.InvalidRefreshTokenException;
import com.vitaliy.medcard.exception.JwtAuthenticationException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.exception.PdfGenerationException;
import com.vitaliy.medcard.exception.RegistrationException;
import com.vitaliy.medcard.exception.ReminderNotFoundException;
import com.vitaliy.medcard.exception.ShareLinkExpiredException;
import com.vitaliy.medcard.exception.ShareLinkNotFoundException;
import com.vitaliy.medcard.exception.UnsupportedFileTypeException;
import com.vitaliy.medcard.exception.UserNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .toList();

        return respond(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<Object> handleBadRequest(RuntimeException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            JwtAuthenticationException.class,
            InvalidRefreshTokenException.class,
            InvalidPasswordResetTokenException.class
    })
    public ResponseEntity<Object> handleUnauthorized(RuntimeException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Object> handleForbidden(RuntimeException ex) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, "You don't have permission to perform this action");
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            PatientProfileNotFoundException.class,
            AllergyNotFoundException.class,
            ConditionNotFoundException.class,
            DocumentNotFoundException.class,
            ReminderNotFoundException.class,
            ShareLinkNotFoundException.class,
            CareLinkNotFoundException.class
    })
    public ResponseEntity<Object> handleNotFound(RuntimeException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({RegistrationException.class, CareLinkAlreadyExistsException.class})
    public ResponseEntity<Object> handleConflict(RuntimeException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        logger.warn("Rejected a write that violates a database constraint", ex);
        return respond(HttpStatus.CONFLICT, "This record conflicts with existing data");
    }

    @ExceptionHandler(ShareLinkExpiredException.class)
    public ResponseEntity<Object> handleGone(RuntimeException ex) {
        return respond(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler({FileStorageException.class, PdfGenerationException.class})
    public ResponseEntity<Object> handleInfrastructureFailure(RuntimeException ex) {
        logger.error("Infrastructure failure", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                "The file service is temporarily unavailable. Please try again later");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        logger.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later");
    }

    private ResponseEntity<Object> respond(HttpStatus status, Object errorDetails) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("error", errorDetails);
        return ResponseEntity.status(status).body(body);
    }
}

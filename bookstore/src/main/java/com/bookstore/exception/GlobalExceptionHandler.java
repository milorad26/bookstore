package com.bookstore.exception;

import com.bookstore.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            messageSource.getMessage("validation.failed", null, locale)
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        log.error("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            messageSource.getMessage("auth.invalid.credentials", null, locale),
            messageSource.getMessage("error.unauthorized", null, locale)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(error);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        log.error("Authentication error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            messageSource.getMessage("auth.failed", null, locale),
            messageSource.getMessage("error.unauthorized", null, locale)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex) {
    Locale locale = LocaleContextHolder.getLocale();

    String param = Objects.requireNonNullElse(ex.getName(), "parameter");
    String expected = Optional.ofNullable(ex.getRequiredType())
        .map(Class::getSimpleName)
        .or(() -> Optional.ofNullable(ex.getParameter())
            .map(p -> p.getParameterType().getSimpleName()))
        .orElse("expected type");

    String invalid = ex.getValue() != null ? " (got: '" + ex.getValue() + "')" : "";

    String message = messageSource.getMessage("validation.parameter.invalid", 
        new Object[]{param, expected, invalid}, locale);

    var error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        message,
        messageSource.getMessage("error.badrequest", null, locale)
    );

    return ResponseEntity.badRequest().body(error);
}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        log.error("Unexpected error occurred: ", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            messageSource.getMessage("error.unexpected", null, locale),
            "Internal Server Error"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            messageSource.getMessage("error.notfound", null, locale)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            messageSource.getMessage("error.badrequest", null, locale)
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            ex.getMessage(),
            messageSource.getMessage("error.forbidden", null, locale)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            messageSource.getMessage("error.insufficientstock", null, locale)
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatus(
            InvalidOrderStatusException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            messageSource.getMessage("error.invalidorderstatus", null, locale)
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            messageSource.getMessage("error.conflict", null, locale)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}

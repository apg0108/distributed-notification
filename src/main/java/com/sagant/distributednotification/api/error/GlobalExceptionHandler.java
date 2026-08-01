package com.sagant.distributednotification.api.error;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sagant.distributednotification.domain.exception.NotificationNotFoundException;
import com.sagant.distributednotification.domain.model.ErrorResponse;
import com.sagant.distributednotification.domain.model.ErrorResponse.FieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
      final List<FieldError> fieldErrors = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
      final ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation failed", "One or more fields are invalid", fieldErrors);
      return ResponseEntity.badRequest().body(body);
   }

   @ExceptionHandler(NotificationNotFoundException.class)
   public ResponseEntity<ErrorResponse> handleNotFound(final NotificationNotFoundException ex) {
      final ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "Not found", ex.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ErrorResponse> handleGeneric(final Exception ex) {
      if (ex instanceof org.springframework.web.ErrorResponse springErrorResponse) {
         final ErrorResponse body = ErrorResponse.of(springErrorResponse.getStatusCode().value(),
               Objects.requireNonNullElse(springErrorResponse.getBody().getTitle(), "Error"),
               Objects.requireNonNullElse(springErrorResponse.getBody().getDetail(), "Request could not be processed"));
         return ResponseEntity.status(springErrorResponse.getStatusCode()).body(body);
      }

      final ErrorResponse body = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", "An unexpected error occurred");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
   }
}

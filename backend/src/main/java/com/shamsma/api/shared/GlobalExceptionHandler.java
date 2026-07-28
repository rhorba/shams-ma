package com.shamsma.api.shared;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
class GlobalExceptionHandler {

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(java.util.Map.of("error", "File exceeds maximum size of 10MB"));
  }

  // @Validated constraints on individual @RequestParam/@PathVariable method params (as opposed to
  // @Valid @RequestBody, which Spring already maps to 400) surface as this unhandled exception
  // instead of an automatic 400 — found via RoiController's query-param validation.
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(java.util.Map.of("error", ex.getMessage()));
  }
}

package com.shamsma.api.shared;

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
}

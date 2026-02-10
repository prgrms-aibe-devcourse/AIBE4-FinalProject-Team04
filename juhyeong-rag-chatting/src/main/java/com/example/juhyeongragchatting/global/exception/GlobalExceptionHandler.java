package com.example.juhyeongragchatting.global.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateResourceException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(Map.of("message", e.getMessage()));
	}
}

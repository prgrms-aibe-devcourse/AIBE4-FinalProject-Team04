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
		Map<String, String> body = new java.util.HashMap<>();
		body.put("message", e.getMessage());
		if (e.getCode() != null) {
			body.put("code", e.getCode());
		}
		if (e.getDuplicateFileId() != null) {
			body.put("duplicateFileId", e.getDuplicateFileId());
		}
		if (e.getDuplicateFileName() != null) {
			body.put("duplicateFileName", e.getDuplicateFileName());
		}
		if (e.getDuplicateVersion() != null) {
			body.put("duplicateVersion", e.getDuplicateVersion());
		}
		if (e.getDuplicateGroupId() != null) {
			body.put("duplicateGroupId", e.getDuplicateGroupId());
		}
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}
}

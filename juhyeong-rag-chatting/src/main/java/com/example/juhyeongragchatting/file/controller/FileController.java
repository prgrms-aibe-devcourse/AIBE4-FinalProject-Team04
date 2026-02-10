package com.example.juhyeongragchatting.file.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.juhyeongragchatting.file.dto.FileInfoResponse;
import com.example.juhyeongragchatting.file.dto.FileUploadRequest;
import com.example.juhyeongragchatting.file.service.FileStorageService;
import com.example.juhyeongragchatting.global.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

	private final FileStorageService fileStorageService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileInfoResponse> uploadFile(
		@RequestPart("metadata") FileUploadRequest request, @RequestPart("file") MultipartFile file
	) throws IOException {
		FileInfoResponse response = fileStorageService.uploadFile(request, file);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<PageResponse<FileInfoResponse>> getAllFiles(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return ResponseEntity.ok(fileStorageService.getAllFiles(page, size));
	}

	@PatchMapping("/{fileId}/category")
	public ResponseEntity<FileInfoResponse> updateCategory(
		@PathVariable Long fileId,
		@RequestBody Map<String, String> body
	) {
		FileInfoResponse response = fileStorageService.updateCategory(fileId, body.get("fileCategory"));
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{fileId}")
	public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
		fileStorageService.deleteFile(fileId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{fileId}/download")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
		Resource resource = fileStorageService.loadFileAsResource(fileId);
		String originalFileName = fileStorageService.getOriginalFileName(fileId);
		String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replace("+", "%20");

		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
			.body(resource);
	}
}

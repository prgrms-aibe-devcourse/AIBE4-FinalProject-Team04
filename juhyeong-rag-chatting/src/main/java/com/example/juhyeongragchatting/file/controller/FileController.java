package com.example.juhyeongragchatting.file.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.juhyeongragchatting.file.dto.FileInfoResponse;
import com.example.juhyeongragchatting.file.dto.FileUploadRequest;
import com.example.juhyeongragchatting.file.service.FileStorageService;

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

	@PostMapping(value = "/{fileId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileInfoResponse> uploadNewVersion(
		@PathVariable Long fileId,
		@RequestPart("metadata") FileUploadRequest request,
		@RequestPart("file") MultipartFile file
	) throws IOException {
		FileInfoResponse response = fileStorageService.uploadNewVersion(fileId, request, file);
		return ResponseEntity.ok(response);
	}

	@PutMapping(value = "/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileInfoResponse> replaceFile(
		@PathVariable Long fileId,
		@RequestPart("file") MultipartFile file
	) throws IOException {
		FileInfoResponse response = fileStorageService.replaceFile(fileId, file);
		return ResponseEntity.ok(response);
	}

	@PatchMapping(value = "/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileInfoResponse> updateFile(
		@PathVariable Long fileId,
		@RequestPart(value = "metadata", required = false) FileUploadRequest request,
		@RequestPart(value = "file", required = false) MultipartFile file
	) throws IOException {
		FileInfoResponse response = fileStorageService.updateFile(fileId, request, file);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<Page<FileInfoResponse>> getAllFiles(Pageable pageable) {
		return ResponseEntity.ok(fileStorageService.getAllFiles(pageable));
	}

	@GetMapping("/{fileId}")
	public ResponseEntity<FileInfoResponse> getFile(@PathVariable Long fileId) {
		return ResponseEntity.ok(fileStorageService.getFile(fileId));
	}

	@GetMapping("/groups/{groupId}/versions")
	public ResponseEntity<List<FileInfoResponse>> getGroupVersions(@PathVariable Long groupId) {
		return ResponseEntity.ok(fileStorageService.getGroupVersions(groupId));
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

	@GetMapping("/{fileId}/preview")
	public ResponseEntity<Resource> previewFile(@PathVariable Long fileId) {
		Resource resource = fileStorageService.loadFileAsResource(fileId);
		String originalFileName = fileStorageService.getOriginalFileName(fileId);
		String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replace("+", "%20");
		String contentType = fileStorageService.getContentType(fileId);

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
			.body(resource);
	}
}

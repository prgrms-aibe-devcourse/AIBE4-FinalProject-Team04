package com.example.juhyeongragchatting.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.juhyeongragchatting.file.dto.FileInfoResponse;
import com.example.juhyeongragchatting.file.dto.FileUploadRequest;
import com.example.juhyeongragchatting.file.model.FileMetadata;
import com.example.juhyeongragchatting.file.repository.FileMetadataRepository;
import com.example.juhyeongragchatting.global.dto.PageResponse;
import com.example.juhyeongragchatting.global.exception.DuplicateResourceException;
import com.example.juhyeongragchatting.global.exception.ResourceNotFoundException;
import com.example.juhyeongragchatting.rag.service.DocumentIngestionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageService {

	private final Path uploadDir;
	private final DocumentIngestionService documentIngestionService;
	private final FileMetadataRepository fileMetadataRepository;

	public FileStorageService(
		@Value("${app.upload.dir}") String uploadPath,
		DocumentIngestionService documentIngestionService,
		FileMetadataRepository fileMetadataRepository
	) {
		this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
		this.documentIngestionService = documentIngestionService;
		this.fileMetadataRepository = fileMetadataRepository;
		try {
			Files.createDirectories(this.uploadDir);
		} catch (IOException e) {
			throw new RuntimeException("업로드 디렉터리를 생성할 수 없습니다: " + uploadPath, e);
		}
	}

	@Transactional
	public FileInfoResponse uploadFile(FileUploadRequest request, MultipartFile file) throws IOException {
		String originalFileName = file.getOriginalFilename();

		String fileHash = computeSha256(file);
		checkDuplicate(originalFileName, fileHash);

		String fileName = extractFileNameWithoutExtension(originalFileName);
		String fileVersion = resolveNextVersion(originalFileName);

		FileMetadata metadata = FileMetadata.builder()
			.fileName(fileName)
			.fileVersion(fileVersion)
			.fileCategory(request.fileCategory())
			.storedFilePath("pending")
			.originalFileName(originalFileName)
			.uploadedAt(LocalDateTime.now())
			.fileHash(fileHash)
			.build();

		FileMetadata saved = fileMetadataRepository.save(metadata);

		String storedFileName = saved.getId() + "_" + originalFileName;
		Path targetPath = uploadDir.resolve(storedFileName);

		Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

		List<String> chunkIds = documentIngestionService.ingest(targetPath, saved);

		FileMetadata metadataWithChunks = saved.toBuilder()
			.storedFilePath(storedFileName)
			.chunkIds(chunkIds)
			.build();

		fileMetadataRepository.save(metadataWithChunks);

		return toResponse(metadataWithChunks);
	}

	public PageResponse<FileInfoResponse> getAllFiles(int page, int size) {
		Page<FileMetadata> result = fileMetadataRepository.findAll(PageRequest.of(page, size));

		List<FileInfoResponse> content = result.getContent().stream()
			.map(this::toResponse)
			.toList();

		return new PageResponse<>(
			content,
			page,
			size,
			result.getTotalElements(),
			result.getTotalPages()
		);
	}

	@Transactional
	public FileInfoResponse updateCategory(Long fileId, String fileCategory) {
		FileMetadata metadata = findByIdOrThrow(fileId);

		FileMetadata updated = metadata.toBuilder()
			.fileCategory(fileCategory)
			.build();
		fileMetadataRepository.save(updated);
		log.info("카테고리 변경 - id={}, '{}' -> '{}'", fileId, metadata.getFileCategory(), fileCategory);
		return toResponse(updated);
	}

	public Resource loadFileAsResource(Long fileId) {
		FileMetadata metadata = findByIdOrThrow(fileId);

		try {
			Path filePath = uploadDir.resolve(metadata.getStoredFilePath()).normalize();
			Resource resource = new UrlResource(filePath.toUri());
			if (resource.exists()) {
				return resource;
			}
			throw new ResourceNotFoundException("파일이 존재하지 않습니다: " + metadata.getStoredFilePath());
		} catch (MalformedURLException e) {
			throw new RuntimeException("파일 경로 오류: " + metadata.getStoredFilePath(), e);
		}
	}

	@Transactional
	public void deleteFile(Long fileId) {
		FileMetadata metadata = findByIdOrThrow(fileId);

		documentIngestionService.delete(metadata.getChunkIds());

		try {
			Path filePath = uploadDir.resolve(metadata.getStoredFilePath()).normalize();
			Files.deleteIfExists(filePath);
		} catch (IOException e) {
			throw new RuntimeException("파일 삭제 오류: " + metadata.getStoredFilePath(), e);
		}

		fileMetadataRepository.deleteById(fileId);
		log.info("파일 삭제 완료 - id={}, name='{}'", fileId, metadata.getOriginalFileName());
	}

	public String getOriginalFileName(Long fileId) {
		return findByIdOrThrow(fileId).getOriginalFileName();
	}

	public Collection<FileMetadata> getAllMetadata() {
		return fileMetadataRepository.findAll();
	}

	public List<String> getDistinctCategories() {
		return fileMetadataRepository.findDistinctCategories();
	}

	public List<String> getDistinctOriginalFileNames() {
		return fileMetadataRepository.findDistinctOriginalFileNames();
	}

	public List<String> getVersionsByOriginalFileName(String originalFileName) {
		return fileMetadataRepository.findVersionsByOriginalFileName(originalFileName);
	}

	public List<FileMetadata> getByCategory(String category) {
		return fileMetadataRepository.findByFileCategory(category);
	}

	public List<FileMetadata> getByOriginalFileName(String originalFileName) {
		return fileMetadataRepository.findByOriginalFileName(originalFileName);
	}

	private String computeSha256(MultipartFile file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream is = file.getInputStream();
				 DigestInputStream dis = new DigestInputStream(is, digest)
			) {
				dis.readAllBytes();
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256을 지원하지 않습니다.", e);
		}
	}

	private void checkDuplicate(String originalFileName, String fileHash) {
		Optional<FileMetadata> duplicate = fileMetadataRepository
			.findFirstByOriginalFileNameAndFileHash(originalFileName, fileHash);
		duplicate.ifPresent(m -> {
			throw new DuplicateResourceException(
				"동일한 파일이 이미 존재합니다: " + originalFileName + " (" + m.getFileVersion() + ")"
			);
		});
	}

	private FileMetadata findByIdOrThrow(Long fileId) {
		return fileMetadataRepository.findById(fileId)
			.orElseThrow(() -> new ResourceNotFoundException("파일을 찾을 수 없습니다. ID: " + fileId));
	}

	private String extractFileNameWithoutExtension(String originalFileName) {
		if (originalFileName == null) {
			return "unknown";
		}
		int dotIndex = originalFileName.lastIndexOf(".");
		return dotIndex > 0 ? originalFileName.substring(0, dotIndex) : originalFileName;
	}

	private String resolveNextVersion(String originalFileName) {
		long current = fileMetadataRepository.findTopByOriginalFileNameOrderByIdDesc(originalFileName)
			.map(FileMetadata::getFileVersion)
			.map(this::extractVersionNumber)
			.orElse(0L);
		long next = current + 1;
		return "v" + next;
	}

	private long extractVersionNumber(String version) {
		if (version == null || !version.startsWith("v")) {
			return 0L;
		}
		try {
			return Long.parseLong(version.substring(1));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private FileInfoResponse toResponse(FileMetadata metadata) {
		String originalFileName = metadata.getOriginalFileName();
		String extension = "";
		if (originalFileName != null && originalFileName.contains(".")) {
			extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toUpperCase();
		}
		return new FileInfoResponse(
			metadata.getId(),
			metadata.getFileName(),
			metadata.getFileVersion(),
			metadata.getFileCategory(),
			originalFileName,
			extension,
			"/api/files/" + metadata.getId() + "/download",
			metadata.getUploadedAt()
		);
	}
}

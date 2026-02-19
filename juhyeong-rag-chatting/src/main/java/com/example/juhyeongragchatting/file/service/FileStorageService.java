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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.juhyeongragchatting.file.dto.FileGroupResponse;
import com.example.juhyeongragchatting.file.dto.FileInfoResponse;
import com.example.juhyeongragchatting.file.dto.FileUploadRequest;
import com.example.juhyeongragchatting.file.model.FileGroup;
import com.example.juhyeongragchatting.file.model.FileMetadata;
import com.example.juhyeongragchatting.file.repository.FileGroupRepository;
import com.example.juhyeongragchatting.file.repository.FileMetadataRepository;
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
	private final FileGroupRepository fileGroupRepository;

	public FileStorageService(
		@Value("${app.upload.dir}") String uploadPath,
		DocumentIngestionService documentIngestionService,
		FileMetadataRepository fileMetadataRepository,
		FileGroupRepository fileGroupRepository
	) {
		this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
		this.documentIngestionService = documentIngestionService;
		this.fileMetadataRepository = fileMetadataRepository;
		this.fileGroupRepository = fileGroupRepository;
		try {
			Files.createDirectories(this.uploadDir);
		} catch (IOException e) {
			throw new RuntimeException("업로드 디렉터리를 생성할 수 없습니다: " + uploadPath, e);
		}
	}

	@Transactional
	public FileInfoResponse uploadFile(FileUploadRequest request, MultipartFile file) throws IOException {
		String originalFileName = file.getOriginalFilename();
		if (originalFileName == null || originalFileName.isBlank()) {
			throw new IllegalArgumentException("파일명이 비어 있습니다.");
		}

		String groupName = request.groupName();
		if (groupName == null || groupName.isBlank()) {
			throw new IllegalArgumentException("그룹명을 입력해 주세요.");
		}

		String fileHash = computeSha256(file);
		validateNewFileUpload(originalFileName, groupName, request.fileCategory(), fileHash);

		FileGroup fileGroup = findOrCreateGroup(groupName.trim(), request.fileCategory());

		Version next = parseVersion(request);
		LocalDateTime now = LocalDateTime.now();

		String storedFileName = UUID.randomUUID() + "_" + originalFileName;
		Path targetPath = uploadDir.resolve(storedFileName);
		Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

		FileMetadata metadata = FileMetadata.builder()
			.fileName(extractFileNameWithoutExtension(originalFileName))
			.majorVersion(next.major())
			.minorVersion(next.minor())
			.patchVersion(next.patch())
			.fileGroup(fileGroup)
			.storedFilePath(storedFileName)
			.originalFileName(originalFileName)
			.uploadedAt(now)
			.updatedAt(now)
			.fileHash(fileHash)
			.build();

		FileMetadata saved = fileMetadataRepository.save(metadata);

		List<String> chunkIds = documentIngestionService.ingest(targetPath, saved);

		FileMetadata metadataWithChunks = saved.toBuilder()
			.chunkIds(chunkIds)
			.build();

		fileMetadataRepository.save(metadataWithChunks);
		return toResponse(metadataWithChunks);
	}

	@Transactional
	public FileInfoResponse uploadNewVersion(Long fileId, FileUploadRequest request, MultipartFile file) throws IOException {
		FileMetadata base = findByIdOrThrow(fileId);
		FileGroup fileGroup = base.getFileGroup();
		String newOriginalFileName = file.getOriginalFilename();
		if (newOriginalFileName == null || newOriginalFileName.isBlank()) {
			throw new IllegalArgumentException("파일명이 비어 있습니다.");
		}
		String fileHash = computeSha256(file);
		Version next = parseVersion(request);
		validateNewVersionUpload(fileGroup, newOriginalFileName, fileHash, next);

		String storedFileName = UUID.randomUUID() + "_" + newOriginalFileName;
		Path targetPath = uploadDir.resolve(storedFileName);
		Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

		LocalDateTime now = LocalDateTime.now();

		FileMetadata metadata = FileMetadata.builder()
			.fileName(extractFileNameWithoutExtension(newOriginalFileName))
			.majorVersion(next.major())
			.minorVersion(next.minor())
			.patchVersion(next.patch())
			.fileGroup(fileGroup)
			.storedFilePath(storedFileName)
			.originalFileName(newOriginalFileName)
			.uploadedAt(now)
			.updatedAt(now)
			.fileHash(fileHash)
			.build();

		FileMetadata saved = fileMetadataRepository.save(metadata);

		List<String> chunkIds = documentIngestionService.ingest(targetPath, saved);

		FileMetadata metadataWithChunks = saved.toBuilder()
			.chunkIds(chunkIds)
			.build();

		fileMetadataRepository.save(metadataWithChunks);
		return toResponse(metadataWithChunks);
	}

	@Transactional
	public FileInfoResponse replaceFile(Long fileId, MultipartFile file) throws IOException {
		FileMetadata metadata = findByIdOrThrow(fileId);
		Version current = new Version(metadata.getMajorVersion(), metadata.getMinorVersion(), metadata.getPatchVersion());
		return replaceFileWithCategory(metadata, file, metadata.getFileCategory(), current);
	}

	public Page<FileInfoResponse> getAllFiles(Pageable pageable) {
		return fileMetadataRepository.findLatestPerGroup(pageable).map(m -> {
			int count = fileMetadataRepository.countByFileGroupId(m.getFileGroup().getId());
			return toResponse(m, count);
		});
	}

	public FileInfoResponse getFile(Long fileId) {
		return toResponse(findByIdOrThrow(fileId));
	}

	public String getStoredFilePath(Long fileId) {
		return findByIdOrThrow(fileId).getStoredFilePath();
	}

	public String getContentType(Long fileId) {
		try {
			Path filePath = uploadDir.resolve(getStoredFilePath(fileId)).normalize();
			String contentType = Files.probeContentType(filePath);
			return contentType != null ? contentType : "application/octet-stream";
		} catch (IOException e) {
			return "application/octet-stream";
		}
	}

	@Transactional
	public FileInfoResponse updateCategory(Long fileId, String fileCategory) {
		FileMetadata metadata = findByIdOrThrow(fileId);

		FileGroup fileGroup = metadata.getFileGroup();
		String oldCategory = fileGroup.getCategory();
		fileGroup.setCategory(fileCategory);
		fileGroupRepository.save(fileGroup);

		FileMetadata updated = metadata.toBuilder()
			.updatedAt(LocalDateTime.now())
			.build();
		fileMetadataRepository.save(updated);

		log.info("카테고리 변경 - id={}, '{}' -> '{}'", fileId, oldCategory, fileCategory);
		return toResponse(updated);
	}

	@Transactional
	public FileInfoResponse updateCategoryAndVersion(Long fileId, String fileCategory, Version version) {
		FileMetadata metadata = findByIdOrThrow(fileId);

		FileGroup fileGroup = metadata.getFileGroup();
		fileGroup.setCategory(fileCategory);
		fileGroupRepository.save(fileGroup);

		FileMetadata updated = metadata.toBuilder()
			.majorVersion(version.major())
			.minorVersion(version.minor())
			.patchVersion(version.patch())
			.updatedAt(LocalDateTime.now())
			.build();
		fileMetadataRepository.save(updated);

		log.info("카테고리/버전 변경 - id={}, '{}'", fileId, updated.getVersionString());
		return toResponse(updated);
	}

	@Transactional
	public FileInfoResponse updateFile(Long fileId, FileUploadRequest request, MultipartFile file) throws IOException {
		FileMetadata metadata = findByIdOrThrow(fileId);
		if (request == null) {
			throw new IllegalArgumentException("버전 정보를 입력해 주세요.");
		}
		boolean hasNewFile = file != null && !file.isEmpty();
		boolean hasCategory = request.fileCategory() != null && !request.fileCategory().isBlank();
		Version newVersion = parseVersion(request);

		if (!hasNewFile && !hasCategory) {
			throw new IllegalArgumentException("변경할 내용이 없습니다.");
		}

		String newCategory = hasCategory ? request.fileCategory() : metadata.getFileCategory();
		boolean categoryChanged = !equalsNullable(metadata.getFileCategory(), newCategory);
		boolean versionChanged = metadata.getMajorVersion() != newVersion.major()
			|| metadata.getMinorVersion() != newVersion.minor()
			|| metadata.getPatchVersion() != newVersion.patch();

		if (hasNewFile) {
			return replaceFileWithCategory(metadata, file, newCategory, newVersion);
		}

		if (!categoryChanged && !versionChanged) {
			return toResponse(metadata);
		}

		if (existsVersionConflict(metadata, newVersion)) {
			throw new DuplicateResourceException("동일한 버전이 이미 존재합니다.", "DUPLICATE_VERSION");
		}

		return updateCategoryAndVersion(fileId, newCategory, newVersion);
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

	public Map<Long, FileMetadata> getMetadataByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Map.of();
		}
		return fileMetadataRepository.findAllById(ids).stream()
			.collect(Collectors.toMap(FileMetadata::getId, m -> m));
	}

	public List<String> getDistinctCategories() {
		return fileGroupRepository.findDistinctCategories();
	}

	public List<FileGroupResponse> getDistinctGroups() {
		return fileGroupRepository.findAllOrdered().stream()
			.map(fg -> new FileGroupResponse(fg.getId(), fg.getGroupName(), fg.getCategory()))
			.toList();
	}

	public List<String> getVersionsByGroupId(Long groupId) {
		return fileMetadataRepository.findByFileGroupIdOrderByVersion(groupId).stream()
			.map(FileMetadata::getVersionString)
			.toList();
	}

	public List<FileMetadata> getByCategory(String category) {
		return fileMetadataRepository.findByFileGroup_Category(category);
	}

	public List<FileMetadata> getByGroupId(Long groupId) {
		return fileMetadataRepository.findByFileGroupId(groupId);
	}

	public List<FileInfoResponse> getGroupVersions(Long groupId) {
		return fileMetadataRepository.findByFileGroupIdOrderByVersionDesc(groupId).stream()
			.map(this::toResponse)
			.toList();
	}

	private FileGroup findOrCreateGroup(String groupName, String category) {
		return fileGroupRepository.findByGroupNameAndCategory(groupName, category)
			.orElseGet(() -> {
				LocalDateTime now = LocalDateTime.now();
				FileGroup newGroup = FileGroup.builder()
					.groupName(groupName)
					.category(category)
					.createdAt(now)
					.updatedAt(now)
					.build();
				return fileGroupRepository.save(newGroup);
			});
	}

	private void validateNewFileUpload(String originalFileName, String groupName, String category, String fileHash) {
		fileMetadataRepository.findFirstByFileHash(fileHash).ifPresent(found -> {
			throw new DuplicateResourceException(
				"동일한 내용의 파일이 이미 존재합니다: " + found.getOriginalFileName()
					+ " (" + found.getVersionString() + ")",
				"DUPLICATE_CONTENT",
				found
			);
		});

		if (fileGroupRepository.existsByGroupNameAndCategory(groupName.trim(), category)) {
			throw new DuplicateResourceException(
				"해당 카테고리에 동일한 그룹명이 이미 존재합니다: " + groupName,
				"DUPLICATE_GROUP"
			);
		}

		if (fileMetadataRepository.existsByOriginalFileName(originalFileName)) {
			throw new DuplicateResourceException(
				"동일한 파일명이 이미 존재합니다. 새 버전 업로드를 사용하세요.",
				"DUPLICATE_NAME"
			);
		}
	}

	private void validateNewVersionUpload(FileGroup fileGroup, String originalFileName, String fileHash, Version version) {
		Long groupId = fileGroup.getId();
		if (fileMetadataRepository.existsByFileGroupIdAndMajorVersionAndMinorVersionAndPatchVersion(
			groupId, version.major(), version.minor(), version.patch())) {
			throw new DuplicateResourceException("동일한 버전이 이미 존재합니다.", "DUPLICATE_VERSION");
		}
		fileMetadataRepository.findFirstByFileHash(fileHash).ifPresent(found -> {
			if (found.getGroupId().equals(groupId)) {
				throw new DuplicateResourceException(
					"동일한 내용의 파일이 이미 존재합니다: " + found.getOriginalFileName()
						+ " (" + found.getVersionString() + ")",
					"DUPLICATE_SAME_GROUP",
					found
				);
			}
			throw new DuplicateResourceException(
				"동일한 내용의 파일이 다른 파일명으로 존재합니다: " + found.getOriginalFileName()
					+ " (" + found.getVersionString() + "). 파일 수정 기능을 사용하세요.",
				"DUPLICATE_OTHER_GROUP",
				found
			);
		});

		if (fileMetadataRepository.existsByOriginalFileNameAndFileGroupIdNot(originalFileName, groupId)) {
			throw new DuplicateResourceException("다른 파일 그룹에서 사용 중인 파일명입니다.", "DUPLICATE_NAME");
		}
	}

	private FileInfoResponse replaceFileWithCategory(
		FileMetadata metadata, MultipartFile file, String fileCategory, Version newVersion
	) throws IOException {
		String newOriginalFileName = file.getOriginalFilename();
		if (newOriginalFileName == null || newOriginalFileName.isBlank()) {
			throw new IllegalArgumentException("파일명이 비어 있습니다.");
		}

		String fileHash = computeSha256(file);
		if (fileMetadataRepository.existsByOriginalFileNameAndFileGroupIdNot(newOriginalFileName, metadata.getGroupId())) {
			throw new DuplicateResourceException("다른 파일 그룹에서 사용 중인 파일명입니다.", "DUPLICATE_NAME");
		}
		validateModifyContent(metadata, fileHash);

		if (existsVersionConflict(metadata, newVersion)) {
			throw new DuplicateResourceException("동일한 버전이 이미 존재합니다.", "DUPLICATE_VERSION");
		}

		FileGroup fileGroup = metadata.getFileGroup();
		if (!fileGroup.getCategory().equals(fileCategory)) {
			fileGroup.setCategory(fileCategory);
			fileGroupRepository.save(fileGroup);
		}

		String storedFileName = UUID.randomUUID() + "_" + newOriginalFileName;
		Path targetPath = uploadDir.resolve(storedFileName);
		Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

		FileMetadata updatedMeta = metadata.toBuilder()
			.fileName(extractFileNameWithoutExtension(newOriginalFileName))
			.majorVersion(newVersion.major())
			.minorVersion(newVersion.minor())
			.patchVersion(newVersion.patch())
			.originalFileName(newOriginalFileName)
			.fileGroup(fileGroup)
			.storedFilePath(storedFileName)
			.updatedAt(LocalDateTime.now())
			.fileHash(fileHash)
			.build();

		List<String> newChunkIds = documentIngestionService.ingest(targetPath, updatedMeta);
		documentIngestionService.delete(metadata.getChunkIds());

		Path oldFilePath = uploadDir.resolve(metadata.getStoredFilePath()).normalize();
		Files.deleteIfExists(oldFilePath);

		FileMetadata saved = updatedMeta.toBuilder()
			.chunkIds(newChunkIds)
			.build();

		fileMetadataRepository.save(saved);
		log.info("파일 교체 완료 - id={}, name='{}'", metadata.getId(), newOriginalFileName);
		return toResponse(saved);
	}

	private void validateModifyContent(FileMetadata current, String fileHash) {
		fileMetadataRepository.findFirstByFileHash(fileHash).ifPresent(found -> {
			if (!found.getId().equals(current.getId())) {
				throw new DuplicateResourceException(
					"동일한 내용의 파일이 다른 파일명으로 존재합니다: " + found.getOriginalFileName()
						+ " (" + found.getVersionString() + ")",
					"DUPLICATE_OTHER_GROUP",
					found
				);
			}
		});
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

	private Version parseVersion(FileUploadRequest request) {
		if (request == null
			|| request.majorVersion() == null
			|| request.minorVersion() == null
			|| request.patchVersion() == null) {
			throw new IllegalArgumentException("버전 정보를 입력해 주세요.");
		}
		int major = request.majorVersion();
		int minor = request.minorVersion();
		int patch = request.patchVersion();
		if (major < 0 || minor < 0 || patch < 0) {
			throw new IllegalArgumentException("버전은 0 이상의 숫자만 가능합니다.");
		}
		return new Version(major, minor, patch);
	}

	private boolean existsVersionConflict(FileMetadata metadata, Version version) {
		return fileMetadataRepository.existsVersionInGroupExcludingId(
			metadata.getGroupId(), version.major(), version.minor(), version.patch(), metadata.getId()
		);
	}

	private record Version(int major, int minor, int patch) {}

	private FileInfoResponse toResponse(FileMetadata metadata) {
		return toResponse(metadata, null);
	}

	private FileInfoResponse toResponse(FileMetadata metadata, Integer fileCount) {
		String originalFileName = metadata.getOriginalFileName();
		String extension = "";
		if (originalFileName != null && originalFileName.contains(".")) {
			extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toUpperCase();
		}
		return new FileInfoResponse(
			metadata.getId(),
			metadata.getFileName(),
			metadata.getVersionString(),
			metadata.getFileCategory(),
			originalFileName,
			extension,
			"/api/files/" + metadata.getId() + "/download",
			metadata.getUploadedAt(),
			metadata.getUpdatedAt(),
			metadata.getGroupId(),
			metadata.getGroupName(),
			fileCount
		);
	}

	private static boolean equalsNullable(String a, String b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
}

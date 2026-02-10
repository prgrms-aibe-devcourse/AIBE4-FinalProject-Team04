package com.example.juhyeongragchatting.global.exception;

public class DuplicateResourceException extends RuntimeException {

	private final String code;
	private final String duplicateFileId;
	private final String duplicateFileName;
	private final String duplicateVersion;
	private final String duplicateGroupId;

	public DuplicateResourceException(String message, String code) {
		super(message);
		this.code = code;
		this.duplicateFileId = null;
		this.duplicateFileName = null;
		this.duplicateVersion = null;
		this.duplicateGroupId = null;
	}

	public DuplicateResourceException(String message, String code, String duplicateFileId, String duplicateFileName,
		String duplicateVersion, String duplicateGroupId) {
		super(message);
		this.code = code;
		this.duplicateFileId = duplicateFileId;
		this.duplicateFileName = duplicateFileName;
		this.duplicateVersion = duplicateVersion;
		this.duplicateGroupId = duplicateGroupId;
	}

	public DuplicateResourceException(String message, String code, com.example.juhyeongragchatting.file.model.FileMetadata file) {
		this(
			message,
			code,
			file != null && file.getId() != null ? String.valueOf(file.getId()) : null,
			file != null ? file.getOriginalFileName() : null,
			file != null ? file.getVersionString() : null,
			file != null && file.getGroupId() != null ? String.valueOf(file.getGroupId()) : null
		);
	}

	public DuplicateResourceException(String message) {
		super(message);
		this.code = null;
		this.duplicateFileId = null;
		this.duplicateFileName = null;
		this.duplicateVersion = null;
		this.duplicateGroupId = null;
	}

	public String getCode() {
		return code;
	}

	public String getDuplicateFileId() {
		return duplicateFileId;
	}

	public String getDuplicateFileName() {
		return duplicateFileName;
	}

	public String getDuplicateVersion() {
		return duplicateVersion;
	}

	public String getDuplicateGroupId() {
		return duplicateGroupId;
	}
}

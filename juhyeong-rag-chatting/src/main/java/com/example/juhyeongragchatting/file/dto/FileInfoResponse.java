package com.example.juhyeongragchatting.file.dto;

import java.time.LocalDateTime;

public record FileInfoResponse(
	Long fileId,
	String fileName,
	String fileVersion,
	String fileCategory,
	String originalFileName,
	String fileExtension,
	String downloadUrl,
	LocalDateTime uploadedAt,
	LocalDateTime updatedAt,
	Long groupId,
	String groupName,
	Integer fileCount
) {
}

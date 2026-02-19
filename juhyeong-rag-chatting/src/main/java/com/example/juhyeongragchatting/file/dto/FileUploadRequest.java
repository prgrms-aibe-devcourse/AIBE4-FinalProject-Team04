package com.example.juhyeongragchatting.file.dto;

public record FileUploadRequest(
	String groupName,
	String fileCategory,
	Integer majorVersion,
	Integer minorVersion,
	Integer patchVersion
) {
}

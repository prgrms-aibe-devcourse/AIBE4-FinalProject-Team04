package com.example.juhyeongragchatting.chat.dto;

public record ChatReference(
	String originalFileName,
	String fileVersion,
	String fileCategory,
	String chunkText
) {
}

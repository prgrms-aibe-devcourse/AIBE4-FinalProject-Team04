package com.example.juhyeongragchatting.chat.dto;

import java.util.List;

public record ChatRequest(
	String conversationId,
	String message,
	String systemMessage,
	SearchScope scope,
	VersionPolicy versionPolicy,
	String filterValue,
	List<String> versions
) {
}

package com.example.juhyeongragchatting.chat.dto;

import java.util.List;

public record SearchFilterResult(
	List<Long> fileIds,
	String description
) {
}

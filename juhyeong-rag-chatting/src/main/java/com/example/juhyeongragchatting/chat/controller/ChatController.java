package com.example.juhyeongragchatting.chat.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.juhyeongragchatting.chat.dto.ChatRequest;
import com.example.juhyeongragchatting.chat.service.ChatService;
import com.example.juhyeongragchatting.file.dto.FileGroupResponse;
import com.example.juhyeongragchatting.file.service.FileSearchService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final FileSearchService fileSearchService;

	@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chat(@RequestBody ChatRequest request) {
		return chatService.chat(request);
	}

	@GetMapping("/categories")
	public List<String> getCategories() {
		return fileSearchService.getDistinctCategories();
	}

	@GetMapping("/groups")
	public List<FileGroupResponse> getGroups() {
		return fileSearchService.getDistinctGroups();
	}

	@GetMapping("/groups/{groupId}/versions")
	public List<String> getVersions(@PathVariable Long groupId) {
		return fileSearchService.getVersionsByGroupId(groupId);
	}
}

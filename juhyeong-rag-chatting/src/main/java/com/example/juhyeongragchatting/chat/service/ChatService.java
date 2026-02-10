package com.example.juhyeongragchatting.chat.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.example.juhyeongragchatting.chat.dto.ChatReference;
import com.example.juhyeongragchatting.chat.dto.ChatRequest;
import com.example.juhyeongragchatting.chat.dto.SearchFilterResult;
import com.example.juhyeongragchatting.file.service.FileSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ChatService {

	private final ChatClient chatClient;
	private final FileSearchService fileSearchService;
	private final ObjectMapper objectMapper;
	private final String serverSystemMessage;

	public ChatService(
		ChatClient.Builder chatClientBuilder,
		Advisor[] advisors,
		FileSearchService fileSearchService,
		ObjectMapper objectMapper,
		@Value("${app.chat.system-message}") String serverSystemMessage
	) {
		this.chatClient = chatClientBuilder.defaultAdvisors(advisors).build();
		this.fileSearchService = fileSearchService;
		this.objectMapper = objectMapper;
		this.serverSystemMessage = serverSystemMessage;
	}

	public Flux<ServerSentEvent<String>> chat(ChatRequest request) {
		SearchFilterResult filterResult = fileSearchService.resolveFileIds(
			request.scope(), request.versionPolicy(),
			request.filterValue(), request.versions()
		);

		if (filterResult.fileIds().isEmpty()) {
			return errorEvents("검색 대상 문서가 없습니다. 먼저 파일을 업로드해 주세요.");
		}

		String filterExpression = buildFilterExpression(filterResult.fileIds());
		log.debug("Chat filter expression: {}", filterExpression);
		log.info("[사용자질문] {}", request.message());

		AtomicReference<List<ChatReference>> referencesHolder = new AtomicReference<>(List.of());
		AtomicBoolean referencesExtracted = new AtomicBoolean(false);

		String systemMessage = buildSystemMessage(request.systemMessage());
		String conversationId = request.conversationId() != null
			? request.conversationId()
			: ChatMemory.DEFAULT_CONVERSATION_ID;

		// 1. 프롬프트 구성
		ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt()
			.system(systemMessage)
			.user(request.message())
			.advisors(advisor -> {
				advisor.param(ChatMemory.CONVERSATION_ID, conversationId);
				advisor.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression);
			});

		// 2. 스트리밍 응답 수신
		Flux<ChatClientResponse> responseStream = promptSpec.stream().chatClientResponse();

		// 3. 응답에서 관련 문서 추출 + 참조 문서 수집
		Flux<ServerSentEvent<String>> tokenEvents = responseStream.mapNotNull(response -> {
			extractReferences(response, referencesHolder, referencesExtracted);
			return toTokenEvent(response);
		});

		// 4. 스트리밍 종료 후 참조 문서 및 완료 이벤트 추가
		Flux<ServerSentEvent<String>> endEvents = Flux.defer(
			() -> buildEndEvents(referencesHolder.get(), filterResult.description())
		);

		// 5. 최종 조합: 토큰 이벤트와 종료 이벤트를 순서대로 처리
		return tokenEvents
			.concatWith(endEvents)
			.onErrorResume(e -> {
				log.error("Chat streaming error", e);
				return errorEvents("응답 생성 중 오류가 발생했습니다: " + e.getMessage());
			});
	}

	// ==================== 스트리밍 헬퍼 ====================

	private void extractReferences(
		ChatClientResponse response,
		AtomicReference<List<ChatReference>> holder,
		AtomicBoolean extracted
	) {
		if (extracted.get() || response.context() == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Document> docs = (List<Document>)response.context()
			.get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
		if (docs != null && !docs.isEmpty()) {
			extracted.set(true);
			holder.set(buildReferences(docs));
		}
	}

	private ServerSentEvent<String> toTokenEvent(ChatClientResponse response) {
		if (response.chatResponse() == null
			|| response.chatResponse().getResult() == null
			|| response.chatResponse().getResult().getOutput() == null) {
			return null;
		}
		String text = response.chatResponse().getResult().getOutput().getText();
		if (text == null || text.isEmpty()) {
			return null;
		}
		return sse("token", text);
	}

	private Flux<ServerSentEvent<String>> buildEndEvents(List<ChatReference> references, String filterDescription) {
		List<ServerSentEvent<String>> events = new ArrayList<>();
		if (!references.isEmpty()) {
			try {
				events.add(sse("references", objectMapper.writeValueAsString(references)));
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize references", e);
			}
		}
		events.add(sse("done", filterDescription));
		return Flux.fromIterable(events);
	}

	// ==================== 유틸 ====================

	private String buildFilterExpression(List<Long> fileIds) {
		return "fileId in ["
			+ fileIds.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", "))
			+ "]";
	}

	private String buildSystemMessage(String userSystemMessage) {
		if (userSystemMessage == null || userSystemMessage.isBlank()) {
			return serverSystemMessage;
		}
		return serverSystemMessage + "\n\n" + userSystemMessage;
	}

	private List<ChatReference> buildReferences(List<Document> documents) {
		Set<String> seen = new LinkedHashSet<>();
		List<ChatReference> references = new ArrayList<>();

		for (Document doc : documents) {
			Map<String, Object> meta = doc.getMetadata();
			String originalFileName = String.valueOf(meta.getOrDefault("originalFileName", ""));
			String fileVersion = String.valueOf(meta.getOrDefault("fileVersion", ""));

			if (seen.add(originalFileName + "|" + fileVersion)) {
				String text = doc.getText() != null ? doc.getText() : "";
				String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;

				references.add(new ChatReference(
					originalFileName,
					fileVersion,
					String.valueOf(meta.getOrDefault("fileCategory", "")),
					preview
				));
			}
		}

		return references;
	}

	private static ServerSentEvent<String> sse(String event, String data) {
		return ServerSentEvent.<String>builder().event(event).data(data).build();
	}

	private static Flux<ServerSentEvent<String>> errorEvents(String message) {
		return Flux.just(sse("error", message), sse("done", ""));
	}
}

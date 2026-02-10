package com.example.juhyeongragchatting.rag.service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.example.juhyeongragchatting.file.model.FileMetadata;
import com.example.juhyeongragchatting.rag.factory.DocumentReaderFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

	private final DocumentReaderFactory documentReaderFactory;
	private final DocumentTransformer documentTransformer;
	private final VectorStore vectorStore;
	private final ObjectMapper objectMapper;

	public List<String> ingest(Path filePath, FileMetadata metadata) {
		List<Document> documents = extractDocuments(filePath, metadata);
		enrichMetadata(documents, metadata);
		List<Document> chunks = splitIntoChunks(documents);
		return storeChunks(chunks, metadata);
	}

	public void delete(List<String> chunkIds) {
		if (chunkIds == null || chunkIds.isEmpty()) {
			return;
		}
		vectorStore.delete(chunkIds);
		log.info("VectorStore에서 {} chunks 삭제 완료", chunkIds.size());
	}

	private List<Document> extractDocuments(Path filePath, FileMetadata metadata) {
		log.info("[1/4] 텍스트 추출 시작 - file: '{}'", metadata.getOriginalFileName());
		DocumentReader reader = documentReaderFactory.createReader(new FileSystemResource(filePath));
		List<Document> documents = reader.read();
		log.info(
			"[1/4] 텍스트 추출 완료 - {} document(s), 총 {}자",
			documents.size(),
			documents.stream().mapToInt(d -> d.getText().length()).sum()
		);
		return documents;
	}

	private void enrichMetadata(List<Document> documents, FileMetadata metadata) {
		log.info("[2/4] 메타데이터 주입 - fileId={}, fileName='{}', version='{}', category='{}'",
			metadata.getId(), metadata.getFileName(), metadata.getFileVersion(), metadata.getFileCategory()
		);
		for (Document doc : documents) {
			doc.getMetadata().put("fileId", String.valueOf(metadata.getId()));
			doc.getMetadata().put("fileName", metadata.getFileName());
			doc.getMetadata().put("fileVersion", metadata.getFileVersion());
			doc.getMetadata().put("fileCategory", metadata.getFileCategory());
			doc.getMetadata().put("originalFileName", metadata.getOriginalFileName());
		}
	}

	private List<Document> splitIntoChunks(List<Document> documents) {
		log.info("[3/4] 토큰 기반 청킹 시작");
		List<Document> chunks = documentTransformer.apply(documents);
		log.info("[3/4] 청킹 완료 - {} chunks 생성", chunks.size());
		try {
			List<Map<String, Object>> summary = chunks.stream().map(chunk -> {
				String text = chunk.getText();
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("id", chunk.getId());
				entry.put("text", text.length() > 100 ? text.substring(0, 100) + "..." : text);
				entry.put("metadata", chunk.getMetadata());
				return entry;
			}).toList();
			log.info("[3/4] chunks 상세:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
		} catch (JsonProcessingException e) {
			log.warn("chunks JSON 직렬화 실패", e);
		}
		return chunks;
	}

	private List<String> storeChunks(List<Document> chunks, FileMetadata metadata) {
		log.info("[4/4] 임베딩 생성 및 VectorStore 저장 시작 - {} chunks", chunks.size());
		vectorStore.add(chunks);
		log.info("[4/4] VectorStore 저장 완료");

		List<String> chunkIds = chunks.stream().map(Document::getId).toList();
		log.info("File '{}' 수집 완료 - 총 {} chunks, ids={}", metadata.getOriginalFileName(), chunkIds.size(), chunkIds);
		return chunkIds;
	}
}

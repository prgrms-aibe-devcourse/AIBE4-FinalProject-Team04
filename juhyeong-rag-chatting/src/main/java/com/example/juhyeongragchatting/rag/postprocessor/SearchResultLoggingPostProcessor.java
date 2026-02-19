package com.example.juhyeongragchatting.rag.postprocessor;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchResultLoggingPostProcessor implements DocumentPostProcessor {

	@Override
	public List<Document> process(Query query, List<Document> documents) {
		log.info("===============================================");
		log.info("[ Vector Store Search Results ]");
		log.info("Query: {}", query.text());
		log.info("===============================================");

		if (documents == null || documents.isEmpty()) {
			log.info("  No search results found.");
			log.info("===============================================");
			return documents;
		}

		for (int i = 0; i < documents.size(); i++) {
			Document doc = documents.get(i);
			Map<String, Object> meta = doc.getMetadata();

			String fileName = String.valueOf(meta.getOrDefault("originalFileName", ""));
			String version = String.valueOf(meta.getOrDefault("fileVersion", ""));
			String category = String.valueOf(meta.getOrDefault("fileCategory", ""));

			log.info(">>{} Document | Score: {} | File: {} | Version: {} | Category: {}",
				i + 1, String.format("%.4f", doc.getScore()), fileName, version, category
			);
			log.info("-----------------------------------------------");

			String text = doc.getText();
			if (text != null) {
				String preview = text.length() > 300 ? text.substring(0, 300) + "..." : text;
				for (String line : preview.split("\n")) {
					log.info("  {}", line);
				}
			}

			log.info("===============================================");
		}

		return documents;
	}
}

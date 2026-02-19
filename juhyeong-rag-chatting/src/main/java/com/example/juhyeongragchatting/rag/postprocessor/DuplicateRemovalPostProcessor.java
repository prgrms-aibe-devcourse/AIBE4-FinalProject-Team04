package com.example.juhyeongragchatting.rag.postprocessor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DuplicateRemovalPostProcessor implements DocumentPostProcessor {

	@Override
	public List<Document> process(Query query, List<Document> documents) {
		Set<String> seenIds = new LinkedHashSet<>();
		List<Document> deduplicated = new ArrayList<>();

		for (Document doc : documents) {
			if (seenIds.add(doc.getId())) {
				deduplicated.add(doc);
			}
		}

		int removed = documents.size() - deduplicated.size();
		if (removed > 0) {
			log.info("[DocumentPostProcessor] {}개 중복 청크 제거 ({} -> {})",
				removed, documents.size(), deduplicated.size()
			);
		}

		return deduplicated;
	}
}

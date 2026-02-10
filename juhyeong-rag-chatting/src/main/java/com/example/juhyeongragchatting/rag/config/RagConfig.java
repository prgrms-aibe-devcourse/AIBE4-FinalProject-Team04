package com.example.juhyeongragchatting.rag.config;

import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.juhyeongragchatting.rag.postprocessor.DuplicateRemovalPostProcessor;
import com.example.juhyeongragchatting.rag.postprocessor.SearchResultLoggingPostProcessor;

@Configuration
public class RagConfig {

	// ==================== ETL Pipeline ====================

	// ETL-1. DocumentTransformer
	@Bean
	public DocumentTransformer documentTransformer() {
		return new TokenTextSplitter();
	}

	// ETL-2. DocumentWriter
	// VectorStore is provided by Spring AI pgvector auto-configuration

	// ==================== RAG Pipeline ====================

	// RAG-1. DocumentRetriever
	@Bean
	public VectorStoreDocumentRetriever documentRetriever(VectorStore vectorStore) {
		return VectorStoreDocumentRetriever.builder()
			.vectorStore(vectorStore)
			.similarityThreshold(0.3)
			.topK(3)
			.build();
	}

	// RAG-2. DocumentPostProcessor
	@Bean
	public DuplicateRemovalPostProcessor duplicateRemovalPostProcessor() {
		return new DuplicateRemovalPostProcessor();
	}

	@Bean
	public SearchResultLoggingPostProcessor searchResultLoggingPostProcessor() {
		return new SearchResultLoggingPostProcessor();
	}

	// RAG-3. QueryAugmenter
	@Bean
	public ContextualQueryAugmenter queryAugmenter() {
		return ContextualQueryAugmenter.builder()
			.allowEmptyContext(false)
			.build();
	}

	// RAG-4. RetrievalAugmentationAdvisor
	@Bean
	public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
		VectorStoreDocumentRetriever documentRetriever,
		DuplicateRemovalPostProcessor duplicateRemovalPostProcessor,
		SearchResultLoggingPostProcessor searchResultLoggingPostProcessor,
		ContextualQueryAugmenter queryAugmenter
	) {
		return RetrievalAugmentationAdvisor.builder()
			.documentRetriever(documentRetriever)
			.documentPostProcessors(duplicateRemovalPostProcessor, searchResultLoggingPostProcessor)
			.queryAugmenter(queryAugmenter)
			.build();
	}
}

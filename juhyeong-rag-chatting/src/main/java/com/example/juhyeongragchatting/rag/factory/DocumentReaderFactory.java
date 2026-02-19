package com.example.juhyeongragchatting.rag.factory;

import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DocumentReaderFactory {

	public DocumentReader createReader(Resource resource) {
		return new TikaDocumentReader(resource);
	}
}

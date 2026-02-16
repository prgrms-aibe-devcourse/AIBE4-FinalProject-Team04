package kr.java.patchnotedemo.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MetadataKeys {
    // 공통 필드
    public static final String PROJECT_ID = "project_id";
    public static final String SOURCE_ID = "source_id";
    public static final String SOURCE_TYPE = "source_type";
    public static final String SECTION_TITLE = "section_title";
    public static final String CHUNK_INDEX = "chunk_index";
    public static final String TOTAL_CHUNK = "total_chunks";
    public static final String CATEGORY = "category";
    public static final String AUTHOR_ID = "author_id";
    public static final String TOKEN_COUNT = "token_count";
    public static final String TIMESTAMP = "timestamp";

    // Issue 필드
    public static final String ISSUE_PRIORITY = "issue_priority";

    // Document 필드
    public static final String DOC_PAGE_NUMBER = "doc_page_number";
    public static final String DOC_VERSION = "doc_version";
    public static final String DOC_FILE_URL = "doc_file_url";
}

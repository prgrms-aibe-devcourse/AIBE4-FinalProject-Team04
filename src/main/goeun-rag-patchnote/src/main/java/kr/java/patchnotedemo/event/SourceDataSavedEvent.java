package kr.java.patchnotedemo.event;

import kr.java.patchnotedemo.enums.SourceType;

public record SourceDataSavedEvent(
        Long sourceId, // 원본 데이터 ID
        SourceType sourceType, // DOCUMENT or ISSUE
        String content, // 분석할 텍스트 내용
        String projectId // 프로젝트 ID
        ) {}

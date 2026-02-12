package kr.java.patchnotedemo.dto;

import kr.java.patchnotedemo.entity.PendingItem;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import kr.java.patchnotedemo.enums.SourceType;

public record PendingItemResponse(
        Long id,
        Long sourceId,
        SourceType sourceType,
        String title,
        String summary,
        PendingItemStatus status,
        String projectId) {

    public static PendingItemResponse from(PendingItem entity) {
        return new PendingItemResponse(
                entity.getId(),
                entity.getSourceId(),
                entity.getSourceType(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getStatus(),
                entity.getProjectId());
    }
}

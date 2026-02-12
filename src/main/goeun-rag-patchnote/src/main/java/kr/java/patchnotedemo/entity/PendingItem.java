package kr.java.patchnotedemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.java.patchnotedemo.enums.PatchType;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import kr.java.patchnotedemo.enums.SourceType;
import kr.java.patchnotedemo.enums.PatchType;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import kr.java.patchnotedemo.enums.SourceType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pending_items")
public class PendingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private Long sourceId;

    private String title; // LLM이 요약한 제목

    @Column(columnDefinition = "TEXT")
    private String summary; // LLM이 요약한 내용

    @Enumerated(EnumType.STRING)
    private PatchType patchType;

    @Enumerated(EnumType.STRING)
    private PendingItemStatus status;

    private String choseong;

    @Builder
    public PendingItem(
            String projectId,
            SourceType sourceType,
            Long sourceId,
            String title,
            String summary,
            PatchType patchType,
            String originalContent,
            String choseong) {
        this.projectId = projectId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.summary = summary;
        this.patchType = patchType;
        this.choseong = choseong;
        this.status = PendingItemStatus.PENDING;
    }

    public void exclude() {
        this.status = PendingItemStatus.EXCLUDED;
    }

    public void restore() {
        this.status = PendingItemStatus.PENDING;
    }
}

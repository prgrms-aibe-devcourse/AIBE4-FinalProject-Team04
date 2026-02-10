package kr.java.patchnote_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.java.patchnote_demo.enums.IssueType;
import kr.java.patchnote_demo.enums.PendingItemStatus;
import kr.java.patchnote_demo.enums.SourceType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PendingItem")
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
    private IssueType category;

    @Enumerated(EnumType.STRING)
    private PendingItemStatus status;

    private String choseong;

    @Column(columnDefinition = "TEXT")
    private String originalContent; // 원본 텍스트 (RAG 프롬프트에 넣을 용도)

    @Builder
    public PendingItem(
            String projectId,
            SourceType sourceType,
            String title,
            String summary,
            IssueType category,
            String choseong) {
        this.projectId = projectId;
        this.sourceType = sourceType;
        this.title = title;
        this.summary = summary;
        this.category = category;
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

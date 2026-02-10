package kr.java.patchnotedemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Document")
public class Document extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String title;

    private String version;

    private String author;

    private String category; // 기획서, 설계서

    @Column(columnDefinition = "TEXT")
    private String url; // 원본 파일 주소

    private String fileType; // PDF, DOCX, PPT, EXCEL

    private Long fileSize; // Byte

    private String choseong;

    private boolean isProcessed; // 벡터 DB 적재 여부

    @Builder
    public Document(
            String projectId,
            String title,
            String version,
            String author,
            String category,
            String url,
            String fileType,
            Long fileSize,
            String choseong,
            boolean isProcessed) {
        this.projectId = projectId;
        this.title = title;
        this.version = version;
        this.author = author;
        this.category = category;
        this.url = url;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.choseong = choseong;
        this.isProcessed = isProcessed;
    }
}

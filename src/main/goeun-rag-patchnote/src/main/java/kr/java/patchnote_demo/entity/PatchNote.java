package kr.java.patchnote_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import kr.java.patchnote_demo.enums.PatchNoteStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PatchNote")
public class PatchNote extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    private String version;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate startDate; // 업데이트 반영 시작일
    private LocalDate endDate; // 업데이트 반영 종료일

    private PatchNoteStatus status;

    @Column(columnDefinition = "TEXT")
    private String promptUsed; // 어떤 프롬프트로 만들었는지 (디버깅용)

    @Builder
    public PatchNote(
            String projectId,
            String version,
            String title,
            String content,
            LocalDate startDate,
            LocalDate endDate) {
        this.projectId = projectId;
        this.version = version;
        this.title = title;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = PatchNoteStatus.DRAFT;
    }
}

package kr.java.patchnotedemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.java.patchnotedemo.enums.IssueStatus;
import kr.java.patchnotedemo.enums.IssuePriority;
import kr.java.patchnotedemo.enums.IssueStatus;
import kr.java.patchnotedemo.enums.IssueType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    @Enumerated(EnumType.STRING)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    private String assignee;

    @Enumerated(EnumType.STRING)
    private IssuePriority priority;

    private LocalDateTime resolvedAt; // 해결 시점

    @Builder
    public Issue(
            String projectId,
            String title,
            String description,
            IssueStatus status,
            String resolutionNote,
            IssuePriority priority,
            String assignee,
            IssueType issueType) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.resolutionNote = resolutionNote;
        this.priority = priority;
        this.assignee = assignee;
        this.issueType = issueType;
    }

    public void assign(String assignee) {
        this.assignee = assignee;
    }

    public void unassign() {
        this.assignee = null;
    }

    public void resolve(String note) {
        this.status = IssueStatus.RESOLVED;
        this.resolutionNote = note;
        this.resolvedAt = LocalDateTime.now();
    }
}

package kr.java.patchnotedemo.repository;

import java.util.List;
import kr.java.patchnotedemo.entity.Issue;
import kr.java.patchnotedemo.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByProjectId(String projectId);

    List<Issue> findByStatus(IssueStatus status);
}

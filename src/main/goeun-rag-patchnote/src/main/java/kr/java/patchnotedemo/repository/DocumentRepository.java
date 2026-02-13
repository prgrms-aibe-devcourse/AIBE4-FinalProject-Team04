package kr.java.patchnotedemo.repository;

import java.util.List;
import kr.java.patchnotedemo.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByProjectId(String projectId);

    List<Document> findByIsProcessedFalse();
}

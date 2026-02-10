package kr.java.patchnote_demo.repository;

import java.util.List;
import kr.java.patchnote_demo.entity.PendingItem;
import kr.java.patchnote_demo.enums.PendingItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingItemRepository extends JpaRepository<PendingItem, Long> {

    List<PendingItem> findByProjectIdAndStatus(String projectId, PendingItemStatus status);

    // 초성 검색 (pg_trgm 활용)
    @Query(
            value =
                    "SELECT * FROM PendingItem "
                            + "WHERE project_id = :projectId "
                            + "AND status = 'PENDING' "
                            + "AND choseong LIKE :keyword",
            nativeQuery = true)
    List<PendingItem> findByChoseong(
            @Param("projectId") String projectId, @Param("keyword") String keyword);

    // 와일드카드 + 유사도 정렬 검색
    // 제목이나 요약에 키워드가 포함된 것을 찾되, 정확도가 높은 순으로 정렬
    @Query(
            value =
                    "SELECT * FROM PendingItem "
                            + "WHERE project_id = :projectId "
                            + "AND status = 'PENDING' "
                            + "AND (title LIKE %:keyword% OR summary LIKE %:keyword%) "
                            + "ORDER BY CASE WHEN title = :keyword THEN 0 "
                            + "              WHEN title LIKE :keyword% THEN 1 "
                            + "              ELSE 2 END",
            nativeQuery = true)
    List<PendingItem> searchByKeyword(
            @Param("projectId") String projectId, @Param("keyword") String keyword);
}

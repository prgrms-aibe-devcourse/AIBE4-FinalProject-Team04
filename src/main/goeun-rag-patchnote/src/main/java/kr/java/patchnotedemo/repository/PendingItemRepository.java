package kr.java.patchnotedemo.repository;

import java.util.List;
import java.util.Optional;
import kr.java.patchnotedemo.entity.PendingItem;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingItemRepository extends JpaRepository<PendingItem, Long> {

    List<PendingItem> findByProjectIdAndStatus(String projectId, PendingItemStatus status);

    List<PendingItem> findByProjectIdAndStatusNot(String projectId, PendingItemStatus status);

    Optional<PendingItem> findByIdAndProjectId(Long id, String projectId);

    // 초성 검색 (pg_trgm 활용)
    @Query(
            value =
                    "SELECT * FROM \"pending_items\" "
                            + "WHERE project_id = :projectId "
                            + "AND status = 'PENDING' "
                            + "AND choseong LIKE CONCAT(:keyword, '%')",
            nativeQuery = true)
    List<PendingItem> findByChoseong(
            @Param("projectId") String projectId, @Param("keyword") String keyword);

    // 와일드카드 + 유사도 정렬 검색
    // 제목이나 요약에 키워드가 포함된 것을 찾되, 정확도가 높은 순으로 정렬
    @Query(
            value =
                    "SELECT * FROM \"pending_items\" "
                            + "WHERE project_id = :projectId "
                            + "AND status = 'PENDING' "
                            + "AND (title LIKE CONCAT('%', :keyword, '%') OR summary LIKE CONCAT('%', :keyword, '%')) "
                            + "ORDER BY CASE WHEN title = :keyword THEN 0 "
                            + "              WHEN title LIKE CONCAT(:keyword, '%') THEN 1 "
                            + "              ELSE 2 END",
            nativeQuery = true)
    List<PendingItem> searchByKeyword(
            @Param("projectId") String projectId, @Param("keyword") String keyword);
}

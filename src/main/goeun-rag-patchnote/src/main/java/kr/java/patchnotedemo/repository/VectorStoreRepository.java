package kr.java.patchnotedemo.repository;

import java.util.List;
import java.util.UUID;
import kr.java.patchnotedemo.entity.ChunkData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VectorStoreRepository extends JpaRepository<ChunkData, UUID> {
    @Query(
            value =
                    """
        SELECT v.content
        FROM vector_store v
        WHERE v.metadata ->> 'project_id' = :projectId
        AND v.metadata ->> 'source_id' IN (:sourceIds)
        """,
            nativeQuery = true)
    List<String> findContentByMetadata(
            @Param("projectId") String projectId, @Param("sourceIds") List<String> sourceIds);
}

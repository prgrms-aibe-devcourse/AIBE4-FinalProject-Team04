package com.example.juhyeongragchatting.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.juhyeongragchatting.file.model.FileGroup;
import com.example.juhyeongragchatting.file.model.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

	Optional<FileMetadata> findTopByFileGroupOrderByMajorVersionDescMinorVersionDescPatchVersionDesc(FileGroup fileGroup);

	boolean existsByOriginalFileName(String originalFileName);

	List<FileMetadata> findByFileGroup_Category(String category);

	List<FileMetadata> findByFileGroup(FileGroup fileGroup);

	List<FileMetadata> findByFileGroupId(Long groupId);

	Optional<FileMetadata> findFirstByFileHash(String fileHash);

	@Query("""
		select f
		from FileMetadata f
		where f.fileGroup.id = :groupId
		order by f.majorVersion asc, f.minorVersion asc, f.patchVersion asc, f.id asc
		""")
	List<FileMetadata> findByFileGroupIdOrderByVersion(@Param("groupId") Long groupId);

	@Query("""
		select f
		from FileMetadata f
		where f.fileGroup.id = :groupId
		order by f.majorVersion desc, f.minorVersion desc, f.patchVersion desc, f.id desc
		""")
	List<FileMetadata> findByFileGroupIdOrderByVersionDesc(@Param("groupId") Long groupId);

	boolean existsByFileGroupIdAndMajorVersionAndMinorVersionAndPatchVersion(
		Long groupId, int majorVersion, int minorVersion, int patchVersion
	);

	@Query("""
		select count(f) > 0
		from FileMetadata f
		where f.fileGroup.id = :groupId
		and f.majorVersion = :major
		and f.minorVersion = :minor
		and f.patchVersion = :patch
		and f.id <> :fileId
		""")
	boolean existsVersionInGroupExcludingId(
		@Param("groupId") Long groupId,
		@Param("major") int major,
		@Param("minor") int minor,
		@Param("patch") int patch,
		@Param("fileId") Long fileId
	);

	@Query("""
		select count(f) > 0
		from FileMetadata f
		where f.originalFileName = :originalFileName
		and f.fileGroup.id <> :groupId
		""")
	boolean existsByOriginalFileNameAndFileGroupIdNot(
		@Param("originalFileName") String originalFileName,
		@Param("groupId") Long groupId
	);

	@Query(value = """
		SELECT * FROM (
			SELECT DISTINCT ON (group_id)
				*
			FROM file_metadata
			ORDER BY group_id,
				major_version DESC, minor_version DESC, patch_version DESC, id DESC
		) latest
		ORDER BY major_version DESC, minor_version DESC, patch_version DESC, id DESC
		""",
		countQuery = """
		SELECT COUNT(DISTINCT group_id)
		FROM file_metadata
		""",
		nativeQuery = true)
	Page<FileMetadata> findLatestPerGroup(Pageable pageable);

	@Query("select count(f) from FileMetadata f where f.fileGroup.id = :groupId")
	int countByFileGroupId(@Param("groupId") Long groupId);
}

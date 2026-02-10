package com.example.juhyeongragchatting.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.juhyeongragchatting.file.model.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

	Optional<FileMetadata> findFirstByOriginalFileNameAndFileHash(
		String originalFileName, String fileHash
	);

	Optional<FileMetadata> findTopByOriginalFileNameOrderByIdDesc(String originalFileName);

	List<FileMetadata> findByFileCategory(String fileCategory);

	List<FileMetadata> findByOriginalFileName(String originalFileName);

	@Query("""
		select distinct f.fileCategory
		from FileMetadata f
		where f.fileCategory is not null
		order by f.fileCategory
		""")
	List<String> findDistinctCategories();

	@Query("""
		select distinct f.originalFileName
		from FileMetadata f
		order by f.originalFileName
		""")
	List<String> findDistinctOriginalFileNames();

	@Query("""
		select f.fileVersion
		from FileMetadata f
		where f.originalFileName = :originalFileName
		order by f.id asc
		""")
	List<String> findVersionsByOriginalFileName(@Param("originalFileName") String originalFileName);
}

package com.example.juhyeongragchatting.file.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Getter
@Entity
@Table(name = "file_metadata")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String fileVersion;

	private String fileCategory;

	@Column(nullable = false)
	private String storedFilePath;

	@Column(nullable = false)
	private String originalFileName;

	@Column(nullable = false)
	private LocalDateTime uploadedAt;

	@Column(nullable = false, length = 64)
	private String fileHash;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "file_chunk_ids", joinColumns = @JoinColumn(name = "file_id"))
	private List<String> chunkIds;
}

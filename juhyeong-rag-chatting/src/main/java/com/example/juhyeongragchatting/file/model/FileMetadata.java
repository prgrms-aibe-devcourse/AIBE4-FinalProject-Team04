package com.example.juhyeongragchatting.file.model;

import java.time.LocalDateTime;
import java.util.Comparator;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
	private int majorVersion;

	@Column(nullable = false)
	private int minorVersion;

	@Column(nullable = false)
	private int patchVersion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_id", nullable = false)
	private FileGroup fileGroup;

	@Column(nullable = false)
	private String originalFileName;

	@Column(nullable = false)
	private String storedFilePath;

	@Column(nullable = false, length = 64)
	private String fileHash;

	@Column(nullable = false)
	private LocalDateTime uploadedAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "file_chunk_id", joinColumns = @JoinColumn(name = "file_id"))
	private List<String> chunkIds;

	public String getVersionString() {
		return majorVersion + "." + minorVersion + "." + patchVersion;
	}

	public String getFileCategory() {
		return fileGroup.getCategory();
	}

	public String getGroupName() {
		return fileGroup.getGroupName();
	}

	public Long getGroupId() {
		return fileGroup.getId();
	}

	public static final Comparator<FileMetadata> VERSION_COMPARATOR = Comparator
		.comparingInt(FileMetadata::getMajorVersion)
		.thenComparingInt(FileMetadata::getMinorVersion)
		.thenComparingInt(FileMetadata::getPatchVersion)
		.thenComparing(FileMetadata::getId);
}

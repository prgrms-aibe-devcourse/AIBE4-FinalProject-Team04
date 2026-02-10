package com.example.juhyeongragchatting.file.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.juhyeongragchatting.chat.dto.SearchFilterResult;
import com.example.juhyeongragchatting.chat.dto.SearchScope;
import com.example.juhyeongragchatting.chat.dto.VersionPolicy;
import com.example.juhyeongragchatting.file.model.FileMetadata;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileSearchService {

	private final FileStorageService fileStorageService;

	public SearchFilterResult resolveFileIds(
		SearchScope scope, VersionPolicy versionPolicy, String filterValue, List<String> versions
	) {
		Stream<FileMetadata> candidates = switch (scope) {
			case ALL -> fileStorageService.getAllMetadata().stream();
			case CATEGORY -> fileStorageService.getByCategory(filterValue).stream();
			case FILE -> fileStorageService.getByOriginalFileName(filterValue).stream();
		};
		List<FileMetadata> filtered = candidates.toList();

		List<Long> fileIds = switch (versionPolicy) {
			case ALL_VERSIONS -> filtered.stream().map(FileMetadata::getId).toList();
			case LATEST -> filtered.stream().collect(
					Collectors.groupingBy(FileMetadata::getOriginalFileName,
						Collectors.maxBy(Comparator.comparingLong(FileMetadata::getId))
					)
				).values().stream()
				.filter(Optional::isPresent)
				.map(opt -> opt.get().getId())
				.toList();
			case SPECIFIC -> filtered.stream()
				.filter(m -> versions != null && versions.contains(m.getFileVersion()))
				.map(FileMetadata::getId).toList();
		};

		String description = buildDescription(scope, versionPolicy, filterValue, versions);
		return new SearchFilterResult(fileIds, description);
	}

	public List<String> getDistinctCategories() {
		return fileStorageService.getDistinctCategories();
	}

	public List<String> getDistinctOriginalFileNames() {
		return fileStorageService.getDistinctOriginalFileNames();
	}

	public List<String> getVersionsByOriginalFileName(String originalFileName) {
		return fileStorageService.getVersionsByOriginalFileName(originalFileName);
	}

	private String buildDescription(
		SearchScope scope, VersionPolicy versionPolicy, String filterValue, List<String> versions
	) {
		String scopeDesc = switch (scope) {
			case ALL -> "전체 문서";
			case CATEGORY -> "카테고리: " + filterValue;
			case FILE -> "파일: " + filterValue;
		};
		String versionDesc = switch (versionPolicy) {
			case LATEST -> "최신 버전";
			case ALL_VERSIONS -> "모든 버전";
			case SPECIFIC -> "버전: " + String.join(", ", versions);
		};
		return scopeDesc + " (" + versionDesc + ")";
	}
}

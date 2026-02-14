package kr.java.patchnotedemo.dto;

import java.util.List;

public record GenerateDraftRequest(
        String projectId,
        List<Long> pendingItemIds,
        String userPrompt,
        String template) {}

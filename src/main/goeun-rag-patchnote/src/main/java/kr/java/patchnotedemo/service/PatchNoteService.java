package kr.java.patchnotedemo.service;

import java.util.List;
import java.util.stream.Collectors;
import kr.java.patchnotedemo.dto.GenerateDraftRequest;
import kr.java.patchnotedemo.dto.PendingItemResponse;
import kr.java.patchnotedemo.entity.PendingItem;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import kr.java.patchnotedemo.repository.PendingItemRepository;
import kr.java.patchnotedemo.repository.VectorStoreRepository;
import kr.java.patchnotedemo.util.PromptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatchNoteService {

    private final PendingItemRepository pendingItemRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final ChatClient chatClient;

    @Value("classpath:prompts/create-patchnote.st")
    private Resource createPatchNotePromptResource;

    @Transactional(readOnly = true)
    public List<PendingItemResponse> getExcludedItems(String projectId) {
        return pendingItemRepository
                .findByProjectIdAndStatus(projectId, PendingItemStatus.EXCLUDED)
                .stream()
                .map(PendingItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingItemResponse> getPendingItems(String projectId) {
        return pendingItemRepository
                .findByProjectIdAndStatus(projectId, PendingItemStatus.PENDING)
                .stream()
                .map(PendingItemResponse::from)
                .toList();
    }

    @Transactional
    public void excludeItem(Long id, String projectId) {
        PendingItem item =
                pendingItemRepository
                        .findByIdAndProjectId(id, projectId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Item not found or access denied: " + id));
        item.exclude();
    }

    @Transactional
    public void restoreItem(Long id, String projectId) {
        PendingItem item =
                pendingItemRepository
                        .findByIdAndProjectId(id, projectId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Item not found or access denied: " + id));
        item.restore();
    }

    public String generatePatchNoteDraft(GenerateDraftRequest request) {

        String context = getContext(request);

        String template = PromptUtils.loadTemplate(createPatchNotePromptResource);
        String promptText =
                template.replace("{context}", context)
                        .replace("{template}", request.template())
                        .replace("{userPrompt}", request.userPrompt());

        return chatClient
                .prompt()
                .user(promptText)
                .options(ChatOptions.builder().temperature(0.0).build())
                .call()
                .content();
    }

    @Transactional(readOnly = true)
    protected String getContext(GenerateDraftRequest request) {
        List<PendingItem> selectedItems =
                pendingItemRepository.findAllById(request.pendingItemIds());

        List<String> sourceIds =
                selectedItems.stream()
                        .map(item -> String.valueOf(item.getSourceId()))
                        .collect(Collectors.toList());

        if (sourceIds.isEmpty()) {
            return "선택된 항목이 없습니다.";
        }

        List<String> contents =
                vectorStoreRepository.findContentByMetadata(request.projectId(), sourceIds);

        return String.join("\n\n---\n\n", contents);
    }
}

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
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatchNoteService {

    private static final String DEFAULT_TEMPLATE = """
        ## 🚀 [업데이트] 주요 변경 사항 안내

        안녕하세요, 플레이어 여러분!
        쾌적하고 즐거운 게임 환경을 위해 진행된 업데이트 상세 내용을 전해드립니다.

        ### ✨ 주요 업데이트
        (이번 패치의 핵심 내용을 3줄 요약으로 작성해 주세요.)

        ---

        ### 🛠️ 상세 패치 노트

        #### 밸런스 및 전투
        - (캐릭터, 스킬, 아이템 관련 변경 사항)

        #### 시스템 및 편의성
        - (UI/UX, 최적화, 편의 기능 관련 사항)

        #### 오류 수정
        - (수정된 버그 내역)

        ---

        항상 저희 게임을 사랑해 주셔서 감사합니다.
        더 나은 서비스를 제공하기 위해 최선을 다하겠습니다.
        """;
    private static final String DEFAULT_USER_PROMPT =
        "플레이어에게 친근하고 예의 바른 GM(운영자) 말투로 작성해줘." +
            "내용의 이해를 돕기 위해 적절한 이모지를 사용해줘(남발 금지)" +
            "딱딱한 용어보다는 플레이어가 이해하기 쉬운 표현을 사용해줘." +
            "문장은 '했습니다' 보다는 '했어요', '되었습니다' 보다는 '되었어요' 같은 부드러운 해요체를 사용해줘.";

    private final PendingItemRepository pendingItemRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final ChatClient chatClient;
    @Value("classpath:prompts/create-patchnote.st")
    private Resource createPatchNotePromptResource;

    public List<PendingItemResponse> getExcludedItems(String projectId) {
        return pendingItemRepository
                .findByProjectIdAndStatus(projectId, PendingItemStatus.EXCLUDED)
                .stream()
                .map(PendingItemResponse::from)
                .toList();
    }

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
        validateDraftRequest(request);

        String context = getContext(request);

        String template = PromptUtils.loadPrompt(createPatchNotePromptResource);

        String customTemplate = request.template() != null && !request.template().isBlank()
            ? request.template()
            : DEFAULT_TEMPLATE;

        String userPrompt = request.userPrompt() != null && !request.userPrompt().isBlank()
            ? request.userPrompt()
            : DEFAULT_USER_PROMPT;

        String promptText =
                template.replace("{context}", context)
                        .replace("{template}", customTemplate)
                        .replace("{userPrompt}", userPrompt);

        return chatClient
                .prompt()
                .user(promptText)
                .options(ChatOptions.builder().temperature(0.0).build())
                .call()
                .content();
    }

    private void validateDraftRequest(GenerateDraftRequest request) {
        Assert.hasText(request.projectId(), "ProjectId must not be empty");
        if (request.pendingItemIds() == null || request.pendingItemIds().isEmpty()) {
            throw new IllegalArgumentException("PendingItemIds must not be empty");
        }
    }

    @Transactional(readOnly = true)
    protected String getContext(GenerateDraftRequest request) {
        List<PendingItem> selectedItems =
                pendingItemRepository.findByIdInAndProjectId(request.pendingItemIds(),request.projectId());

        if (selectedItems.isEmpty()) {
            return "선택된 항목이 없습니다.";
        }

        List<String> sourceIds =
                selectedItems.stream()
                        .map(item -> String.valueOf(item.getSourceId()))
                        .collect(Collectors.toList());

        List<String> contents =
                vectorStoreRepository.findContentByMetadata(request.projectId(), sourceIds);

        return String.join("\n\n---\n\n", contents);
    }
}

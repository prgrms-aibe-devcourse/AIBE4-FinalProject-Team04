package kr.java.patchnotedemo.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.patchnotedemo.dto.DocumentSummaryResponse;
import kr.java.patchnotedemo.dto.IssueSummaryResponse;
import kr.java.patchnotedemo.entity.Issue;
import kr.java.patchnotedemo.entity.PendingItem;
import kr.java.patchnotedemo.enums.IssueType;
import kr.java.patchnotedemo.enums.PatchType;
import kr.java.patchnotedemo.enums.SourceType;
import kr.java.patchnotedemo.repository.IssueRepository;
import kr.java.patchnotedemo.repository.PendingItemRepository;
import kr.java.patchnotedemo.util.PromptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceDataSavedEventListener {
    private final ChatClient chatClient;
    private final IssueRepository issueRepository;
    private final PendingItemRepository pendingItemRepository;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/extract-document-summary.st")
    private Resource extractDocumentSummaryPromptResource;

    @Value("classpath:prompts/extract-issue-summary.st")
    private Resource extractIssueSummaryPromptResource;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDataSaved(SourceDataSavedEvent event) {
        log.info(
                "SourceDataSavedEvent 발생 sourceID: {}, content: {}",
                event.sourceId(),
                event.content().substring(0, Math.min(event.content().length(), 100)) + "...");
        try {
            if (event.sourceType() == SourceType.ISSUE) {
                processIssueEvent(event);
            } else {
                processDocumentEvent(event);
            }
        } catch (Exception e) {
            log.error(
                    "Error processing SourceDataSavedEvent for sourceId: {}", event.sourceId(), e);
        }
    }

    private void processIssueEvent(SourceDataSavedEvent event) throws JsonProcessingException {
        Issue issue =
                issueRepository
                        .findById(event.sourceId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Issue not found with id: " + event.sourceId()));

        PatchType patchType = mapIssueToPatchCategory(issue.getIssueType());

        IssueSummaryResponse dto =
                extractMetadata(
                        extractIssueSummaryPromptResource,
                        event.content(),
                        IssueSummaryResponse.class);

        savePendingItem(event, dto.title(), dto.summary(), patchType);
    }

    private void processDocumentEvent(SourceDataSavedEvent event) throws JsonProcessingException {
        DocumentSummaryResponse dto =
                extractMetadata(
                        extractDocumentSummaryPromptResource,
                        event.content(),
                        DocumentSummaryResponse.class);

        savePendingItem(event, dto.title(), dto.summary(), dto.category());
    }

    private <T> T extractMetadata(Resource promptResource, String content, Class<T> responseType)
            throws JsonProcessingException {
        String template = PromptUtils.loadTemplate(promptResource);
        if (template == null || template.isBlank()) {
            throw new IllegalStateException(
                    "Prompt template is empty: " + promptResource.getDescription());
        }

        String promptText = template.replace("{content}", content);
        String jsonResponse = chatClient.prompt().user(promptText).call().content();

        log.debug("LLM Response: {}", jsonResponse);

        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new IllegalStateException("LLM returned empty response");
        }

        String cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim();
        return objectMapper.readValue(cleanJson, responseType);
    }

    private void savePendingItem(
            SourceDataSavedEvent event, String title, String summary, PatchType patchType) {
        PendingItem item =
                PendingItem.builder()
                        .projectId(event.projectId())
                        .sourceType(event.sourceType())
                        .sourceId(event.sourceId())
                        .title(title)
                        .summary(summary)
                        .patchType(patchType)
                        .build();

        pendingItemRepository.save(item);
        log.info("Saved PendingItem for sourceId: {}", event.sourceId());
    }

    private PatchType mapIssueToPatchCategory(IssueType issueType) {
        return switch (issueType) {
            case FEATURE -> PatchType.NEW;
            case IMPROVEMENT, BALANCING -> PatchType.CHANGE;
            case BUG_FIX, SECURITY -> PatchType.FIX;
        };
    }
}

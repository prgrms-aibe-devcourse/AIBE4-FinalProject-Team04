package kr.java.patchnotedemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.java.patchnotedemo.constant.MetadataKeys;
import kr.java.patchnotedemo.dto.IssueDummyResponse;
import kr.java.patchnotedemo.entity.Document;
import kr.java.patchnotedemo.entity.Issue;
import kr.java.patchnotedemo.enums.IssuePriority;
import kr.java.patchnotedemo.enums.IssueStatus;
import kr.java.patchnotedemo.enums.IssueType;
import kr.java.patchnotedemo.enums.SourceType;
import kr.java.patchnotedemo.event.SourceDataSavedEvent;
import kr.java.patchnotedemo.repository.DocumentRepository;
import kr.java.patchnotedemo.repository.IssueRepository;
import kr.java.patchnotedemo.util.PromptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {

    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;
    private final IssueRepository issueRepository;
    private final VectorStore vectorStore;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private final List<String> DOC_TYPES =
            List.of(
                    "게임 기획서(GDD)",
                    "레벨/밸런스 설계서",
                    "시나리오",
                    "API 명세서",
                    "시스템 아키텍처",
                    "DB 스키마",
                    "인프라 구성도",
                    "UI/UX 기획서",
                    "디자인 가이드",
                    "와이어프레임");
    private final List<String> NAMES = List.of("김철수", "이영희", "박민수", "최지우", "정재석", "강하늘");
    private final List<String> DEPARTMENTS =
            List.of("기획팀", "기획팀", "시나리오팀", "서버팀", "서버팀", "서버팀", "인프라팀", "디자인팀", "디자인팀", "디자인팀");
    private final List<String> ROLES = List.of("팀장", "시니어", "주니어", "파트장", "인턴", "시니어");

    @Value("classpath:prompts/dummy-doc.st")
    private Resource dummyDocPromptResource;

    @Value("classpath:prompts/dummy-issue.st")
    private Resource dummyIssuePromptResource;

    public void generateDummy(SourceType type, String projectId) throws JsonProcessingException {
        int batchSize = 10;
        int maxRetries = 3;

        for (int i = 0; i < batchSize; i++) {
            int retryCount = 0;
            boolean success = false;

            while (!success && retryCount < maxRetries) {
                try {
                    log.info("데이터 생성 시도 [{}/{}], 재시도: {}", i + 1, batchSize, retryCount);
                    if (type == SourceType.DOCUMENT) {
                        createDummyDocument(projectId, i);
                    } else {
                        createDummyIssue(projectId, i);
                    }
                    // api 사용 시 무료 티어 제한(RPM)을 피하기 위해 4초 휴식
                    // 20 RPM = 1분에 20개 = 3초에 1개 허용
                    //Thread.sleep(4000);
                    success = true; // 성공 시 루프 탈출
                } catch (NonTransientAiException e) {
                    if (e.getMessage().contains("429")) {
                        log.warn("API 쿼터 초과 (429)! 40초 대기... (시도 {}/{})", retryCount + 1,
                            maxRetries);
                        retryCount++;
                    } else {
                        log.error("생성 실패 (Index: " + i + ")", e);
                        break;
                    }

                } catch (Exception e) {
                    log.error("아이템 생성 최종 실패 (Index: {}). 다음으로 넘어감.", i);
                }
            }
        }
    }

    private void createDummyDocument(String projectId, int index) {
        String docType = DOC_TYPES.get(index % DOC_TYPES.size());
        String name = NAMES.get(index % NAMES.size());
        String dept = DEPARTMENTS.get(index % DEPARTMENTS.size());
        String role = ROLES.get(index % ROLES.size());

        String genre = "MMORPG";
        String gameName = "레전드 오브 스프링";

        String template = PromptUtils.loadPrompt(dummyDocPromptResource);
        String promptText =
                template.replace("{genre}", genre)
                        .replace("{gameName}", gameName)
                        .replace("{author}", name)
                        .replace("{dept}", dept)
                        .replace("{role}", role)
                        .replace("{docType}", docType);

        String generatedContent = chatClient.prompt().user(promptText).call().content();
        if (generatedContent == null) { generatedContent = "";}

        final String contentToSave = generatedContent;
        transactionTemplate.executeWithoutResult(status -> {
            Document doc =
                Document.builder()
                    .projectId(projectId)
                    .title("[" + docType + "] " + gameName + " v1." + index + ".0")
                    .author(name)
                    .version("v1." + index + ".0")
                    .category(docType)
                    .fileUrl("http://dummy-s3/docs/" + UUID.randomUUID())
                    .fileType("PDF")
                    .isProcessed(true)
                    .build();

            Long savedId = documentRepository.save(doc).getId();

            Map<String, Object> docMetadata = new HashMap<>();
            docMetadata.put(MetadataKeys.CATEGORY, docType);
            docMetadata.put(MetadataKeys.DOC_VERSION, "v1." + index + ".0");

            saveToVectorStore(savedId, SourceType.DOCUMENT, projectId, contentToSave, docMetadata);

            eventPublisher.publishEvent(
                new SourceDataSavedEvent(
                    savedId, SourceType.DOCUMENT, contentToSave, projectId));
        });

        log.info("Generated Document [{}/10]: {}", index + 1, docType);
    }

    private void createDummyIssue(String projectId, int index) throws JsonProcessingException {
        IssueType[] randomTypes = IssueType.values();
        IssuePriority[] randomPrioritys = IssuePriority.values();

        IssueType issueType = randomTypes[index % randomTypes.length];
        IssuePriority issuePriority = randomPrioritys[index % randomPrioritys.length];

        String name = NAMES.get(index % NAMES.size());
        String dept = DEPARTMENTS.get(index % DEPARTMENTS.size());
        String role = ROLES.get(index % ROLES.size());

        String template = PromptUtils.loadPrompt(dummyIssuePromptResource);
        String promptText =
                template.replace("{name}", name)
                        .replace("{dept}", dept)
                        .replace("{role}", role)
                        .replace("{issueType}", issueType.name())
                        .replace("{priority}", issuePriority.name());

        String jsonResponse = chatClient.prompt().user(promptText).call().content();
        if (jsonResponse == null) {jsonResponse = "{}";}

        String cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim();
        IssueDummyResponse dto = objectMapper.readValue(cleanJson, IssueDummyResponse.class);

        transactionTemplate.executeWithoutResult(status -> {
            Issue issue =
                Issue.builder()
                    .projectId(projectId)
                    .title(dto.title())
                    .description(dto.description())
                    .resolutionNote(dto.resolutionNote())
                    .priority(issuePriority)
                    .assignee(name)
                    .issueType(issueType)
                    .status(IssueStatus.RESOLVED)
                    .build();

            issue.resolve(dto.resolutionNote());

            Long savedId = issueRepository.save(issue).getId();

            String fullContent =
                String.format(
                    """
            [이슈 정보]
            유형: %s
            담당자: %s (%s %s)
            우선순위: %s
            제목: %s
            내용: %s
            해결: %s
            """,
                    issueType,
                    name,
                    dept,
                    role,
                    issuePriority,
                    dto.title(),
                    dto.description(),
                    dto.resolutionNote());

            Map<String, Object> issueMetadata = new HashMap<>();
            issueMetadata.put(MetadataKeys.CATEGORY, issueType.name());
            issueMetadata.put(MetadataKeys.ISSUE_PRIORITY, issuePriority.name());
            issueMetadata.put(MetadataKeys.AUTHOR_ID, name);

            saveToVectorStore(savedId, SourceType.ISSUE, projectId, fullContent, issueMetadata);

            String contentForSummary = "이슈 제목: " + dto.title() + "\n해결 내용: " + dto.resolutionNote();
            eventPublisher.publishEvent(
                new SourceDataSavedEvent(savedId, SourceType.ISSUE, contentForSummary, projectId));
        });
    }

    private void saveToVectorStore(
            Long sourceId,
            SourceType type,
            String projectId,
            String content,
            Map<String, Object> specificMetadata) {
        TokenTextSplitter splitter = new TokenTextSplitter(1000, 400, 10, 10000, true);

        List<String> chunks =
                splitter
                        .apply(List.of(new org.springframework.ai.document.Document(content)))
                        .stream()
                        .map(org.springframework.ai.document.Document::getText)
                        .toList();

        List<org.springframework.ai.document.Document> vectorDocs = new ArrayList<>();

        for (String chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(MetadataKeys.SOURCE_ID, sourceId);
            metadata.put(MetadataKeys.SOURCE_TYPE, type.name());
            metadata.put(MetadataKeys.PROJECT_ID, projectId);

            if (specificMetadata != null && !specificMetadata.isEmpty()) {
                metadata.putAll(specificMetadata);
            }

            vectorDocs.add(new org.springframework.ai.document.Document(chunk, metadata));
        }

        vectorStore.add(vectorDocs);
        log.info("Saved {} vector chunks. Source: {}-{}", vectorDocs.size(), type, sourceId);
    }
}

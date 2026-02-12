package kr.java.patchnotedemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import kr.java.patchnotedemo.dto.IssueDummyResponse;
import kr.java.patchnotedemo.entity.Document;
import kr.java.patchnotedemo.entity.Issue;
import kr.java.patchnotedemo.enums.IssuePriority;
import kr.java.patchnotedemo.enums.IssueStatus;
import kr.java.patchnotedemo.enums.IssueType;
import kr.java.patchnotedemo.enums.SourceType;
import kr.java.patchnotedemo.repository.DocumentRepository;
import kr.java.patchnotedemo.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class DummyDataService {

    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;
    private final IssueRepository issueRepository;
    private final VectorStore vectorStore;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private final List<String> DOC_TOPICS =
            List.of(
                    "신규 레이드 던전 '얼어붙은 왕좌' 기획서",
                    "길드 대항전 PvP 밸런스 조정안",
                    "서버 아키텍처 MSA 전환 설계서",
                    "여름방학 이벤트 '해변의 수호자' 상세 기획",
                    "신규 캐릭터 '그림자 암살자' 스킬 명세서",
                    "시즌 패스 '여명의 기사단' 보상 구조 및 BM 설계서",
                    "신규 필드 '가시나무 숲' 몬스터 배치 및 드랍 테이블 상세",
                    "길드 하우징 시스템 건축 및 상호작용 기획안",
                    "클라이언트 메모리 최적화 및 텍스처 로딩 속도 개선 리포트",
                    "메인 시나리오 챕터 12 '타락한 신전' 스크립트 및 컷신 연출안");
    private final List<String> NAMES = List.of("김철수", "이영희", "박민수", "최지우", "정재석", "강하늘");
    private final List<String> DEPARTMENTS = List.of("서버팀", "클라이언트팀", "기획팀", "QA팀", "보안팀", "엔진팀");
    private final List<String> ROLES = List.of("팀장", "시니어", "주니어", "파트장", "인턴");

    @Value("classpath:prompts/dummy-doc.st")
    private Resource dummyDocPromptResource;

    @Value("classpath:prompts/dummy-issue.st")
    private Resource dummyIssuePromptResource;

    @Transactional
    public void generateDummy(SourceType type, String projectId) throws JsonProcessingException {
        if (type == SourceType.DOCUMENT) {
            createDummyDocument(projectId);
        } else {
            createDummyIssue(projectId);
        }
    }

    private void createDummyDocument(String projectId) {
        String topic = DOC_TOPICS.get(random.nextInt(DOC_TOPICS.size()));
        String genre = "MMORPG";
        String gameName = "레전드 오브 스프링";
        String role = "10년차 시니어 기획자";

        String generatedContent =
                chatClient
                        .prompt()
                        .user(
                                userSpec ->
                                        userSpec.text(dummyDocPromptResource) // 리소스 파일 주입
                                                .param("genre", genre) // {genre} 치환
                                                .param("gameName", gameName) // {gameName} 치환
                                                .param("role", role) // {role} 치환
                                                .param("topic", topic) // {topic} 치환
                                )
                        .call()
                        .content();

        if (generatedContent == null) {
            generatedContent = "";
        }

        Document doc =
                Document.builder()
                        .projectId(projectId)
                        .title(topic)
                        .author("AI")
                        .version("v1.0.0")
                        .category("기획서")
                        .fileUrl("http://dummy-s3/docs/" + UUID.randomUUID())
                        .fileType("PDF")
                        .isProcessed(true)
                        .build();

        Long savedId = documentRepository.save(doc).getId();

        TokenTextSplitter splitter = new TokenTextSplitter(1000, 400, 10, 10000, true);
        // (참고: 청킹 사이즈는 모델에 따라 조절. Gemini는 문맥이 기니까 1000 정도도 괜찮음)

        List<String> chunks =
                splitter
                        .apply(
                                List.of(
                                        new org.springframework.ai.document.Document(
                                                generatedContent)))
                        .stream()
                        .map(org.springframework.ai.document.Document::getText)
                        .toList();
        List<org.springframework.ai.document.Document> vectorDocs = new ArrayList<>();

        for (String chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_id", savedId);
            metadata.put("source_type", SourceType.DOCUMENT.name());
            metadata.put("project_id", projectId);

            vectorDocs.add(new org.springframework.ai.document.Document(chunk, metadata));
        }

        vectorStore.add(vectorDocs);
    }

    private void createDummyIssue(String projectId) throws JsonProcessingException {
        IssueType randomType = IssueType.values()[random.nextInt(IssueType.values().length)];
        IssuePriority randomPriority =
                IssuePriority.values()[random.nextInt(IssuePriority.values().length)];

        String name = NAMES.get(random.nextInt(NAMES.size()));
        String dept = DEPARTMENTS.get(random.nextInt(DEPARTMENTS.size()));
        String role = ROLES.get(random.nextInt(ROLES.size()));

        String jsonResponse =
                chatClient
                        .prompt()
                        .user(
                                userSpec ->
                                        userSpec.text(dummyIssuePromptResource)
                                                .param("name", name)
                                                .param("dept", dept)
                                                .param("role", role)
                                                .param("issueType", randomType.name())
                                                .param("priority", randomPriority.name()))
                        .call()
                        .content();

        if (jsonResponse == null) {
            jsonResponse = "{}";
        }

        String cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim();
        IssueDummyResponse dto = objectMapper.readValue(cleanJson, IssueDummyResponse.class);

        Issue issue =
                Issue.builder()
                        .projectId(projectId)
                        .title(dto.title())
                        .description(dto.description())
                        .resolutionNote(dto.resolutionNote())
                        .priority(randomPriority)
                        .assignee(name)
                        .issueType(randomType)
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
                        randomType,
                        name,
                        dept,
                        role,
                        randomPriority,
                        dto.title(),
                        dto.description(),
                        dto.resolutionNote());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_id", savedId);
        metadata.put("source_type", SourceType.ISSUE.name());
        metadata.put("project_id", projectId);
        metadata.put("issue_type", randomType.name());

        vectorStore.add(
                List.of(new org.springframework.ai.document.Document(fullContent, metadata)));
    }
}

package kr.java.springbootworker.dto.request;

import java.util.Map;

public record RawLogRequest(
        String projectId,       // 프로젝트 식별자
        String sessionId,       // 게임 세션 ID
        String userId,          // 유저 식별자 (Nullable)
        String severity,        // 로그 레벨 (INFO, WARN, ERROR 등)
        String body,            // 로그 본문
        String occurredAt,      // 클라이언트 발생 시각 (String으로 수신 후 보정)
        String traceId,         // 트랜잭션 추적 ID
        String spanId,          // 구간 추적 ID
        Map<String, Object> resource,   // 정적 환경 정보 (Semantic Convention)
        Map<String, Object> attributes  // 동적 상황 정보 (Performance, Context 등)
) {}

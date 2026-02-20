package kr.java.minyeonglogapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Getter
@NoArgsConstructor
@ToString
// 정의되지 않은 필드가 와도 에러를 내지 않고 무시
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameLogDto {

    // 고정 필드 (RDB 일반 컬럼 예정)
    private String projectId;
    private String sessionId;
    private String userId;
    private String severity;
    private String body;
    private String occurredAt; // 시간은 일단 String으로 받고 나중에 파싱
    private String traceId;
    private String spanId;

    // 유동 필드 (RDB JSONB 컬럼 예정)
    // JSON 내부의 객체를 Map으로 유연하게 받음
    private Map<String, Object> resource;
    private Map<String, Object> attributes;
}
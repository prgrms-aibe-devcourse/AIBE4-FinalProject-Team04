package kr.java.springbootworker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.dto.request.RawLogRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LogMapper {

    private final ObjectMapper objectMapper;

    public Log toEntity(Map<String, String> map) throws JsonProcessingException {
        if (map.get("projectId") == null || map.get("body") == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        // logId 처리: null이거나 빈 문자열이면 새 UUID 생성
        String logIdStr = map.get("logId");
        UUID logId = (logIdStr != null && !logIdStr.isEmpty())
                ? UUID.fromString(logIdStr)
                : UUID.randomUUID();

        return Log.builder()
                .logId(logId)
                .projectId(map.get("projectId"))
                .sessionId(map.get("sessionId"))
                .userId(map.get("userId"))
                .severity(map.getOrDefault("severity", "INFO"))
                .body(map.get("body"))
                .occurredAt(parseTime(map.get("occurredAt")))
                .ingestedAt(parseTime(map.get("ingestedAt")))
                .traceId(map.get("traceId"))
                .spanId(map.get("spanId"))
                .fingerprint(map.get("fingerprint"))
                .resource(objectMapper.readValue(map.getOrDefault("resource", "{}"), new TypeReference<Map<String, Object>>() {}))
                .attributes(objectMapper.readValue(map.getOrDefault("attributes", "{}"), new TypeReference<Map<String, Object>>() {}))
                .build();
    }

    public Log toEntity(RawLogRequest dto) {
        return Log.builder()
                .logId(UUID.randomUUID())
                .projectId(dto.projectId())
                .sessionId(dto.sessionId())
                .userId(dto.userId())
                .severity(dto.severity())
                .body(dto.body())
                .occurredAt(parseTime(dto.occurredAt()))
                .ingestedAt(OffsetDateTime.now())
                .traceId(dto.traceId())
                .spanId(dto.spanId())
                .fingerprint(null)
                .resource(dto.resource() != null ? dto.resource() : Map.of())
                .attributes(dto.attributes() != null ? dto.attributes() : Map.of())
                .build();
    }

    private OffsetDateTime parseTime(String timeStr) {
        if (timeStr == null) return OffsetDateTime.now();
        try {
            return OffsetDateTime.parse(timeStr);
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }
}

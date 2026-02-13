package kr.java.springbootworker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.springbootworker.domain.entity.logs.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final LogBufferService logBufferService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> value = message.getValue();
        
        try {
            Log logEntity = convertToLog(value);
            logBufferService.add(logEntity, message.getId());
        } catch (Exception e) {
            log.error("Failed to convert message to Log entity: {}", value, e);
        }
    }

    private Log convertToLog(Map<String, String> map) throws JsonProcessingException {
        return Log.builder()
                .logId(UUID.fromString(map.get("logId")))
                .projectId(map.get("projectId"))
                .sessionId(map.get("sessionId"))
                .userId(map.get("userId"))
                .severity(map.get("severity"))
                .body(map.get("body"))
                .occurredAt(OffsetDateTime.parse(map.get("occurredAt")))
                .ingestedAt(OffsetDateTime.parse(map.get("ingestedAt")))
                .traceId(map.get("traceId"))
                .spanId(map.get("spanId"))
                .fingerprint(map.get("fingerprint"))
                .resource(objectMapper.readValue(map.get("resource"), new TypeReference<Map<String, Object>>() {}))
                .attributes(objectMapper.readValue(map.get("attributes"), new TypeReference<Map<String, Object>>() {}))
                .build();
    }
}

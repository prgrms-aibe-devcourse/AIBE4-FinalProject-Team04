package kr.java.springbootworker.controller;

import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.repository.LogJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final LogJdbcRepository logJdbcRepository;

    @PostMapping("/bulk-insert")
    public String bulkInsert(@RequestBody List<LogDto> logDtos) {
        List<Log> logs = logDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        
        long startTime = System.currentTimeMillis();
        logJdbcRepository.saveAll(logs);
        long endTime = System.currentTimeMillis();
        
        return "Inserted " + logs.size() + " logs in " + (endTime - startTime) + "ms";
    }

    private Log toEntity(LogDto dto) {
        return Log.builder()
                .logId(UUID.randomUUID())
                .projectId(dto.projectId())
                .sessionId(dto.sessionId())
                .userId(dto.userId())
                .severity(dto.severity())
                .body(dto.body())
                .occurredAt(OffsetDateTime.now())
                .ingestedAt(OffsetDateTime.now())
                .traceId(UUID.randomUUID().toString())
                .spanId(UUID.randomUUID().toString())
                .fingerprint(dto.fingerprint())
                .resource(dto.resource())
                .attributes(dto.attributes())
                .build();
    }
    
    public record LogDto(
            String projectId,
            String sessionId,
            String userId,
            String severity,
            String body,
            String fingerprint,
            java.util.Map<String, Object> resource,
            java.util.Map<String, Object> attributes
    ) {}
}

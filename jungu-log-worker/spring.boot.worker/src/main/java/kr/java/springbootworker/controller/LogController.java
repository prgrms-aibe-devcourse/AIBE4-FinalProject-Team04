package kr.java.springbootworker.controller;

import jakarta.validation.Valid;
import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.dto.request.RawLogRequest;
import kr.java.springbootworker.repository.LogJdbcRepository;
import kr.java.springbootworker.service.LogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Validated
@Profile("!prod")
public class LogController {

    private final LogJdbcRepository logJdbcRepository;
    private final LogMapper logMapper;

    @PostMapping("/bulk")
    public String bulkInsert(@RequestBody List<@Valid RawLogRequest> logDtos) {
        List<Log> logs = logDtos.stream()
                .map(logMapper::toEntity)
                .collect(Collectors.toList());
        
        long startTime = System.currentTimeMillis();
        logJdbcRepository.saveAll(logs);
        long endTime = System.currentTimeMillis();
        
        return "Inserted " + logs.size() + " logs in " + (endTime - startTime) + "ms";
    }
}

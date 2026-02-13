package kr.java.springbootworker.controller;

import jakarta.validation.Valid;
import kr.java.springbootworker.dto.request.RawLogRequest;
import kr.java.springbootworker.service.LogBufferService;
import kr.java.springbootworker.service.LogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Validated
@Profile("!prod")
public class LogApiController {

    private final LogBufferService logBufferService;
    private final LogMapper logMapper;

    @PostMapping("/bulk")
    public String bulkInsert(@RequestBody List<@Valid RawLogRequest> logDtos) {
        logDtos.forEach(dto -> {
            logBufferService.add(logMapper.toEntity(dto));
        });
        
        return "Accepted " + logDtos.size() + " logs.";
    }
}

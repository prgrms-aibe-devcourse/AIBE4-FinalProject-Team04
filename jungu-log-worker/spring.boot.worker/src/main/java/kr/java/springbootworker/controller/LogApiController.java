package kr.java.springbootworker.controller;

import jakarta.validation.Valid;
import kr.java.springbootworker.dto.request.RawLogRequest;
import kr.java.springbootworker.service.LogService;
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

    private final LogService logService;

    @PostMapping("/bulk")
    public String bulkInsert(@RequestBody List<@Valid RawLogRequest> logDtos) {
        long startTime = System.currentTimeMillis();
        logService.bulkInsert(logDtos);
        long endTime = System.currentTimeMillis();
        
        return "Inserted " + logDtos.size() + " logs in " + (endTime - startTime) + "ms";
    }
}

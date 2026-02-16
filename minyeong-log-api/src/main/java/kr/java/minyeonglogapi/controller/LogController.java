package kr.java.minyeonglogapi.controller;

import kr.java.minyeonglogapi.dto.GameLogDto;
import kr.java.minyeonglogapi.service.LogProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogProducerService logProducerService;

    /**
     * 배치 로그 수신 API
     * 클라이언트가 [ {log1}, {log2}, ... ] 형태로 보냄
     */
    @PostMapping("/batch")
    public ResponseEntity<String> receiveBatchLogs(@RequestBody List<GameLogDto> logs) {
        // 1. 로그가 비어있으면 바로 리턴 (방어 로직)
        if (logs == null || logs.isEmpty()) {
            return ResponseEntity.badRequest().body("Log list is empty");
        }

        // 2. 서비스 계층으로 리스트 통째로 전달 (Pipeline 태우기 위함)
        logProducerService.sendLogsInBatch(logs);

        // 3. 비동기 처리이므로 클라이언트에게는 즉시 OK 응답
        return ResponseEntity.ok("Received " + logs.size() + " logs");
    }
}
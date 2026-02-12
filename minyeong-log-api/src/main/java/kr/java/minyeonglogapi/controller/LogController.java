package kr.java.minyeonglogapi.controller;

import kr.java.minyeonglogapi.dto.LogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LogController {

    // Redis에 텍스트를 저장하기 위한 도구 주입
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/logs")
    public ResponseEntity<String> receiveLog(@RequestBody LogRequest request) {

        // 1. [눈으로 확인] 서버 로그에 찍어보기 (docker logs로 확인 가능)
        log.info("📝 [Log Received] Service: {}, Level: {}, Message: {}",
                request.getService(), request.getLevel(), request.getMessage());

        // 2. [데이터 검증] Redis에 저장하기 (List 자료구조 사용)
        // 키: "incoming:logs", 값: 로그 내용 + 시간
        String logEntry = String.format("[%s] %s: %s", LocalDateTime.now(), request.getLevel(), request.getMessage());
        redisTemplate.opsForList().rightPush("incoming:logs", logEntry);

        // 3. [응답] k6에게 "잘 받았어(200 OK)"라고 응답
        return ResponseEntity.ok("Log saved successfully");
    }
}

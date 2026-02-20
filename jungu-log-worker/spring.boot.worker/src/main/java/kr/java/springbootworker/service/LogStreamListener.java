package kr.java.springbootworker.service;

import kr.java.springbootworker.domain.entity.logs.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final LogBufferService logBufferService;
    private final LogMapper logMapper;
    private final BackpressureManager backpressureManager;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        applyBackpressure();

        try {
            Log logEntity = logMapper.toEntity(message.getValue());
            logBufferService.add(logEntity, message.getId());
        } catch (Exception e) {
            // 보안: 민감 정보(value)는 로그에 남기지 않고 Message ID만 기록
            log.error("Failed to process Redis Stream message. ID: {}", message.getId(), e);
        }
    }

    private void applyBackpressure() {
        long sleepMs = backpressureManager.getSleepMillis();
        if (sleepMs <= 0) {
            return;
        }

        log.warn("[Backpressure] DB 지연 감지 (state={}, avg={} ms) - {} ms 대기",
                backpressureManager.getState(),
                String.format("%.1f", backpressureManager.getAvgLatencyMs()),
                sleepMs);

        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Backpressure] 대기 중 인터럽트 발생. 소비를 재개합니다.");
        }
    }
}

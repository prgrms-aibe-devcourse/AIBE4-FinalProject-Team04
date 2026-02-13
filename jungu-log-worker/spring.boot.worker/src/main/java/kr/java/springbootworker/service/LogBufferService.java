package kr.java.springbootworker.service;

import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.repository.LogJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogBufferService {

    private final LogJdbcRepository logJdbcRepository;
    private final List<Log> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 1000;

    public synchronized void add(Log log) {
        buffer.add(log);
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        
        try {
            logJdbcRepository.saveAll(buffer);
            log.info("Flushed {} logs to DB", buffer.size());
            buffer.clear();
        } catch (Exception e) {
            log.error("Failed to flush logs to DB", e);
            // 실패 시 재시도 로직이나 DLQ 처리 필요하지만 여기서는 생략
        }
    }
}

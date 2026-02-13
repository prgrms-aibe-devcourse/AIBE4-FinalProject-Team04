package kr.java.springbootworker.service;

import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.repository.LogJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogBufferService {

    private final LogJdbcRepository logJdbcRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ConcurrentLinkedQueue<LogWrapper> buffer = new ConcurrentLinkedQueue<>();

    @Value("${worker.bulk.size:1000}")
    private int batchSize;

    @Value("${redis.stream.key:log-stream}")
    private String streamKey;

    @Value("${redis.stream.group:log-group}")
    private String consumerGroup;

    public void add(Log log, RecordId recordId) {
        buffer.offer(new LogWrapper(log, recordId));
        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    @Scheduled(fixedDelayString = "${worker.bulk.flush-interval-ms:1000}")
    public synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        List<LogWrapper> wrappersToSave = new ArrayList<>();
        LogWrapper wrapper;
        while ((wrapper = buffer.poll()) != null) {
            wrappersToSave.add(wrapper);
            if (wrappersToSave.size() >= batchSize) {
                break;
            }
        }

        if (wrappersToSave.isEmpty()) {
            return;
        }

        List<Log> logs = wrappersToSave.stream().map(LogWrapper::log).collect(Collectors.toList());
        
        try {
            logJdbcRepository.saveAll(logs);
            
            // DB 저장 성공 시 ACK 전송
            List<RecordId> recordIds = wrappersToSave.stream().map(LogWrapper::recordId).collect(Collectors.toList());
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordIds.toArray(new RecordId[0]));
            
            log.info("Flushed {} logs to DB and acknowledged", logs.size());
        } catch (Exception e) {
            log.error("Failed to flush logs to DB", e);
            // 실패 시 재시도 로직 필요 (여기서는 생략)
        }
    }

    public record LogWrapper(Log log, RecordId recordId) {}
}

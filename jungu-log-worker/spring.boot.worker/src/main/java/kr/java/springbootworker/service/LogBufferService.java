package kr.java.springbootworker.service;

import jakarta.annotation.PostConstruct;
import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.repository.LogJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogBufferService {

    private final LogJdbcRepository logJdbcRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ConcurrentLinkedQueue<LogWrapper> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);

    @Value("${worker.bulk.size:1000}")
    private int batchSize;

    @Value("${worker.buffer.max-size:10000}")
    private int maxBufferSize;

    @Value("${redis.stream.key:log-stream}")
    private String streamKey;

    @Value("${redis.stream.group:log-group}")
    private String consumerGroup;

    @PostConstruct
    public void init() {
        if (batchSize <= 0) {
            log.warn("Invalid batch size: {}. Resetting to 1000.", batchSize);
            batchSize = 1000;
        }
    }

    public void add(Log log, RecordId recordId) {
        if (buffer.size() >= maxBufferSize) {
            log.warn("Buffer is full (size: {}). Dropping log.", buffer.size());
            return; 
        }
        
        buffer.offer(new LogWrapper(log, recordId));
        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    @Scheduled(fixedDelayString = "${worker.bulk.flush-interval-ms:1000}")
    @Transactional
    public void flush() {
        if (!isFlushing.compareAndSet(false, true)) {
            return;
        }

        try {
            if (buffer.isEmpty()) {
                return;
            }

            List<LogWrapper> wrappersToSave = new ArrayList<>();
            LogWrapper wrapper;
            while (wrappersToSave.size() < batchSize && (wrapper = buffer.poll()) != null) {
                wrappersToSave.add(wrapper);
            }

            if (wrappersToSave.isEmpty()) {
                return;
            }

            List<Log> logs = wrappersToSave.stream().map(LogWrapper::log).collect(Collectors.toList());
            
            try {
                logJdbcRepository.saveAll(logs);
                
                List<RecordId> recordIds = wrappersToSave.stream().map(LogWrapper::recordId).collect(Collectors.toList());
                redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordIds.toArray(new RecordId[0]));
                
                log.info("Flushed {} logs to DB and acknowledged", logs.size());
            } catch (Exception e) {
                log.error("Failed to flush logs to DB", e);
            }
        } finally {
            isFlushing.set(false);
        }
    }

    public record LogWrapper(Log log, RecordId recordId) {}
}

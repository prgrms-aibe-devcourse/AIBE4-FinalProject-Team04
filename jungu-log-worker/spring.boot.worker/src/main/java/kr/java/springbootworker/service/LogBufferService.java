package kr.java.springbootworker.service;

import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.repository.LogJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogBufferService {

    private final LogJdbcRepository logJdbcRepository;
    private final ConcurrentLinkedQueue<Log> buffer = new ConcurrentLinkedQueue<>();

    @Value("${worker.bulk.size:1000}")
    private int batchSize;

    public void add(Log log) {
        buffer.offer(log);
        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    @Scheduled(fixedDelayString = "${worker.bulk.flush-interval-ms:1000}")
    public synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        List<Log> logsToSave = new ArrayList<>();
        Log logItem;
        // 큐에서 데이터를 꺼내서 임시 리스트에 담음 (최대 batchSize만큼)
        while ((logItem = buffer.poll()) != null) {
            logsToSave.add(logItem);
            if (logsToSave.size() >= batchSize) {
                break;
            }
        }

        if (logsToSave.isEmpty()) {
            return;
        }
        
        try {
            logJdbcRepository.saveAll(logsToSave);
            log.info("Flushed {} logs to DB", logsToSave.size());
        } catch (Exception e) {
            log.error("Failed to flush logs to DB", e);
        }
    }
}

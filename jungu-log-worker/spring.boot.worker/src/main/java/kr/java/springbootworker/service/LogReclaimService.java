package kr.java.springbootworker.service;

import kr.java.springbootworker.domain.entity.logs.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogReclaimService {

    private final RedisTemplate<String, String> redisTemplate;
    private final LogBufferService logBufferService;
    private final LogMapper logMapper;

    @Value("${redis.stream.key:log-stream}")
    private String streamKey;

    @Value("${redis.stream.group:log-group}")
    private String consumerGroup;

    @Value("${redis.stream.consumer:log-consumer-1}")
    private String consumerName;

    @Scheduled(fixedDelay = 60000)
    public void reclaimPendingMessages() {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        
        try {
            PendingMessages pendingMessages = streamOps.pending(
                    streamKey,
                    Consumer.from(consumerGroup, consumerName),
                    Range.unbounded(),
                    100
            );

            if (pendingMessages.isEmpty()) {
                return;
            }

            List<String> idsToClaim = pendingMessages.stream()
                    .filter(msg -> msg.getElapsedTimeSinceLastDelivery().toMillis() > 10000)
                    .map(msg -> msg.getId().getValue())
                    .collect(Collectors.toList());

            if (idsToClaim.isEmpty()) {
                return;
            }

            log.info("Found {} pending messages to reclaim.", idsToClaim.size());

            List<MapRecord<String, String, String>> claimedMessages = streamOps.claim(
                    streamKey,
                    consumerGroup,
                    consumerName,
                    Duration.ofMillis(10000),
                    idsToClaim.stream().map(RecordId::of).toArray(RecordId[]::new)
            );

            for (MapRecord<String, String, String> message : claimedMessages) {
                try {
                    Log logEntity = logMapper.toEntity(message.getValue());
                    logBufferService.add(logEntity, message.getId());
                    log.debug("Reclaimed message ID: {}", message.getId());
                } catch (Exception e) {
                    log.error("Failed to reclaim message ID: {}", message.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error during pending message reclaim", e);
        }
    }
}

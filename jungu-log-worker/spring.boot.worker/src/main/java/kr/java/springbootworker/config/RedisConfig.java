package kr.java.springbootworker.config;

import kr.java.springbootworker.service.LogStreamListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    @Value("${redis.stream.key:log-stream}")
    private String streamKey;

    @Value("${redis.stream.group:log-group}")
    private String consumerGroup;

    @Value("${redis.stream.consumer:log-consumer-1}")
    private String consumerName;

    private final LogStreamListener logStreamListener;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory factory) {

        // Consumer Group 생성 (Connection을 안전하게 닫음)
        createConsumerGroupIfNotExists(factory);

        // ListenerContainer 옵션 설정
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        // ListenerContainer 생성 및 Subscription 등록
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(factory, options);

        container.receive(
                Consumer.from(consumerGroup, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                logStreamListener
        );

        // Spring lifecycle 관리 시작
        container.start();
        return container;
    }

    private void createConsumerGroupIfNotExists(RedisConnectionFactory factory) {
        // try-with-resources로 Connection을 안전하게 닫음
        try (var connection = factory.getConnection()) {
            connection.streamCommands().xGroupCreate(
                    streamKey.getBytes(),
                    consumerGroup,
                    ReadOffset.from("0-0"),
                    true
            );
            log.info("Redis Stream consumer group created: {}", consumerGroup);
        } catch (Exception e) {
            // "already exists" 에러는 정상적인 상황이므로 무시
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group already exists: {}", consumerGroup);
            } else {
                log.error("Failed to create Redis Stream consumer group", e);
            }
        }
    }
}

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
    public Subscription subscription(RedisConnectionFactory factory) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(factory, options);

        try {
            // 그룹이 없으면 생성 시도 (선택 사항)
             factory.getConnection().streamCommands().xGroupCreate(streamKey.getBytes(), consumerGroup, ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // 이미 존재하면 무시
        }

        Subscription subscription = listenerContainer.receive(
                Consumer.from(consumerGroup, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                logStreamListener
        );

        listenerContainer.start();
        return subscription;
    }
}

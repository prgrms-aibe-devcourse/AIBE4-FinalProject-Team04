package kr.java.minyeonglogapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.minyeonglogapi.dto.GameLogDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogProducerService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Stream의 키 이름 (데모는 상수로 관리)
    private static final String STREAM_KEY = "game-log-stream";

    public void sendLogsInBatch(List<GameLogDto> logs) {
        if (logs == null || logs.isEmpty()) return;

        // Pipeline 시작
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {

            // 커넥션을 String 전용으로 캐스팅 (사용하기 더 편함)
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

            for (GameLogDto logDto : logs) {
                try {
                    // 1. DTO -> JSON String 직렬화 (가장 비용이 큰 작업)
                    String jsonPayload = objectMapper.writeValueAsString(logDto);

                    // 2. Stream에 저장할 Body 생성 (Key: "payload", Value: JSON)
                    // Collections.singletonMap은 Map.of("payload", json)과 같으며 메모리를 적게 씀
                    Map<String, String> body = Collections.singletonMap("payload", jsonPayload);

                    // 3. XADD 명령어 실행 (실제 전송 X, 버퍼에 쌓임)
                    // xAdd(키, ID생성전략, 데이터Map)
                    // executePipelined 안에서는 리턴값이 null임
                    stringRedisConn.xAdd(STREAM_KEY, body);

                } catch (Exception e) {
                    // 배치 중 로그 하나가 잘못돼도 나머지는 보내야 함
                    log.error("Failed to serialize log: {}", logDto, e);
                }
            }
            return null; // 콜백의 리턴값은 무시됨
        });
        // Pipeline 종료 및 일괄 전송. 실제로 Redis로 날아가는 부분

        log.info("Sent {} logs to Redis Stream via Pipeline", logs.size());
    }
}

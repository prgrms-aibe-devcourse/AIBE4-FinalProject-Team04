package kr.java.springbootworker.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.springbootworker.domain.entity.logs.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LogJdbcRepository.class, ObjectMapper.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/logdb?reWriteBatchedInserts=true",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.datasource.driver-class-name=org.postgresql.Driver"
})
class LogJdbcRepositoryTest {

    @Autowired
    private LogJdbcRepository logJdbcRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS log (" +
                "log_id UUID PRIMARY KEY, " +
                "project_id VARCHAR(255) NOT NULL, " +
                "session_id VARCHAR(255) NOT NULL, " +
                "user_id VARCHAR(255), " +
                "severity VARCHAR(255) NOT NULL, " +
                "body TEXT NOT NULL, " +
                "occurred_at TIMESTAMPTZ NOT NULL, " +
                "ingested_at TIMESTAMPTZ NOT NULL, " +
                "trace_id VARCHAR(255), " +
                "span_id VARCHAR(255), " +
                "fingerprint VARCHAR(255), " +
                "resource JSONB NOT NULL, " +
                "attributes JSONB NOT NULL" +
                ")");
        
        // 테스트 전 데이터 초기화 (선택 사항)
        jdbcTemplate.execute("TRUNCATE TABLE log");
    }

    @Test
    @DisplayName("Bulk Insert 동작 테스트")
    void bulkInsertTest() {
        // given
        int dataSize = 100;
        List<Log> logs = new ArrayList<>();
        for (int i = 0; i < dataSize; i++) {
            Map<String, Object> resource = new HashMap<>();
            resource.put("service", "test-service");
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("http.method", "GET");

            logs.add(Log.builder()
                    .logId(UUID.randomUUID())
                    .projectId("project-1")
                    .sessionId("session-" + i)
                    .userId("user-" + i)
                    .severity("INFO")
                    .body("Log message " + i)
                    .occurredAt(OffsetDateTime.now())
                    .ingestedAt(OffsetDateTime.now())
                    .traceId(UUID.randomUUID().toString())
                    .spanId(UUID.randomUUID().toString())
                    .fingerprint("fingerprint-" + i)
                    .resource(resource)
                    .attributes(attributes)
                    .build());
        }

        // when
        logJdbcRepository.saveAll(logs);

        // then
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM log", Integer.class);
        assertThat(count).isEqualTo(dataSize);
    }
}

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
        // 기존 테이블 삭제 (테스트 반복 실행을 위해)
        jdbcTemplate.execute("DROP TABLE IF EXISTS log CASCADE");

        // 파티셔닝 테이블 생성
        jdbcTemplate.execute("CREATE TABLE log (" +
                "log_id UUID, " +
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
                "attributes JSONB NOT NULL, " +
                "PRIMARY KEY (log_id, occurred_at)" + 
                ") PARTITION BY RANGE (occurred_at)");
        
        // 파티션 생성 (테스트용 Default 파티션)
        jdbcTemplate.execute("CREATE TABLE log_default PARTITION OF log DEFAULT");
    }

    @Test
    @DisplayName("Bulk Insert 동작 및 파티셔닝 테스트")
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
        // 메인 테이블 조회
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM log", Integer.class);
        assertThat(count).isEqualTo(dataSize);

        // 파티션 테이블 직접 조회 (데이터가 실제로 파티션으로 들어갔는지 확인)
        Integer partitionCount = jdbcTemplate.queryForObject("SELECT count(*) FROM log_default", Integer.class);
        assertThat(partitionCount).isEqualTo(dataSize);
    }
}

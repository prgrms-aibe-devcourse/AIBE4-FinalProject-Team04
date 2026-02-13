package kr.java.springbootworker.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.springbootworker.domain.entity.logs.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogJdbcRepositoryUnitTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LogJdbcRepository logJdbcRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(logJdbcRepository, "batchSize", 1000);
    }

    @Test
    @DisplayName("saveAll 호출 시 batchUpdate가 실행되어야 한다")
    void saveAll_shouldCallBatchUpdate() {
        // given
        Log log = Log.builder()
                .logId(UUID.randomUUID())
                .projectId("test-project")
                .sessionId("test-session")
                .severity("INFO")
                .body("test body")
                .resource(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .build();
        List<Log> logs = List.of(log);

        // when
        logJdbcRepository.saveAll(logs);

        // then
        verify(jdbcTemplate, times(1)).batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class));
    }
}

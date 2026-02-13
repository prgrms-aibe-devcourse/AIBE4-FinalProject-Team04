package kr.java.springbootworker.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.springbootworker.domain.entity.logs.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LogJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveAll(List<Log> logs) {
        String sql = "INSERT INTO log (log_id, project_id, session_id, user_id, severity, body, occurred_at, ingested_at, trace_id, span_id, fingerprint, resource, attributes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Log log = logs.get(i);
                ps.setObject(1, log.getLogId());
                ps.setString(2, log.getProjectId());
                ps.setString(3, log.getSessionId());
                ps.setString(4, log.getUserId());
                ps.setString(5, log.getSeverity());
                ps.setString(6, log.getBody());
                ps.setObject(7, log.getOccurredAt());
                ps.setObject(8, log.getIngestedAt());
                ps.setString(9, log.getTraceId());
                ps.setString(10, log.getSpanId());
                ps.setString(11, log.getFingerprint());
                
                try {
                    ps.setString(12, objectMapper.writeValueAsString(log.getResource()));
                    ps.setString(13, objectMapper.writeValueAsString(log.getAttributes()));
                } catch (JsonProcessingException e) {
                    throw new SQLException("Error converting map to json", e);
                }
            }

            @Override
            public int getBatchSize() {
                return logs.size();
            }
        });
    }
}

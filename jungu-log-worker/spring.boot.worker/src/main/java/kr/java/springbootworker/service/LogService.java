package kr.java.springbootworker.service;

import kr.java.springbootworker.domain.entity.logs.Log;
import kr.java.springbootworker.dto.request.RawLogRequest;
import kr.java.springbootworker.repository.LogJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogJdbcRepository logJdbcRepository;
    private final LogMapper logMapper;

    @Transactional
    public void bulkInsert(List<RawLogRequest> logDtos) {
        List<Log> logs = logDtos.stream()
                .map(logMapper::toEntity)
                .collect(Collectors.toList());
        
        logJdbcRepository.saveAll(logs);
    }
}

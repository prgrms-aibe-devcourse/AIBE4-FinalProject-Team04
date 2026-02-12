package kr.java.springbootworker.domain.entity.logs;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LogId implements Serializable { // 파티셔닝을 위해 logId와 occurredAt을 묶는 식별자 클래스
    private UUID logId;
    private OffsetDateTime occurredAt;
}

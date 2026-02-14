package kr.java.patchnotedemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vector_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class ChunkData {
    @Id private UUID id;

    @Column(columnDefinition = "text")
    private String content;
}

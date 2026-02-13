package kr.java.patchnotedemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vector_store")
@Getter
@Immutable
public class ChunkData {
    @Id private UUID id;

    @Column(columnDefinition = "text")
    private String content;
}

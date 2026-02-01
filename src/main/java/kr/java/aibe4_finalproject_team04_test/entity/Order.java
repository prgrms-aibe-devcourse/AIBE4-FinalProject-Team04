package kr.java.aibe4_finalproject_team04_test.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 연관관계 매핑 없이 단순 ID 저장 (초보자 스타일)
    private int amount;  // 주문 금액
}

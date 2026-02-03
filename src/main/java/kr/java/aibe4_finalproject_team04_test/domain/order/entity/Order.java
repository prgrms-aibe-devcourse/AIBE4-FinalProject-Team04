package kr.java.aibe4_finalproject_team04_test.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter // ❌ 위반: Setter 지양. 변경 메서드(changeStatus 등)를 만들어야 함
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // ORDER, CANCEL

    // ❌ 실수: FetchType.LAZY 미명시 (OneToMany는 기본이 LAZY지만 명시하는 습관 권장, ManyToOne은 EAGER가 기본이라 위험)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 편의 메서드 누락: 양방향 연관관계 설정 시 실수하기 쉬움
}

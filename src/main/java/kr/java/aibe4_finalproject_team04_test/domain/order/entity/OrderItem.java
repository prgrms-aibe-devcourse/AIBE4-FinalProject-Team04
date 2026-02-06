package kr.java.aibe4_finalproject_team04_test.domain.order.entity;

import jakarta.persistence.*;
import kr.java.aibe4_finalproject_team04_test.domain.product.entity.Product;
import lombok.Data;

@Entity
@Data // ❌ 위반: 양방향 연관관계(Order <-> OrderItem)에서 toString() 무한 루프 발생 위험 높음
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ❌ 치명적 실수: @ManyToOne은 기본 FetchType이 EAGER(즉시 로딩)임.
    // Order를 조회할 때 이와 연관된 OrderItem을 다 가져오고,
    // 그 OrderItem과 연관된 Product까지 모조리 조회하는 쿼리가 나감 (N+1 문제의 주범).
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Product product;

    // ❌ 치명적 실수: 여기도 마찬가지로 EAGER 로딩.
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; // 주문 당시 가격
    private int count;      // 주문 수량
}

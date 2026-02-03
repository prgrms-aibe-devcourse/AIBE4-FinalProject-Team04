package kr.java.aibe4_finalproject_team04_test.domain.order.service;

import kr.java.aibe4_finalproject_team04_test.domain.order.dto.OrderRequestDto;
import kr.java.aibe4_finalproject_team04_test.domain.order.entity.Order;
import kr.java.aibe4_finalproject_team04_test.domain.order.entity.OrderStatus;
import kr.java.aibe4_finalproject_team04_test.domain.order.repository.OrderRepository;
import kr.java.aibe4_finalproject_team04_test.domain.product.entity.Product;
import kr.java.aibe4_finalproject_team04_test.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // ❌ 배드 패턴: OrderService가 ProductRepository를 직접 의존함.
    // 올바른 구조: ProductService를 주입받거나, Facade 패턴을 사용해야 함.
    private final ProductRepository productRepository;

    // 트랜잭션이 걸려있긴 하지만, 클래스 레벨 readOnly 설정이 없어 성능 최적화 안 됨
    @Transactional
    public Order createOrder(OrderRequestDto request) {

        // ❌ 위반: Product 도메인의 로직(조회 및 검증)을 Order 도메인이 직접 수행
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // ❌ 위반: 재고 부족 체크 로직이 ProductService가 아닌 이곳에 파편화됨
        if (product.getStockQuantity() < request.getCount()) {
            throw new RuntimeException("재고 부족");
        }

        // ❌ 위반: Product의 Setter를 직접 호출하여 상태 변경 (객체지향 위반)
        // 만약 ProductService에서 "재고 감소 시 로그 기록" 같은 로직이 추가되면 여기선 누락됨
        product.setStockQuantity(product.getStockQuantity() - request.getCount());

        // --- 주문 생성 로직 ---
        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.ORDER);

        // 단순히 연관관계만 맺고 끝남 (편의 메서드 부재)
        // OrderItem 생성 로직 등은 생략됨 (단순화를 위해)

        return orderRepository.save(order); // ❌ 위반: Entity 반환
    }
}

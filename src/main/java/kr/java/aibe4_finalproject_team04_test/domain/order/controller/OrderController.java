package kr.java.aibe4_finalproject_team04_test.domain.order.controller;

import kr.java.aibe4_finalproject_team04_test.domain.order.dto.OrderRequestDto;
import kr.java.aibe4_finalproject_team04_test.domain.order.entity.Order;
import kr.java.aibe4_finalproject_team04_test.domain.order.service.OrderService;
import kr.java.aibe4_finalproject_team04_test.domain.product.entity.Product;
import kr.java.aibe4_finalproject_team04_test.domain.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

    // ❌ 위반: Field Injection (@Autowired) 사용. 생성자 주입(@RequiredArgsConstructor) 권장.
    @Autowired
    private OrderService orderService;

    // ❌ 위반: Controller가 Repository를 직접 의존 (계층 위반)
    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/new")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequestDto request) {

        // ❌ 위반: 비즈니스 로직이 Controller에 존재 (재고 확인 및 감소)
        Product product = productRepository.findById(request.getProductId()).orElseThrow();

        if (product.getStockQuantity() < request.getCount()) {
            throw new RuntimeException("재고 부족");
        }

        // ❌ 위반: Setter를 사용하여 상태 변경 (도메인 주도 설계 위반)
        product.setStockQuantity(product.getStockQuantity() - request.getCount());
        productRepository.save(product); // 더티 체킹을 안 쓰고 명시적 호출 (트랜잭션 범위 밖일 수 있음)

        Order order = orderService.createOrder(request);

        // ❌ 위반: Entity(Order)를 Response로 직접 반환
        return ResponseEntity.ok(order);
    }
}

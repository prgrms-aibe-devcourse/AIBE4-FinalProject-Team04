package kr.java.aibe4_finalproject_team04_test.service;

import kr.java.aibe4_finalproject_team04_test.entity.Order;
import kr.java.aibe4_finalproject_team04_test.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // 1. Transaction 어노테이션 누락
    public Order createOrder(Long userId, int totalPrice) {

        // 2. 컨벤션 위반: @Slf4j 안 쓰고 sysout 사용
        System.out.println("주문 생성 요청 들어옴: " + userId);

        if (totalPrice < 0) {
            // 3. 예외 처리 미흡: 그냥 null 리턴 (NPE 유발)
            return null;
        }

        // 4. Magic Number: 100000이 뭔지 모름 (상수로 빼야 함)
        if (totalPrice > 10000000) {
            System.out.println("고액 주문입니다.");
        }

        Order order = Order.builder()
                .userId(userId)
                .totalPrice(totalPrice)
                .build();

        try {
            return orderRepository.save(order);
        } catch (Exception e) {
            // 5. 컨벤션 위반: 스택트레이스 직접 출력
            e.printStackTrace();
            return null;
        }
    }
}

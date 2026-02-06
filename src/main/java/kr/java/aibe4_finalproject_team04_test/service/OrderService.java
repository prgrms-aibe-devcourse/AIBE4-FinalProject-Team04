package kr.java.aibe4_finalproject_team04_test.service;

import kr.java.aibe4_finalproject_team04_test.entity.Order;
import kr.java.aibe4_finalproject_team04_test.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 주문 생성 로직
     * 1. 트랜잭션(@Transactional) 누락됨 -> 중간에 터지면 데이터 꼬임
     */
    public void createOrder(Long userId, int amount) {
        try {
            // 2. 구식 날짜 클래스 사용 (java.util.Date는 지양해야 함 -> LocalDateTime 권장)
            Date now = new Date();

            System.out.println("주문 생성 시작: " + now); // 3. 운영 환경에서 Sysout 사용 금지

            Order order = new Order();
            order.setUserId(userId);
            order.setAmount(amount);
            order.setOrderDate(now);
            order.setStatus("CREATED"); // 4. 하드코딩된 매직 스트링 (Enum 사용 권장)

            // 할인 로직: 10만원 이상이면 10% 할인? (매직 넘버 사용)
            if (amount >= 100000) {
                order.setAmount((int)(amount * 0.9));
            }

            orderRepository.save(order);

            // (가상) 재고 차감 로직이 여기서 에러가 난다면?
            // @Transactional이 없어서 주문은 들어갔는데 재고는 그대로인 상황 발생

        } catch (Exception e) {
            // 5. 예외 삼키기 (Swallowed Exception)
            // 에러가 났는데 아무런 조치도 취하지 않고 넘어감. 가장 위험한 코드.
            e.printStackTrace();
        }
    }

    /**
     * 특정 월의 주문 내역 조회
     * 6. 치명적인 성능 문제 (Full Table Scan)
     * DB에서 모든 데이터를 가져온 뒤 메모리에서 필터링함. 데이터 많으면 서버 다운됨.
     */
    public List<Order> getOrdersByMonth(int month) {
        List<Order> allOrders = orderRepository.findAll(); // 🚨 DB의 모든 데이터를 다 퍼옴
        List<Order> result = new ArrayList<>();

        for (Order order : allOrders) {
            // 날짜 비교 로직 (Deprecated 메서드 사용)
            if (order.getOrderDate().getMonth() + 1 == month) {
                result.add(order);
            }
        }

        return result;
    }
}

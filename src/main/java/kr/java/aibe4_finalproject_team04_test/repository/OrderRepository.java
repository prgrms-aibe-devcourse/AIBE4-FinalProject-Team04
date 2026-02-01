package kr.java.aibe4_finalproject_team04_test.repository;

import kr.java.aibe4_finalproject_team04_test.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 특정 유저의 주문 내역 조회
    List<Order> findByUserId(Long userId);
}

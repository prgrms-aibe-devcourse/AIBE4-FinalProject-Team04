package kr.java.aibe4_finalproject_team04_test.domain.order.repository;

import kr.java.aibe4_finalproject_team04_test.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}

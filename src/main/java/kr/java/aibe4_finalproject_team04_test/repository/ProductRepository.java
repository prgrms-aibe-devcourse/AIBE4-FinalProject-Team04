package kr.java.aibe4_finalproject_team04_test.repository;

import kr.java.aibe4_finalproject_team04_test.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 기본 메서드 사용
}

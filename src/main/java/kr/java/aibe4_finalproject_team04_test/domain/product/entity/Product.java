package kr.java.aibe4_finalproject_team04_test.domain.product.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data // ❌ 위반: @Getter만 사용해야 함 (JPA 엔티티에서 @Data는 지양)
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    // ❌ 위반: 기본 생성자 보호 누락 (public 기본 생성자 생성됨)
    // ❌ 위반: 비즈니스 로직 메서드 없이 setStockQuantity()를 외부에서 호출 가능하게 둠
}

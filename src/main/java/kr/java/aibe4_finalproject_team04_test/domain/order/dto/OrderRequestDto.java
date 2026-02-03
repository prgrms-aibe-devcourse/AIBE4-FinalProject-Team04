package kr.java.aibe4_finalproject_team04_test.domain.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequestDto {
    private Long productId;
    private int count;
}

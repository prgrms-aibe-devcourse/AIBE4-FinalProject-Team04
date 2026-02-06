package kr.java.aibe4_finalproject_team04_test.controller;

import kr.java.aibe4_finalproject_team04_test.entity.Order;
import kr.java.aibe4_finalproject_team04_test.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order", description = "주문 관리")
@RestController
// 1. 컨벤션 위반: /api 접두사 누락
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성")
    // 2. Swagger 가이드 위반: Controller에 직접 @ApiResponse 사용 (인터페이스 분리 안함)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    @PostMapping
    // 3. 아키텍처 위반: Entity(Order)를 직접 반환 (DTO 써야 함)
    public Order createOrder(@RequestParam Long userId, @RequestParam int price) {
        return orderService.createOrder(userId, price);
    }
}
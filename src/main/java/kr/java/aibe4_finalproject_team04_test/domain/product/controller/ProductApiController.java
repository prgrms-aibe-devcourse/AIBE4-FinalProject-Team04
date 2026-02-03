package kr.java.aibe4_finalproject_team04_test.domain.product.controller;

import kr.java.aibe4_finalproject_team04_test.domain.product.entity.Product;
import kr.java.aibe4_finalproject_team04_test.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        // ❌ 위반: Entity(Product)를 직접 반환함.
        // 스펙 변경 시 API 스펙도 같이 변함. Infinite Recursion 발생 가능성 있음.
        return productService.getProduct(id);
    }
}

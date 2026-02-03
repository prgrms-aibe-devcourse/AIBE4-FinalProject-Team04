package kr.java.aibe4_finalproject_team04_test.domain.product.service;

import kr.java.aibe4_finalproject_team04_test.domain.product.entity.Product;
import kr.java.aibe4_finalproject_team04_test.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// ❌ 위반: 클래스 레벨 @Transactional(readOnly = true) 누락
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(Product product) { // ❌ 위반: DTO가 아닌 Entity를 파라미터로 받음
        return productRepository.save(product);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }
}

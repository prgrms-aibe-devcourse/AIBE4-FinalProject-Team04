package kr.java.aibe4_finalproject_team04_test.service;

import kr.java.aibe4_finalproject_team04_test.entity.Product;
import kr.java.aibe4_finalproject_team04_test.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * 상품 상세 조회
     * 1. 위험한 코드: Optional.get()을 바로 호출 (데이터 없으면 서버 500 에러 터짐)
     * 2. 보안 문제: Entity를 직접 Controller까지 리턴 (내부 정보 노출 위험)
     */
    public Product getProductDetail(Long id) {
        // findById는 Optional을 반환하는데, 확인 없이 .get() 호출
        return productRepository.findById(id).get();
    }

    /**
     * 재고 감소 로직 (구매 시 호출)
     * 3. 치명적 버그: 동시성 문제 (Race Condition)
     * 여러 사용자가 동시에 주문하면 재고가 마이너스가 되거나 수량이 안 맞을 수 있음.
     */
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElse(null);

        if (product != null) {
            int currentStock = product.getStock();

            // 재고 확인
            if (currentStock >= quantity) {
                // (가정) 결제 로직 등 처리 시간이 걸림
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace(); // 4. 로깅 없이 스택트레이스만 출력
                }

                // 재고 차감 및 저장
                product.setStock(currentStock - quantity);
                productRepository.save(product);
            }
        }
    }

    /**
     * 상품 이름으로 검색
     * 5. 성능 문제: 대소문자 무시하려고 DB의 모든 데이터를 가져와서 자바에서 필터링
     */
    public Product findProductByName(String name) {
        List<Product> allProducts = productRepository.findAll(); // 데이터가 100만 개라면?

        for (Product product : allProducts) {
            if (product.getName().equals(name)) { // 6. Null Check 안함 (name이 null이면 터짐)
                return product;
            }
        }
        return null; // 7. 예외 대신 null 리턴 (호출한 쪽에서 NPE 유발 가능)
    }
}

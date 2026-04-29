package com.example.team3Project.domain.product.coupang.dao;

import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DummyCoupangProductRepository extends JpaRepository<DummyCoupangProduct, Long> {

    // 현재 로그인 사용자의 쿠팡 더미 상품 목록만 최신순으로 조회한다.
    // 쿠팡 더미 테이블에 있고 등록 상태도 REGISTERED인 상품만 판매 중 목록으로 조회한다.
    @Query("""
            select p
            from DummyCoupangProduct p
            join fetch p.registration r
            where p.userId = :userId
              and r.registrationStatus = com.example.team3Project.domain.product.registration.entity.RegistrationStatus.REGISTERED
            order by r.registeredAt desc, p.dummyCoupangProductId desc
            """)
    List<DummyCoupangProduct> findSellingProducts(@Param("userId") Long userId);

    // 상세 조회는 다른 사용자의 상품이 섞이지 않도록 userId까지 함께 건다.
    Optional<DummyCoupangProduct> findByDummyCoupangProductIdAndUserId(Long dummyCoupangProductId, Long userId);

    // 같은 사용자의 같은 원본 상품은 쿠팡 더미 상품 1건만 유지한다.
    Optional<DummyCoupangProduct> findByUserIdAndSourceProductId(Long userId, String sourceProductId);

    void deleteByUserIdAndSourceProductId(Long userId, String sourceProductId);

    void deleteByUserIdAndSourceProductIdIn(Long userId, List<String> sourceProductIds);

    @Query("""
            select p
            from DummyCoupangProduct p
            join p.registration r
            where p.userId = :userId
              and r.registrationStatus = com.example.team3Project.domain.product.registration.entity.RegistrationStatus.REGISTERED
              and (:fromDateTime is null or r.registeredAt >= :fromDateTime)
              and (:toDateTime is null or r.registeredAt < :toDateTime)
              and (:source is null or lower(p.sourceUrl) like lower(concat('%', :source, '%')))
              and (:keyword is null
                   or lower(p.productName) like lower(concat('%', :keyword, '%'))
                   or lower(p.sourceProductId) like lower(concat('%', :keyword, '%')))
              and (:minMarginRate is null
                   or (p.salePrice > 0 and (p.marginKrw * 100 / p.salePrice) >= :minMarginRate))
              and (:maxMarginRate is null
                   or (p.salePrice > 0 and (p.marginKrw * 100 / p.salePrice) <= :maxMarginRate))
            order by r.registeredAt desc, p.dummyCoupangProductId desc
            """)
    List<DummyCoupangProduct> searchRegisteredProducts(
            @Param("userId") Long userId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            @Param("source") String source,
            @Param("minMarginRate") BigDecimal minMarginRate,
            @Param("maxMarginRate") BigDecimal maxMarginRate,
            @Param("keyword") String keyword
    );
}

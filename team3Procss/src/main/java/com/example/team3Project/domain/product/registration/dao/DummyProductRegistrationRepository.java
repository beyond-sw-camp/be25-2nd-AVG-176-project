package com.example.team3Project.domain.product.registration.dao;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DummyProductRegistrationRepository extends JpaRepository<DummyProductRegistration, Long> {
    List<DummyProductRegistration> findByUserIdAndMarketCode(Long userId, MarketCode marketCode);

    // 단건 조회 시 다른 사용자의 등록 상품이 보이지 않도록 userId까지 조건으로 건다.
    Optional<DummyProductRegistration> findByDummyProductRegistrationIdAndUserId(Long registrationId, Long userId);

    // 선택 삭제에서는 현재 로그인 사용자의 소유 데이터만 한 번에 조회한다.
    List<DummyProductRegistration> findAllByDummyProductRegistrationIdInAndUserId(List<Long> registrationIds, Long userId);

    // 같은 사용자/마켓/원본 상품 조합은 한 건만 유지하고 재처리 시 갱신한다.
    Optional<DummyProductRegistration> findByUserIdAndMarketCodeAndSourceProductId(
            Long userId,
            MarketCode marketCode,
            String sourceProductId
    );

    @Query("""
            select r
            from DummyProductRegistration r
            where r.userId = :userId
              and r.registrationStatus = com.example.team3Project.domain.product.registration.entity.RegistrationStatus.READY
              and (:fromDateTime is null or r.receivedAt >= :fromDateTime)
              and (:toDateTime is null or r.receivedAt < :toDateTime)
              and (:source is null or lower(r.sourceUrl) like lower(concat('%', :source, '%')))
              and (:keyword is null
                   or lower(r.processedProductName) like lower(concat('%', :keyword, '%'))
                   or lower(r.sourceProductId) like lower(concat('%', :keyword, '%')))
              and (:minMarginRate is null
                   or (r.salePrice > 0 and (r.marginKrw * 100 / r.salePrice) >= :minMarginRate))
              and (:maxMarginRate is null
                   or (r.salePrice > 0 and (r.marginKrw * 100 / r.salePrice) <= :maxMarginRate))
            order by r.receivedAt desc, r.dummyProductRegistrationId desc
            """)
    List<DummyProductRegistration> searchReadyProducts(
            @Param("userId") Long userId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            @Param("source") String source,
            @Param("minMarginRate") java.math.BigDecimal minMarginRate,
            @Param("maxMarginRate") java.math.BigDecimal maxMarginRate,
            @Param("keyword") String keyword
    );

    @Query("""
            select r
            from DummyProductRegistration r
            where r.userId = :userId
              and r.registrationStatus = com.example.team3Project.domain.product.registration.entity.RegistrationStatus.REGISTERED
              and (:fromDateTime is null or r.registeredAt >= :fromDateTime)
              and (:toDateTime is null or r.registeredAt < :toDateTime)
              and (:source is null or lower(r.sourceUrl) like lower(concat('%', :source, '%')))
              and (:keyword is null
                   or lower(r.processedProductName) like lower(concat('%', :keyword, '%'))
                   or lower(r.sourceProductId) like lower(concat('%', :keyword, '%')))
              and (:minMarginRate is null
                   or (r.salePrice > 0 and (r.marginKrw * 100 / r.salePrice) >= :minMarginRate))
              and (:maxMarginRate is null
                   or (r.salePrice > 0 and (r.marginKrw * 100 / r.salePrice) <= :maxMarginRate))
            order by r.registeredAt desc, r.dummyProductRegistrationId desc
            """)
    List<DummyProductRegistration> searchRegisteredProducts(
            @Param("userId") Long userId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            @Param("source") String source,
            @Param("minMarginRate") java.math.BigDecimal minMarginRate,
            @Param("maxMarginRate") java.math.BigDecimal maxMarginRate,
            @Param("keyword") String keyword
    );

    // 취소 상품 목록 검색. 날짜는 소싱 유입일(receivedAt) 기준으로 필터링한다.
    @Query("""
            select r
            from DummyProductRegistration r
            where r.userId = :userId
              and r.registrationStatus = com.example.team3Project.domain.product.registration.entity.RegistrationStatus.CANCELED
              and (:fromDateTime is null or r.receivedAt >= :fromDateTime)
              and (:toDateTime is null or r.receivedAt < :toDateTime)
              and (:source is null or lower(r.sourceUrl) like lower(concat('%', :source, '%')))
              and (:keyword is null
                   or lower(r.processedProductName) like lower(concat('%', :keyword, '%'))
                   or lower(r.sourceProductId) like lower(concat('%', :keyword, '%')))
              and (:minMarginRate is null
                   or (r.salePrice > 0 and (r.marginKrw * 100 / r.salePrice) >= :minMarginRate))
              and (:maxMarginRate is null
                   or (r.salePrice > 0 and (r.marginKrw * 100 / r.salePrice) <= :maxMarginRate))
            order by r.receivedAt desc, r.dummyProductRegistrationId desc
            """)
    List<DummyProductRegistration> searchCanceledProducts(
            @Param("userId") Long userId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            @Param("source") String source,
            @Param("minMarginRate") java.math.BigDecimal minMarginRate,
            @Param("maxMarginRate") java.math.BigDecimal maxMarginRate,
            @Param("keyword") String keyword
    );

    // 사용자별 전체 등록 테이블 행 수를 집계한다. 날짜가 있으면 소싱 유입일 기준으로 제한한다.
    @Query("""
            select count(r)
            from DummyProductRegistration r
            where r.userId = :userId
              and (:fromDateTime is null or r.receivedAt >= :fromDateTime)
              and (:toDateTime is null or r.receivedAt < :toDateTime)
            """)
    long countByUserIdAndReceivedAtRange(
            @Param("userId") Long userId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    // 사용자별 등록 상태 개수를 집계한다. 날짜가 있으면 소싱 유입일 기준으로 제한한다.
    @Query("""
            select count(r)
            from DummyProductRegistration r
            where r.userId = :userId
              and r.registrationStatus = :registrationStatus
              and (:fromDateTime is null or r.receivedAt >= :fromDateTime)
              and (:toDateTime is null or r.receivedAt < :toDateTime)
            """)
    long countByUserIdAndRegistrationStatusAndReceivedAtRange(
            @Param("userId") Long userId,
            @Param("registrationStatus") RegistrationStatus registrationStatus,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    // 실패/취소처럼 여러 상태를 한 묶음으로 보여줘야 할 때 사용한다.
    @Query("""
            select count(r)
            from DummyProductRegistration r
            where r.userId = :userId
              and r.registrationStatus in :registrationStatuses
              and (:fromDateTime is null or r.receivedAt >= :fromDateTime)
              and (:toDateTime is null or r.receivedAt < :toDateTime)
            """)
    long countByUserIdAndRegistrationStatusInAndReceivedAtRange(
            @Param("userId") Long userId,
            @Param("registrationStatuses") List<RegistrationStatus> registrationStatuses,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );
}

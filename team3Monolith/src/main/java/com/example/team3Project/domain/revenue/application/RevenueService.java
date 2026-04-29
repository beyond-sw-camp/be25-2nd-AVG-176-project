package com.example.team3Project.domain.revenue.application;

import com.example.team3Project.domain.product.coupang.dao.DummyCoupangProductRepository;
import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProduct;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.revenue.dto.MarginSummaryResponse;
import com.example.team3Project.domain.revenue.dto.MarginSummaryResponse.MarginBreakdown;
import com.example.team3Project.domain.revenue.dto.ProductRevenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueService {

    private final DummyCoupangProductRepository coupangProductRepository;
    private final JdbcTemplate jdbcTemplate;

    // 예상/실제 마진 구성 합계 데이터를 반환한다 (도넛 차트 2개용).
    public MarginSummaryResponse getMarginSummary(Long userId) {
        List<DummyCoupangProduct> products = coupangProductRepository.findSellingProducts(userId);

        // 예상: 상품 1개당 1번 판매 기준
        MarginBreakdown expected = buildBreakdown(products, false);
        // 실제: order_count 만큼 판매 기준
        MarginBreakdown actual = buildBreakdown(products, true);

        return new MarginSummaryResponse(expected, actual);
    }

    // 상품별 마진 테이블 데이터를 반환한다.
    public List<ProductRevenueResponse> getProductRevenue(Long userId) {
        List<DummyCoupangProduct> products = coupangProductRepository.findSellingProducts(userId);
        return products.stream()
                .map(p -> toProductRevenueResponse(p, userId))
                .toList();
    }

    private MarginBreakdown buildBreakdown(List<DummyCoupangProduct> products, boolean useOrderCount) {
        BigDecimal totalSalePrice = BigDecimal.ZERO;
        BigDecimal totalProductCost = BigDecimal.ZERO;
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        BigDecimal totalShippingFee = BigDecimal.ZERO;
        BigDecimal totalMargin = BigDecimal.ZERO;

        for (DummyCoupangProduct p : products) {
            BigDecimal multiplier = useOrderCount
                    ? BigDecimal.valueOf(p.getOrderCount())
                    : BigDecimal.ONE;

            BigDecimal salePrice = nullToZero(p.getSalePrice()).multiply(multiplier);
            BigDecimal costInKrw = nullToZero(registrationCost(p)).multiply(multiplier);
            BigDecimal shippingFee = nullToZero(p.getShippingFee()).multiply(multiplier);
            BigDecimal margin = nullToZero(p.getMarginKrw()).multiply(multiplier);
            // 플랫폼 수수료 = 판매가 - 원가 - 배송비 - 마진으로 역산한다.
            BigDecimal platformFee = salePrice.subtract(costInKrw).subtract(shippingFee).subtract(margin);
            if (platformFee.compareTo(BigDecimal.ZERO) < 0) {
                platformFee = BigDecimal.ZERO;
            }

            totalSalePrice = totalSalePrice.add(salePrice);
            totalProductCost = totalProductCost.add(costInKrw);
            totalPlatformFee = totalPlatformFee.add(platformFee);
            totalShippingFee = totalShippingFee.add(shippingFee);
            totalMargin = totalMargin.add(margin);
        }

        BigDecimal marginRate = totalSalePrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalMargin.multiply(BigDecimal.valueOf(100))
                        .divide(totalSalePrice, 2, RoundingMode.HALF_UP);

        return new MarginBreakdown(
                totalSalePrice,
                totalProductCost,
                totalPlatformFee,
                totalShippingFee,
                totalMargin,
                marginRate
        );
    }

    private ProductRevenueResponse toProductRevenueResponse(DummyCoupangProduct p, Long userId) {
        DummyProductRegistration reg = p.getRegistration();
        BigDecimal salePrice = nullToZero(p.getSalePrice());
        BigDecimal margin = nullToZero(p.getMarginKrw());
        BigDecimal marginRate = salePrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : margin.multiply(BigDecimal.valueOf(100))
                        .divide(salePrice, 2, RoundingMode.HALF_UP);

        int reviewCount = fetchReviewCount(userId, p.getSourceProductId());

        return new ProductRevenueResponse(
                p.getDummyCoupangProductId(),
                p.getProductName(),
                salePrice,
                margin,
                marginRate,
                reg != null ? reg.getRegisteredAt() : null,
                p.getOrderCount(),
                reviewCount
        );
    }

    // sourcing_variation 테이블에서 해당 상품의 리뷰 수 합계를 조회한다.
    private int fetchReviewCount(Long userId, String sourceProductId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(sv.reviews_count), 0)" +
                    " FROM sourcing s" +
                    " JOIN sourcing_variation sv ON sv.sourcing_id = s.id" +
                    " WHERE s.user_id = ? AND s.product_id = ?",
                    Integer.class, userId, sourceProductId
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            // 컬럼이 없거나 조회 불가 시 0 반환
            return 0;
        }
    }

    // 쿠팡 상품의 매입원가는 연결된 등록 데이터에서 가져온다.
    private BigDecimal registrationCost(DummyCoupangProduct p) {
        DummyProductRegistration reg = p.getRegistration();
        return reg != null ? reg.getCostInKrw() : BigDecimal.ZERO;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

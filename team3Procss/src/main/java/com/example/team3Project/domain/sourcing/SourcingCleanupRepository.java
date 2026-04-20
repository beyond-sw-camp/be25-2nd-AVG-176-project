package com.example.team3Project.domain.sourcing;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * sourcing 관련 테이블은 JPA 엔티티가 없으므로 JdbcTemplate으로 직접 삭제한다.
 *
 * 테이블 FK 관계 (삭제 순서 역순):
 *   sourcing_variation_dimensions  →  sourcing_variation  →  sourcing
 *   sourcing_variation_images      →  sourcing_variation
 *   sourcing_description_images                           →  sourcing
 */
@Repository
@RequiredArgsConstructor
public class SourcingCleanupRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 특정 사용자의 단일 상품(source_product_id = sourcing.product_id)에 해당하는
     * sourcing 관련 레코드를 전부 삭제한다.
     */
    public void deleteByUserIdAndProductId(Long userId, String productId) {
        if (userId == null || productId == null) return;

        // 삭제 대상 sourcing ID 조회
        List<Long> sourcingIds = jdbcTemplate.queryForList(
                "SELECT id FROM sourcing WHERE user_id = ? AND product_id = ?",
                Long.class, userId, productId
        );
        if (sourcingIds.isEmpty()) return;

        deleteByIds(sourcingIds);
    }

    /**
     * 특정 사용자의 여러 상품에 해당하는 sourcing 관련 레코드를 한 번에 삭제한다.
     */
    public void deleteByUserIdAndProductIds(Long userId, List<String> productIds) {
        if (userId == null || productIds == null || productIds.isEmpty()) return;

        String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
        Object[] params = new Object[productIds.size() + 1];
        params[0] = userId;
        for (int i = 0; i < productIds.size(); i++) {
            params[i + 1] = productIds.get(i);
        }

        List<Long> sourcingIds = jdbcTemplate.queryForList(
                "SELECT id FROM sourcing WHERE user_id = ? AND product_id IN (" + placeholders + ")",
                Long.class, params
        );
        if (sourcingIds.isEmpty()) return;

        deleteByIds(sourcingIds);
    }

    // sourcing ID 목록을 기준으로 연관 레코드를 FK 역순으로 삭제한다.
    private void deleteByIds(List<Long> sourcingIds) {
        String idPlaceholders = String.join(",", Collections.nCopies(sourcingIds.size(), "?"));
        Object[] idParams = sourcingIds.toArray();

        // variation ID 조회 (자식 테이블 삭제에 필요)
        List<Long> variationIds = jdbcTemplate.queryForList(
                "SELECT id FROM sourcing_variation WHERE sourcing_id IN (" + idPlaceholders + ")",
                Long.class, idParams
        );

        if (!variationIds.isEmpty()) {
            String varPlaceholders = String.join(",", Collections.nCopies(variationIds.size(), "?"));
            Object[] varParams = variationIds.toArray();

            // sourcing_variation_dimensions 삭제
            jdbcTemplate.update(
                    "DELETE FROM sourcing_variation_dimensions WHERE variation_id IN (" + varPlaceholders + ")",
                    varParams
            );
            // sourcing_variation_images 삭제
            jdbcTemplate.update(
                    "DELETE FROM sourcing_variation_images WHERE variation_id IN (" + varPlaceholders + ")",
                    varParams
            );
            // sourcing_variation 삭제
            jdbcTemplate.update(
                    "DELETE FROM sourcing_variation WHERE sourcing_id IN (" + idPlaceholders + ")",
                    idParams
            );
        }

        // sourcing_description_images 삭제
        jdbcTemplate.update(
                "DELETE FROM sourcing_description_images WHERE sourcing_id IN (" + idPlaceholders + ")",
                idParams
        );

        // sourcing 삭제
        jdbcTemplate.update(
                "DELETE FROM sourcing WHERE id IN (" + idPlaceholders + ")",
                idParams
        );
    }
}

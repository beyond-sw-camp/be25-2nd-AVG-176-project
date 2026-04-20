package com.example.team3Project.domain.revenue.api;

import com.example.team3Project.domain.revenue.application.RevenueService;
import com.example.team3Project.domain.revenue.dto.MarginSummaryResponse;
import com.example.team3Project.domain.revenue.dto.ProductRevenueResponse;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    // 도넛 차트용 예상/실제 마진 구성 합계 조회
    // LoginUserArgumentResolver가 X-User-Id 헤더를 기준으로 현재 사용자를 해석한다.
    @GetMapping("/margin/summary")
    public ResponseEntity<MarginSummaryResponse> getMarginSummary(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(revenueService.getMarginSummary(user.getId()));
    }

    // 상품별 마진 테이블 데이터 조회
    @GetMapping("/products")
    public ResponseEntity<List<ProductRevenueResponse>> getProductRevenue(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(revenueService.getProductRevenue(user.getId()));
    }
}

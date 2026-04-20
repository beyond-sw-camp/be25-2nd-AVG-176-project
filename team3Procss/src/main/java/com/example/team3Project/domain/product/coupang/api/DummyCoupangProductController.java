package com.example.team3Project.domain.product.coupang.api;

import com.example.team3Project.domain.product.coupang.application.DummyCoupangProductService;
import com.example.team3Project.domain.product.coupang.dto.DummyCoupangPublishRequest;
import com.example.team3Project.domain.product.coupang.dto.DummyCoupangProductResponse;
import com.example.team3Project.domain.product.coupang.entity.DummyCoupangProduct;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class DummyCoupangProductController {

    // 쿠팡 더미 마켓 발행과 조회는 모두 현재 로그인 사용자를 기준으로 처리한다.
    private final DummyCoupangProductService dummyCoupangProductService;

    // 등록 후보 1건을 쿠팡 더미 마켓 상품으로 발행한다.
    @PostMapping("/products/registrations/{registrationId}/publish/coupang")
    public ResponseEntity<DummyCoupangProduct> publish(
            @LoginUser User user,
            @PathVariable Long registrationId,
            @RequestParam(required = false, defaultValue = "true") boolean manual,
            @RequestParam(required = false, defaultValue = "true") boolean ignoreAutoPublish
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        DummyCoupangProduct product = dummyCoupangProductService.publishManually(user.getId(), registrationId);
        return ResponseEntity.ok(product);
    }

    // 체크박스로 선택한 등록 후보 여러 건을 한 번에 쿠팡 더미 마켓으로 발행한다.
    @PostMapping("/products/registrations/publish/coupang")
    public ResponseEntity<List<DummyCoupangProduct>> publishAll(
            @LoginUser User user,
            @RequestParam(required = false, defaultValue = "true") boolean manual,
            @RequestParam(required = false, defaultValue = "true") boolean ignoreAutoPublish,
            @Valid @RequestBody DummyCoupangPublishRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<DummyCoupangProduct> products =
                dummyCoupangProductService.publishAllManually(user.getId(), request.getRegistrationIds());
        return ResponseEntity.ok(products);
    }

    // 현재 로그인 사용자의 쿠팡 더미 상품 목록을 조회한다.
    @GetMapping("/products/coupang")
    public ResponseEntity<List<DummyCoupangProductResponse>> getProducts(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(dummyCoupangProductService.getProducts(user.getId()));
    }

    // 현재 로그인 사용자의 쿠팡 더미 상품 상세를 조회한다.
    @GetMapping("/products/coupang/search")
    public ResponseEntity<List<DummyCoupangProductResponse>> searchProducts(
            @LoginUser User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) BigDecimal minMarginRate,
            @RequestParam(required = false) BigDecimal maxMarginRate,
            @RequestParam(required = false) String keyword
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<DummyCoupangProductResponse> products = dummyCoupangProductService.searchRegisteredProducts(
                user.getId(),
                from,
                to,
                source,
                minMarginRate,
                maxMarginRate,
                keyword
        );

        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/coupang/{dummyCoupangProductId}")
    public ResponseEntity<DummyCoupangProduct> getProduct(
            @LoginUser User user,
            @PathVariable Long dummyCoupangProductId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        DummyCoupangProduct product =
                dummyCoupangProductService.getProduct(user.getId(), dummyCoupangProductId);
        return ResponseEntity.ok(product);
    }
}
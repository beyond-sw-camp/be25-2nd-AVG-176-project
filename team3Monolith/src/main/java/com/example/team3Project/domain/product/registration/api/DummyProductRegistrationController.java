package com.example.team3Project.domain.product.registration.api;

import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.product.registration.application.ProductRegistrationService;
import com.example.team3Project.domain.product.registration.dto.DummyProductRegistrationDeleteRequest;
import com.example.team3Project.domain.product.registration.dto.DummyProductRegistrationStatusCountsResponse;
import com.example.team3Project.domain.product.registration.entity.DummyProductRegistration;
import com.example.team3Project.domain.product.registration.entity.RegistrationStatus;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("products/registrations")
@RequiredArgsConstructor
public class DummyProductRegistrationController {

    // 등록 상품 조회/삭제 API도 현재 로그인 사용자의 데이터만 대상으로 한다.
    private final ProductRegistrationService productRegistrationService;

    // 등록 상품 목록을 로그인 사용자와 마켓 코드 기준으로 조회한다.
    @GetMapping
    public ResponseEntity<List<DummyProductRegistration>> getRegistrations(
            @LoginUser User user,
            @RequestParam MarketCode marketCode
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<DummyProductRegistration> registrations =
                productRegistrationService.getRegistrations(user.getId(), marketCode);

        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/ready/search")
    public ResponseEntity<List<DummyProductRegistration>> searchReadyProducts(
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

        List<DummyProductRegistration> registrations = productRegistrationService.searchReadyProducts(
                user.getId(),
                from,
                to,
                source,
                minMarginRate,
                maxMarginRate,
                keyword
        );

        return ResponseEntity.ok(registrations);
    }

    // 취소된 등록 상품 목록을 로그인 사용자 기준으로 검색한다.
    @GetMapping("/canceled/search")
    public ResponseEntity<List<DummyProductRegistration>> searchCanceledProducts(
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

        List<DummyProductRegistration> registrations = productRegistrationService.searchCanceledProducts(
                user.getId(),
                from,
                to,
                source,
                minMarginRate,
                maxMarginRate,
                keyword
        );

        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/registered/search")
    public ResponseEntity<List<DummyProductRegistration>> searchRegisteredProducts(
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

        List<DummyProductRegistration> registrations = productRegistrationService.searchRegisteredProducts(
                user.getId(),
                from,
                to,
                source,
                minMarginRate,
                maxMarginRate,
                keyword
        );

        return ResponseEntity.ok(registrations);
    }

    // 등록 대기/완료/실패 또는 취소 건수를 현재 로그인 사용자 기준으로 반환한다.
    @GetMapping("/status-counts")
    public ResponseEntity<DummyProductRegistrationStatusCountsResponse> getStatusCounts(
            @LoginUser User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        DummyProductRegistrationStatusCountsResponse response =
                productRegistrationService.getStatusCounts(user.getId(), from, to);

        return ResponseEntity.ok(response);
    }

    // 등록 상품 단건을 로그인 사용자 소유 기준으로 조회한다.
    @GetMapping("/{registrationId}")
    public ResponseEntity<DummyProductRegistration> getRegistration(
            @LoginUser User user,
            @PathVariable Long registrationId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        DummyProductRegistration registration =
                productRegistrationService.getRegistration(user.getId(), registrationId);

        return ResponseEntity.ok(registration);
    }

    @PatchMapping("/{registrationId}/status")
    public ResponseEntity<DummyProductRegistration> updateRegistrationStatus(
            @LoginUser User user,
            @PathVariable Long registrationId,
            @RequestParam RegistrationStatus status,
            @RequestParam(required = false) String reason
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        DummyProductRegistration registration = productRegistrationService.updateRegistrationStatus(
                user.getId(),
                registrationId,
                status,
                reason
        );

        return ResponseEntity.ok(registration);
    }

    // 등록 상품 1건을 로그인 사용자 소유 기준으로 삭제한다.
    @DeleteMapping("/{registrationId}")
    public ResponseEntity<Void> deleteRegistration(
            @LoginUser User user,
            @PathVariable Long registrationId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        productRegistrationService.deleteRegistration(user.getId(), registrationId);
        return ResponseEntity.noContent().build();
    }

    // 체크박스로 선택한 등록 상품 여러 건을 한 번에 삭제한다.
    @DeleteMapping
    public ResponseEntity<Void> deleteRegistrations(
            @LoginUser User user,
            @Valid @RequestBody DummyProductRegistrationDeleteRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        productRegistrationService.deleteRegistrations(user.getId(), request.getRegistrationIds());
        return ResponseEntity.noContent().build();
    }
}

package com.example.team3Project.domain.policy.api;

import com.example.team3Project.domain.policy.application.PolicySettingService;
import com.example.team3Project.domain.policy.dto.PolicySettingResponse;
import com.example.team3Project.domain.policy.dto.PolicySettingUpsertRequest;
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("policies/settings")
@RequiredArgsConstructor
public class PolicySettingController {

    // 정책 설정 CRUD는 로그인 사용자 기준으로만 접근한다.
    private final PolicySettingService policySettingService;

    @PutMapping
    public ResponseEntity<PolicySettingResponse> upsertPolicySetting(
            @LoginUser User user,
            @RequestParam MarketCode marketCode,
            @Valid @RequestBody PolicySettingUpsertRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        PolicySettingResponse response =
                policySettingService.upsertPolicySetting(user.getId(), marketCode, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PolicySettingResponse> getPolicySetting(
            @LoginUser User user,
            @RequestParam MarketCode marketCode
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        PolicySettingResponse response = policySettingService.getPolicySetting(user.getId(), marketCode);
        return ResponseEntity.ok(response);
    }
}

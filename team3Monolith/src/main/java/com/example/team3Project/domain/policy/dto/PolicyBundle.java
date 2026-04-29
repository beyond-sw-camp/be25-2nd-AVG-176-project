package com.example.team3Project.domain.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter // 가공, 소싱 서비스에서 get
@AllArgsConstructor
public class PolicyBundle {
    private final PolicySettingResponse policySettingResponse;   // 마진율 관련 정책

    private final List<BlockedWordResponse> blockedWords;  // 금지어 관련 정책

    private final List<ReplacementWordResponse> replacementWords;  // 치환어 관련 정책

}

package com.example.team3Project.domain.policy.application;

import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.policy.dto.PolicyBundle;
import com.example.team3Project.domain.policy.dto.PolicySettingResponse;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
import com.example.team3Project.domain.policy.entity.MarketCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service  // 서비스 계층
@RequiredArgsConstructor  // 생성자 주입
@Transactional(readOnly = true)  // 조회 메서드를 읽기 전용 트랜잭션으로 처리한다.
@Slf4j
public class PolicyQueryService {
    private final PolicySettingService policySettingService;   // 정책 설정 조회용 서비스
    private final BlockedWordService blockedWordService;    // 금지어 조회용 서비스
    private final ReplacementWordService replacementWordService;    // 치환어 조회용 서비스

    // 정책 설정 번들을 가져오는 메소드
    public PolicyBundle getPolicyBundle(Long userId, MarketCode marketCode){
        PolicySettingResponse policySetting = policySettingService.getPolicySetting(userId, marketCode); // 마켓별 정책 설정 가져오기
        List<BlockedWordResponse> blockedWords = blockedWordService.getBlockedWords(userId); // 사용자의 금지어 목록 가져오기
        List<ReplacementWordResponse> replacementWords = replacementWordService.getReplacementWords(userId); // 사용자의 치환어 목록 가져오기

        // 반환값 전체가 캐시에 저장됨
        return new PolicyBundle(
          policySetting,
          blockedWords,
          replacementWords
        );
    }
}

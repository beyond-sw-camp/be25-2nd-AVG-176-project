package com.example.team3Project.domain.policy.dao;

// 이 Repository가 어떤 엔티티를 다룰지 import
import com.example.team3Project.domain.policy.entity.MarketCode;
import com.example.team3Project.domain.policy.entity.UserPolicySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional; // 조회 결과가 없을 때에 사용 - null 대신 Optional().empty()로 사용가능
// 엔티티를 DB에 저장하고 조회하는 통로 - dao(data access object)
public interface UserPolicySettingRepository extends JpaRepository<UserPolicySetting, Long> {
    // 정책 상세 조회
    Optional<UserPolicySetting> findByUserIdAndMarketCode(Long userId, MarketCode marketCode);

    // 사용자별 전체 마켓 정책 목록 조회(실제 사용 X - 확장성 고려)
    List<UserPolicySetting> findAllByUserId(Long userId);
}
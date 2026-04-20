package com.example.team3Project.domain.policy.dao;

import com.example.team3Project.domain.policy.entity.UserBlockedWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 이 repository가 다룰 엔터티 : UserBlockedWord, 엔터티 PK : Long 타입
public interface UserBlockedWordRepository extends JpaRepository<UserBlockedWord, Long> {
    // 특정 사용자의 금지어 목록 전체를 조회하는 메서드
    List<UserBlockedWord> findAllByUserId(Long userId);

    /*
    Optional<T> : 값이 있을 수도 있고 없을 수도 있는 상황을 표현하는 객체
      - DB 조회 시 null을 get하면 NullPointException이 발생한다.
      - Optional은 해당 상황을 방지하여 존재 여부를 확인할 수 있도록 한다.
      -
    */
    // 특정 사용자가 특정 금지어를 이미 등록했는지 확인하는 메서드
    // findByUserIdAndBlockedWord를 했을 때 결과가 있으면 UserBlockedWord로 반환
    // 없으면 Optional.empty()로 반환
    Optional<UserBlockedWord> findByUserIdAndBlockedWord(Long userId, String blockedWord);

}

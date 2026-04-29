package com.example.team3Project.domain.policy.dao;

import com.example.team3Project.domain.policy.entity.UserReplacementWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 해당 레포지토리에서는 UserReplacementWord엔티티를 다룰 것이고 해당 엔터티의 PK 타입은 Long이다.
// 레포지토리를 interface로 만드는 이유 :
// 구현 클래스를 직접 만들지 않아도 Spring Data가 런타임에 구현체를 만들어주기 때문이다.
public interface UserReplacementWordRepository extends JpaRepository<UserReplacementWord, Long> {

    // 특정 사용자의 치환어 목록 전체를 조회하는 메서드
    List<UserReplacementWord> findAllByUserId(Long userId);

    // 특정 사용자가 특정 원본 단어를 이미 등록했는지 확인하는 메서드
    Optional<UserReplacementWord> findByUserIdAndSourceWord(Long userId, String sourceWord);
}

package com.example.team3Project.domain.policy.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // 사용자별 금지어 1건를 담는 엔터티
@Table(name = "user_blocked_word",  // 테이블명
        // 제약조건 설정 - 중복되는 (사용자 id, 금지어) 쌍이 없도록 함
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_blocked_word_user_id_word", columnNames = {"user_id", "blocked_word"})
        })
@Getter
@NoArgsConstructor
public class UserBlockedWord {
    // 해당 테이블의 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_blocked_word_id")
    private Long UserBlockedWordId;

    // 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 금지어 문자열
    // @Column(name = "blocked_word", nullable = false, length = 100) - 길이 제한 원하면 설정
    @Column(name = "blocked_word", nullable = false)
    private String blockedWord;

    // 새 금지어 엔티티 인스턴스 생성 메서드
    public static UserBlockedWord create(Long userId, String blockedWord){
        UserBlockedWord userBlockedWord = new UserBlockedWord();    // 새 인스턴스 생성
        userBlockedWord.userId = userId;
        userBlockedWord.blockedWord = blockedWord;
        return userBlockedWord;
    }

    // 금지어 수정 메서드
    public void update(String blockedWord){
        this.blockedWord = blockedWord;
    }
}

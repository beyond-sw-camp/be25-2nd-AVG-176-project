package com.example.team3Project.domain.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_replacement_word",
       // 같은 원본 단어에 대해서 중복 등록을 방지하는 제어 조건
       uniqueConstraints = {
               @UniqueConstraint(
                       name = "uk_user_replacement_word_user_id_source_word",
                       columnNames = {"user_id", "source_word"}
               )
       }
)

@Getter
@NoArgsConstructor
// 해당 클래스는 Jpa 레포지토리의 키값으로 들어간다.
public class UserReplacementWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_replacement_id", nullable = false)
    private Long userReplacementWordId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="source_word", nullable = false)
    private String sourceWord;

    @Column(name="replacement_word", nullable = false)
    private String replacementWord;

    // 치환어를 생성 시 해당 메서드를 호출 하여 함께 받은 argument를 넣어 엔터티 인스턴스를 생성 및 반환한다.
    public static UserReplacementWord create(Long userId, String sourceWord, String replacementWord){
        UserReplacementWord userReplacementWord = new UserReplacementWord();
        userReplacementWord.userId = userId;
        userReplacementWord.sourceWord = sourceWord;    // 원본 단어
        userReplacementWord.replacementWord = replacementWord;  // 치환어
        return userReplacementWord;
    }

    // 치환어를 수정할 때에 해당 메서드를 호출하여 함께 받은 argument에 값을 다시 넣어준다.
    public void update(String sourceWord, String replacementWord){
        this.sourceWord = sourceWord;
        this.replacementWord = replacementWord;
    }
}

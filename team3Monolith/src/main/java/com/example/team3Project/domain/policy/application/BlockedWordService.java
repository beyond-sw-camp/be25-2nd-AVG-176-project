package com.example.team3Project.domain.policy.application;

import com.example.team3Project.domain.policy.dao.UserBlockedWordRepository;
import com.example.team3Project.domain.policy.dto.BlockedWordCreateRequest;
import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.policy.entity.UserBlockedWord;
import com.example.team3Project.domain.policy.exception.BlockedWordAlreadyExistsException;
import com.example.team3Project.domain.policy.exception.BlockedWordNotFoundException;
import com.example.team3Project.domain.policy.dto.BlockedWordUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional  // DB 작업을 하나의 트랜잭션 안에서 처리하도록 한다. 쓰기, 조회 작업
public class BlockedWordService {
    // 해당 Repository를 통해 DB에 저장, 조회한다.
    private final UserBlockedWordRepository userBlockedWordRepository;

    // 금지어 1건을 등록하는 메서드 (2가지 기능)
    // - DB에 금지어를 저장
    // - 저장 결과를 응답 형태로 돌려줌 : 보통 등록 직후 화면에서 바로 보여주거나 응답으로 확인하는 경우가 많음
    // - (사용자 ID, 금지어) 쌍으로 검색했을 때에 조회 결과가 이미 존재하면 BlockedWordAlreadyExistsException을 던진다.
    @CacheEvict(value = "policyBundle", key = "#userId")    // policyBundle 캐시에서 현재 사용자 userId에 해당하는 캐시 엔트리만 현재 메서드가 성공한 뒤 제거한다.
    public BlockedWordResponse createBlockedWord(Long userId, BlockedWordCreateRequest request) {
        userBlockedWordRepository.findByUserIdAndBlockedWord(userId, request.getBlockedWord())
                .ifPresent(blockedWord -> {
                    throw new BlockedWordAlreadyExistsException("이미 등록된 금지어입니다.");
                });

        // 새로운 엔터티 인스턴스를 만들어서 담아 놓음
        UserBlockedWord blockedWord = UserBlockedWord.create(userId, request.getBlockedWord());
        // 담아 놓은 엔터티 인스턴스를 DB에 저장
        UserBlockedWord saved = userBlockedWordRepository.save(blockedWord);
        /*
            CrudRepository.save(entity)
            - 주어진 엔터티를 저장한다.
            - 새 엔터티는 저장, 기존 엔터티는 병합(업데이트)한다.
            - @Version 필드 등을 보고 기존 엔터티인지 확인한다.
        */

        // 응답 DTO로 바꿔서 반환한다. - 해당 결과를 클라이언트에게 보낸다.
        return new BlockedWordResponse(
                saved.getUserBlockedWordId(),
                saved.getUserId(),
                saved.getBlockedWord()
        );
    }

    // 특정 사용자의 금지어 목록 전체를 조회하는 메서드
    @Transactional(readOnly = true)   // 읽기 전용 트랜잭션
    public List<BlockedWordResponse> getBlockedWords(Long userId) {
        // 특정 사용자의 금지어 목록 가져오기 - List 반환형 : 금지어가 여러 개일 수 있음
        /*
            findAllByUserId()
            - 파생 쿼리 메서드 : 메서드 이름으로부터 쿼리를 유도한다.
            - find...By : 조회 메서드
            - UserId : 엔터티의 필드명 userId 기준 조회
            - findby + 필드명 패턴을 지원한다.
        */
        return userBlockedWordRepository.findAllByUserId(userId)
                .stream()
                .map(blockedWord -> new BlockedWordResponse(
                        blockedWord.getUserBlockedWordId(),
                        blockedWord.getUserId(),
                        blockedWord.getBlockedWord()
                )).toList();
    }

    // 금지어를 삭제하는 메서드
    @CacheEvict(value = "policyBundle", key = "#userId")    // policyBundle 캐시에서 현재 사용자 userId에 해당하는 캐시 엔트리만 현재 메서드가 성공한 뒤 제거한다.
    public void deleteBlockedWord(Long userId, Long userBlockedWordId) {
        // 금지어 ID로 조회 후 없으면 예외 던짐
        UserBlockedWord blockedWord = userBlockedWordRepository.findById(userBlockedWordId)
                .orElseThrow(() -> new BlockedWordNotFoundException("금지어가 존재하지 않습니다."));

        // 금지어 쌍과 사용자 ID가 일치하지 않으면 예외 던짐
        if (!blockedWord.getUserId().equals(userId)) {
            throw new BlockedWordNotFoundException("금지어가 존재하지 않습니다.");
        }

        // 해당 금지어를 삭제한다.
        userBlockedWordRepository.delete(blockedWord);
    }

    // 금지어를 수정하는 메서드
    // request에서 금지어의 ID와 수정할 금지어 문자열이 들어온다.
    @CacheEvict(value = "policyBundle", key = "#userId")    // policyBundle 캐시에서 현재 사용자 userId에 해당하는 캐시 엔트리만 현재 메서드가 성공한 뒤 제거한다.
    public BlockedWordResponse updateBlockedWord(
            Long userId, Long userBlockedWordId, BlockedWordUpdateRequest request
    ) {
        // 수정할 금지어 존재 여부 확인
        UserBlockedWord blockedWord = userBlockedWordRepository.findById(userBlockedWordId)
                .orElseThrow(() -> new BlockedWordNotFoundException("금지어가 존재하지 않습니다."));

        // 수정할 금지어의 사용자ID와 현재 사용자ID가 일치하는지 확인
        if (!blockedWord.getUserId().equals(userId)) {
            throw new BlockedWordNotFoundException("금지어가 존재하지 않습니다.");
        }

        // 현재 사용자 ID와 금지어 문자열의 수정값을 받음
        userBlockedWordRepository.findByUserIdAndBlockedWord(userId, request.getBlockedWord())
                .ifPresent(existingBlockedWord -> {
                    if (!existingBlockedWord.getUserBlockedWordId().equals(userBlockedWordId)) {
                        throw new BlockedWordAlreadyExistsException("이미 등록된 금지어입니다.");
                    }
                });
        // 금지어 문자열 수정
        blockedWord.update(request.getBlockedWord());

        return new BlockedWordResponse(
                blockedWord.getUserBlockedWordId(),
                blockedWord.getUserId(),
                blockedWord.getBlockedWord()
        );
    }
}

package com.example.team3Project.domain.policy.application;

import com.example.team3Project.domain.policy.dao.UserReplacementWordRepository;
import com.example.team3Project.domain.policy.dto.ReplacementWordCreateRequest;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
import com.example.team3Project.domain.policy.dto.ReplacementWordUpdateRequest;
import com.example.team3Project.domain.policy.entity.UserReplacementWord;
import com.example.team3Project.domain.policy.exception.ReplacementWordAlreadyExistsException;
import com.example.team3Project.domain.policy.exception.ReplacementWordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReplacementWordService {
    private final UserReplacementWordRepository userReplacementWordRepository;

    // 치환어 1건을 등록하는 메서드
    @CacheEvict(value = "policyBundle", key = "#userId")    // policyBundle 캐시에서 현재 사용자 userId에 해당하는 캐시 엔트리만 현재 메서드가 성공한 뒤 제거한다.
    public ReplacementWordResponse createReplacementWord(Long userId, ReplacementWordCreateRequest request) {
        // (사용자 ID, 원본단어) 쌍으로 조회하여 이미 해당 원본단어에 대해 치환어가 있는지 확인
        userReplacementWordRepository.findByUserIdAndSourceWord(userId, request.getSourceWord())
                .ifPresent(replacementWord -> {
                    throw new ReplacementWordAlreadyExistsException("이미 등록된 치환어입니다.");
                });

        // 해당 치환 정책이 존재하지 않으면 요청으로 받은 객체를 담은 엔터티 인스턴스 생성
        UserReplacementWord userReplacementWord = UserReplacementWord.create(
                userId,
                request.getSourceWord(),
                request.getReplacementWord()
        );

        // 해당 인스턴스 객체를 DB에 저장함
        UserReplacementWord saved = userReplacementWordRepository.save(userReplacementWord);

        return new ReplacementWordResponse(
                saved.getUserReplacementWordId(),
                saved.getUserId(),
                saved.getSourceWord(),
                saved.getReplacementWord()
        );
    }

    // 특정 사용자의 치환어 목록 전체를 조회하는 메서드(일기 전용)
    @Transactional(readOnly = true)
    public List<ReplacementWordResponse> getReplacementWords(Long userId){
        return userReplacementWordRepository.findAllByUserId(userId)
                .stream()
                .map(replacementWord -> new ReplacementWordResponse(
                        replacementWord.getUserReplacementWordId(),
                        replacementWord.getUserId(),
                        replacementWord.getSourceWord(),
                        replacementWord.getReplacementWord()
                ))
                .toList();
    }

    // 특정 사용자의 치환어 1건을 삭제하는 메서드
    @CacheEvict(value = "policyBundle", key = "#userId")    // policyBundle 캐시에서 현재 사용자 userId에 해당하는 캐시 엔트리만 현재 메서드가 성공한 뒤 제거한다.
    public void deleteReplacementWord(Long userId, Long userReplacementWordId){
        // 치환어 ID를 바탕으로 DB에서 치환어 Entity를 가져온다.
        UserReplacementWord replacementWord = userReplacementWordRepository.findById(userReplacementWordId)
                // 해당 치환어가 존재하지 않으면 예외를 발생시킨다.
                .orElseThrow(() -> new ReplacementWordNotFoundException("치환어가 존재하지 않습니다."));

        // 치환어가 존재해도 사용자ID의 치환어가 아닌 경우
        if (!replacementWord.getUserId().equals(userId)){
            throw new ReplacementWordNotFoundException("치환어가 존재하지 않습니다..");
        }

        // 치환어 DB에 접근하여 해당 치환어를 삭제한다.
        userReplacementWordRepository.delete(replacementWord);
    }

    // 사용자의 치환어 1건을 수정하는 메서드
    @CacheEvict(value = "policyBundle", key = "#userId")    // policyBundle 캐시에서 현재 사용자 userId에 해당하는 캐시 엔트리만 현재 메서드가 성공한 뒤 제거한다.
    public ReplacementWordResponse updateReplacementWord(
            Long userId,
            Long userReplacementWordId,
            ReplacementWordUpdateRequest request
    ) {
        // DB에서 치환어 PK로 수정할 대상을 조회한다.
        UserReplacementWord replacementWord = userReplacementWordRepository.findById(userReplacementWordId)
                // ID에 해당하는 값이 없으면 예외를 던진다.
                .orElseThrow(() -> new ReplacementWordNotFoundException("치환어가 존재하지 않습니다."));

        // 해당 치환어 소유자의 ID와 요청 사용자 ID가 같은지 비교한다.
        if (!replacementWord.getUserId().equals(userId)){
            throw new ReplacementWordNotFoundException("치환어가 존재하지 않습니다.");
        }

        // 사용자가 수정하려는 원본단어가 이미 같은 사용자에게 이미 다른행으로 존재하는지 확인한다.
        // 등록하려는 새 원본단어가 이미 있는지 확인한다.
        userReplacementWordRepository.findByUserIdAndSourceWord(userId, request.getSourceWord())
                // 찾은 인스턴스가 내가 현재 수정 중인 객체인지 확인한다.
                // 찾은 행의 ID가 현재 수정 중인 행의 ID와 다를 때만 남긴다.
                .filter(foundReplacement ->
                        !foundReplacement.getUserReplacementWordId().equals(userReplacementWordId))
                // 현재 수정중인 행과 다른 행이 존재하면 예외를 던진다.
                .ifPresent(foundReplacementWord -> {
                    throw new ReplacementWordAlreadyExistsException("이미 등록된 치환어입니다.");
                });

        // 해당 치환어를 수정한다.
        replacementWord.update(request.getSourceWord(), request.getReplacementWord());

        // 치환어 수정 결과를 반환한다.
        return new ReplacementWordResponse(
                replacementWord.getUserReplacementWordId(),
                replacementWord.getUserId(),
                replacementWord.getSourceWord(),
                replacementWord.getReplacementWord()
        );
    }

}

